package com.example.rabit.data.bluetooth

import android.content.Context
import android.util.Log
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import java.io.BufferedReader
import java.io.DataInputStream
import java.io.InputStreamReader
import java.nio.ByteBuffer
import java.nio.ByteOrder

/**
 * UsbMouseBridgeManager — Bridges a USB OTG wired mouse to Bluetooth HID.
 *
 * Flow: Wired Mouse → USB OTG → Phone reads /dev/input/eventX → Phone sends
 * Bluetooth HID mouse reports → PC receives wireless mouse input.
 *
 * Requires root access to read /dev/input/ event devices (same constraint as
 * UsbHidGadgetManager for writing to /dev/hidgX).
 *
 * Linux input_event struct (arm64): 24 bytes
 *   - struct timeval tv (16 bytes: tv_sec u64 + tv_usec u64)
 *   - __u16 type
 *   - __u16 code
 *   - __s32 value
 *
 * Linux input_event struct (arm32): 16 bytes
 *   - struct timeval tv (8 bytes: tv_sec u32 + tv_usec u32)
 *   - __u16 type
 *   - __u16 code
 *   - __s32 value
 */
class UsbMouseBridgeManager(private val context: Context) {

    companion object {
        private const val TAG = "UsbMouseBridge"

        // Linux input event types
        private const val EV_SYN: Short = 0x00
        private const val EV_KEY: Short = 0x01
        private const val EV_REL: Short = 0x02

        // Relative axis codes
        private const val REL_X: Short = 0x00
        private const val REL_Y: Short = 0x01
        private const val REL_WHEEL: Short = 0x08
        private const val REL_HWHEEL: Short = 0x06

        // Button codes
        private const val BTN_LEFT: Short = 0x110
        private const val BTN_RIGHT: Short = 0x111
        private const val BTN_MIDDLE: Short = 0x112

        // Try 64-bit first (most modern Android phones are arm64)
        private const val INPUT_EVENT_SIZE_64 = 24
        private const val INPUT_EVENT_SIZE_32 = 16

        @Volatile
        private var INSTANCE: UsbMouseBridgeManager? = null

        fun getInstance(context: Context): UsbMouseBridgeManager {
            return INSTANCE ?: synchronized(this) {
                INSTANCE ?: UsbMouseBridgeManager(context.applicationContext).also { INSTANCE = it }
            }
        }
    }

    enum class BridgeState {
        IDLE,
        SCANNING_DEVICES,
        ACTIVE,
        NO_ROOT,
        NO_MOUSE_FOUND,
        ERROR
    }

    private val _bridgeState = MutableStateFlow(BridgeState.IDLE)
    val bridgeState: StateFlow<BridgeState> = _bridgeState.asStateFlow()

    private val _detectedMouseName = MutableStateFlow("")
    val detectedMouseName: StateFlow<String> = _detectedMouseName.asStateFlow()

    private val _detectedEventPath = MutableStateFlow("")
    val detectedEventPath: StateFlow<String> = _detectedEventPath.asStateFlow()

    private val _eventsForwarded = MutableStateFlow(0L)
    val eventsForwarded: StateFlow<Long> = _eventsForwarded.asStateFlow()

    private val _errorMessage = MutableStateFlow("")
    val errorMessage: StateFlow<String> = _errorMessage.asStateFlow()

    private val _isRootAvailable = MutableStateFlow(false)
    val isRootAvailable: StateFlow<Boolean> = _isRootAvailable.asStateFlow()

    var sensitivity: Float = 1.0f

    private val scope = CoroutineScope(Dispatchers.IO + SupervisorJob())
    private var bridgeJob: Job? = null
    private var rootProcess: Process? = null

    // Accumulated mouse state for the current SYN frame
    private var accumDx = 0
    private var accumDy = 0
    private var accumWheel = 0
    private var buttonState = 0 // bitmask: bit0=left, bit1=right, bit2=middle

    init {
        scope.launch { checkRootAvailability() }
    }

    private suspend fun checkRootAvailability() {
        withContext(Dispatchers.IO) {
            try {
                val process = Runtime.getRuntime().exec(arrayOf("su", "-c", "echo root_ok"))
                val reader = BufferedReader(InputStreamReader(process.inputStream))
                val output = reader.readLine()
                val ok = output?.trim() == "root_ok"
                _isRootAvailable.value = ok
                process.destroy()
            } catch (e: Exception) {
                _isRootAvailable.value = false
                Log.w(TAG, "Root not available: ${e.message}")
            }
        }
    }

    /**
     * Scans /proc/bus/input/devices for USB mouse event devices.
     * Returns the /dev/input/eventX path and device name if found.
     */
    private suspend fun findUsbMouse(): Pair<String, String>? {
        return withContext(Dispatchers.IO) {
            try {
                val process = Runtime.getRuntime().exec(arrayOf("su", "-c", "cat /proc/bus/input/devices"))
                val reader = BufferedReader(InputStreamReader(process.inputStream))
                val lines = reader.readLines()
                process.destroy()

                var currentName = ""
                var currentHandlers = ""
                var isUsb = false
                var isMouse = false

                for (line in lines) {
                    when {
                        line.startsWith("N: Name=") -> {
                            currentName = line.substringAfter("N: Name=").trim('"')
                        }
                        line.startsWith("S: Sysfs=") -> {
                            isUsb = line.contains("/usb") || line.contains("USB")
                        }
                        line.startsWith("H: Handlers=") -> {
                            currentHandlers = line.substringAfter("H: Handlers=")
                            isMouse = currentHandlers.contains("mouse")
                        }
                        line.isBlank() -> {
                            // End of device block
                            if (isUsb && isMouse) {
                                val eventMatch = Regex("event(\\d+)").find(currentHandlers)
                                if (eventMatch != null) {
                                    val eventPath = "/dev/input/event${eventMatch.groupValues[1]}"
                                    return@withContext Pair(eventPath, currentName)
                                }
                            }
                            currentName = ""
                            currentHandlers = ""
                            isUsb = false
                            isMouse = false
                        }
                    }
                }
                null
            } catch (e: Exception) {
                Log.e(TAG, "Failed to scan input devices", e)
                null
            }
        }
    }

    /**
     * Start the mouse bridge: detect USB mouse and begin forwarding events.
     */
    fun start(hidManager: HidDeviceManager) {
        if (bridgeJob?.isActive == true) return

        bridgeJob = scope.launch {
            try {
                if (!_isRootAvailable.value) {
                    checkRootAvailability()
                    if (!_isRootAvailable.value) {
                        _bridgeState.value = BridgeState.NO_ROOT
                        _errorMessage.value = "Root access required to read USB mouse events"
                        return@launch
                    }
                }

                _bridgeState.value = BridgeState.SCANNING_DEVICES
                _errorMessage.value = ""

                val mouseInfo = findUsbMouse()
                if (mouseInfo == null) {
                    _bridgeState.value = BridgeState.NO_MOUSE_FOUND
                    _errorMessage.value = "No USB mouse detected. Connect a mouse via OTG cable."
                    return@launch
                }

                val (eventPath, mouseName) = mouseInfo
                _detectedMouseName.value = mouseName
                _detectedEventPath.value = eventPath
                _eventsForwarded.value = 0
                Log.i(TAG, "Found USB mouse: $mouseName at $eventPath")

                _bridgeState.value = BridgeState.ACTIVE
                readAndForwardEvents(eventPath, hidManager)

            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                Log.e(TAG, "Bridge error", e)
                _bridgeState.value = BridgeState.ERROR
                _errorMessage.value = e.message ?: "Unknown error"
            }
        }
    }

    /**
     * Stop the mouse bridge.
     */
    fun stop() {
        bridgeJob?.cancel()
        bridgeJob = null
        rootProcess?.destroy()
        rootProcess = null
        resetAccumulator()
        _bridgeState.value = BridgeState.IDLE
        _detectedMouseName.value = ""
        _detectedEventPath.value = ""
    }

    private fun resetAccumulator() {
        accumDx = 0
        accumDy = 0
        accumWheel = 0
        // Don't reset button state — buttons persist
    }

    /**
     * Opens a root shell, reads raw input_event structs from the event device,
     * parses them, and forwards mouse movements/clicks to the Bluetooth HID manager.
     */
    private suspend fun readAndForwardEvents(eventPath: String, hidManager: HidDeviceManager) {
        withContext(Dispatchers.IO) {
            // Use 'cat' via root to stream raw bytes from the event device.
            // We can't open /dev/input/eventX directly from Java without root.
            val process = Runtime.getRuntime().exec(arrayOf("su", "-c", "cat $eventPath"))
            rootProcess = process

            val inputStream = DataInputStream(process.inputStream)
            val eventSize = detectEventSize()
            val buffer = ByteArray(eventSize)

            Log.i(TAG, "Reading events from $eventPath (event_size=$eventSize)")

            try {
                while (isActive) {
                    // Read one complete input_event struct
                    inputStream.readFully(buffer)

                    val bb = ByteBuffer.wrap(buffer).order(ByteOrder.LITTLE_ENDIAN)

                    // Skip the timeval field
                    val timevalSize = if (eventSize == INPUT_EVENT_SIZE_64) 16 else 8
                    bb.position(timevalSize)

                    val type = bb.short
                    val code = bb.short
                    val value = bb.int

                    processInputEvent(type, code, value, hidManager)
                }
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                if (isActive) {
                    Log.e(TAG, "Error reading events", e)
                    _bridgeState.value = BridgeState.ERROR
                    _errorMessage.value = "Lost connection to USB mouse: ${e.message}"
                }
            } finally {
                process.destroy()
                rootProcess = null
            }
        }
    }

    /**
     * Detect whether the kernel uses 32-bit or 64-bit input_event structs.
     * Most modern Android devices are arm64, so we default to 64-bit.
     */
    private fun detectEventSize(): Int {
        return try {
            val process = Runtime.getRuntime().exec(arrayOf("su", "-c", "getprop ro.product.cpu.abi"))
            val reader = BufferedReader(InputStreamReader(process.inputStream))
            val abi = reader.readLine()?.trim() ?: ""
            process.destroy()
            if (abi.contains("64")) INPUT_EVENT_SIZE_64 else INPUT_EVENT_SIZE_32
        } catch (e: Exception) {
            INPUT_EVENT_SIZE_64 // Default to 64-bit
        }
    }

    /**
     * Process a single Linux input event and accumulate state.
     * On EV_SYN, flush accumulated state to the Bluetooth HID channel.
     */
    private fun processInputEvent(type: Short, code: Short, value: Int, hidManager: HidDeviceManager) {
        when (type) {
            EV_REL -> {
                when (code) {
                    REL_X -> accumDx += (value * sensitivity).toInt()
                    REL_Y -> accumDy += (value * sensitivity).toInt()
                    REL_WHEEL -> accumWheel += value
                }
            }
            EV_KEY -> {
                val pressed = value != 0 // 1 = pressed, 0 = released
                when (code) {
                    BTN_LEFT -> {
                        buttonState = if (pressed) buttonState or 0x01 else buttonState and 0x01.inv()
                    }
                    BTN_RIGHT -> {
                        buttonState = if (pressed) buttonState or 0x02 else buttonState and 0x02.inv()
                    }
                    BTN_MIDDLE -> {
                        buttonState = if (pressed) buttonState or 0x04 else buttonState and 0x04.inv()
                    }
                }
            }
            EV_SYN -> {
                // Flush: send accumulated deltas + current button state
                if (accumDx != 0 || accumDy != 0 || accumWheel != 0 || true) {
                    hidManager.sendMouseMove(
                        dx = accumDx.toFloat(),
                        dy = accumDy.toFloat(),
                        buttons = buttonState,
                        wheel = accumWheel
                    )
                    _eventsForwarded.value++
                }
                accumDx = 0
                accumDy = 0
                accumWheel = 0
            }
        }
    }

    fun destroy() {
        stop()
        scope.cancel()
    }
}
