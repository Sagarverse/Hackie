package com.example.rabit.data.bluetooth

import android.content.Context
import android.util.Log
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import java.io.File
import java.io.FileOutputStream
import java.io.OutputStream
import java.util.concurrent.TimeUnit
import kotlin.math.roundToInt

/**
 * UsbHidGadgetManager — Full USB HID gadget: keyboard + mouse.
 *
 * Sets up TWO HID functions on the USB gadget:
 *   - hid.gs0 = Keyboard (8-byte reports via /dev/hidg0)
 *   - hid.gs1 = Mouse    (4-byte reports via /dev/hidg1)
 *
 * Uses a persistent root shell pipe when direct file access fails (SELinux).
 * This ensures CodeTyper works at full speed even without direct /dev access.
 */
class UsbHidGadgetManager private constructor(private val context: Context) {

    companion object {
        private const val TAG = "UsbHidGadget"
        private const val CMD_TIMEOUT = 10L
        private const val GADGET = "/config/usb_gadget/g1"
        private const val KB_FUNC = "$GADGET/functions/hid.gs0"
        private const val MOUSE_FUNC = "$GADGET/functions/hid.gs1"
        private const val CONFIG = "$GADGET/configs/b.1"

        @Volatile
        private var INSTANCE: UsbHidGadgetManager? = null

        fun getInstance(context: Context): UsbHidGadgetManager {
            return INSTANCE ?: synchronized(this) {
                INSTANCE ?: UsbHidGadgetManager(context.applicationContext).also { INSTANCE = it }
            }
        }

        // Standard keyboard HID report descriptor
        private val KEYBOARD_REPORT_DESC = byteArrayOf(
            0x05, 0x01, 0x09, 0x06, 0xA1.toByte(), 0x01,
            0x05, 0x07, 0x19, 0xE0.toByte(), 0x29, 0xE7.toByte(),
            0x15, 0x00, 0x25, 0x01, 0x75, 0x01, 0x95.toByte(), 0x08,
            0x81.toByte(), 0x02,
            0x95.toByte(), 0x01, 0x75, 0x08, 0x81.toByte(), 0x03,
            0x95.toByte(), 0x06, 0x75, 0x08, 0x15, 0x00, 0x25, 0x65,
            0x05, 0x07, 0x19, 0x00, 0x29, 0x65,
            0x81.toByte(), 0x00, 0xC0.toByte()
        )

        // Mouse HID report descriptor: 3 buttons + X + Y + wheel (relative)
        private val MOUSE_REPORT_DESC = byteArrayOf(
            0x05, 0x01,                         // Usage Page (Generic Desktop)
            0x09, 0x02,                         // Usage (Mouse)
            0xA1.toByte(), 0x01,                // Collection (Application)
            0x09, 0x01,                         //   Usage (Pointer)
            0xA1.toByte(), 0x00,                //   Collection (Physical)
            0x05, 0x09,                         //     Usage Page (Buttons)
            0x19, 0x01,                         //     Usage Min (Button 1)
            0x29, 0x03,                         //     Usage Max (Button 3)
            0x15, 0x00,                         //     Logical Min (0)
            0x25, 0x01,                         //     Logical Max (1)
            0x95.toByte(), 0x03,                //     Report Count (3)
            0x75, 0x01,                         //     Report Size (1)
            0x81.toByte(), 0x02,                //     Input (Data, Var, Abs)
            0x95.toByte(), 0x01,                //     Report Count (1)
            0x75, 0x05,                         //     Report Size (5) padding
            0x81.toByte(), 0x03,                //     Input (Const)
            0x05, 0x01,                         //     Usage Page (Generic Desktop)
            0x09, 0x30,                         //     Usage (X)
            0x09, 0x31,                         //     Usage (Y)
            0x09, 0x38,                         //     Usage (Wheel)
            0x15, 0x81.toByte(),                //     Logical Min (-127)
            0x25, 0x7F,                         //     Logical Max (127)
            0x75, 0x08,                         //     Report Size (8)
            0x95.toByte(), 0x03,                //     Report Count (3)
            0x81.toByte(), 0x06,                //     Input (Data, Var, Rel)
            0xC0.toByte(),                      //   End Collection
            0xC0.toByte()                       // End Collection
        )
    }

    sealed class UsbGadgetState {
        object NotAvailable : UsbGadgetState()
        object Disconnected : UsbGadgetState()
        object Configuring : UsbGadgetState()
        object Connected : UsbGadgetState()
        data class Error(val message: String) : UsbGadgetState()
    }

    private val _state = MutableStateFlow<UsbGadgetState>(UsbGadgetState.Disconnected)
    val state: StateFlow<UsbGadgetState> = _state.asStateFlow()

    private val _isRootAvailable = MutableStateFlow(false)
    val isRootAvailable: StateFlow<Boolean> = _isRootAvailable.asStateFlow()

    private val scope = CoroutineScope(Dispatchers.IO + SupervisorJob())

    // Direct file streams (used when SELinux allows direct access)
    private var kbOutputStream: FileOutputStream? = null
    private var mouseOutputStream: FileOutputStream? = null

    // Persistent root shell pipes (fallback when direct access is blocked)
    private var kbPipeProcess: Process? = null
    private var kbPipeStream: OutputStream? = null
    private var mousePipeProcess: Process? = null
    private var mousePipeStream: OutputStream? = null

    private val kbLock = Any()
    private val mouseLock = Any()
    private var kbDevPath = ""
    private var mouseDevPath = ""

    // Mouse fractional accumulator for smooth movement
    private var mouseAccumX = 0f
    private var mouseAccumY = 0f

    private val operationLock = Any()
    private var currentJob: Job? = null

    init {
        scope.launch { _isRootAvailable.value = checkRoot() }
    }

    // ═══ Shell helpers ═══

    private fun su(cmd: String): Boolean {
        return try {
            val p = Runtime.getRuntime().exec(arrayOf("su", "-c", cmd))
            if (!p.waitFor(CMD_TIMEOUT, TimeUnit.SECONDS)) {
                p.destroyForcibly(); false
            } else p.exitValue() == 0
        } catch (_: Exception) { false }
    }

    private fun suBatch(vararg cmds: String): Boolean {
        return su(cmds.joinToString(" ; "))
    }

    private fun suOut(cmd: String): String {
        return try {
            val p = Runtime.getRuntime().exec(arrayOf("su", "-c", cmd))
            val out = p.inputStream.bufferedReader().readText().trim()
            if (!p.waitFor(CMD_TIMEOUT, TimeUnit.SECONDS)) { p.destroyForcibly(); "" } else out
        } catch (_: Exception) { "" }
    }

    private suspend fun checkRoot(): Boolean = withContext(Dispatchers.IO) {
        try {
            val p = Runtime.getRuntime().exec(arrayOf("su", "-c", "id"))
            val out = p.inputStream.bufferedReader().readText()
            if (!p.waitFor(3, TimeUnit.SECONDS)) { p.destroyForcibly(); false }
            else out.contains("uid=0")
        } catch (_: Exception) { false }
    }

    // ═══ Persistent Root Pipe ═══
    // Opens a long-lived `su -c 'cat > /dev/hidgX'` process.
    // Writing to its stdin goes directly to the HID device at native speed.

    private fun openRootPipe(devPath: String): Pair<Process, OutputStream>? {
        return try {
            val p = Runtime.getRuntime().exec(arrayOf("su", "-c", "cat > $devPath"))
            val stream = p.outputStream
            // Test: write an empty report to verify the pipe works
            val testReport = if (devPath.contains("hidg0")) ByteArray(8) else ByteArray(4)
            stream.write(testReport)
            stream.flush()
            Log.d(TAG, "Root pipe OK: $devPath")
            Pair(p, stream)
        } catch (e: Exception) {
            Log.e(TAG, "Root pipe failed for $devPath: ${e.message}")
            null
        }
    }

    private fun closeRootPipe(process: Process?, stream: OutputStream?) {
        try { stream?.close() } catch (_: Exception) {}
        try { process?.destroyForcibly() } catch (_: Exception) {}
    }

    // ═══ Connect ═══

    fun connect() {
        currentJob?.cancel()
        currentJob = scope.launch {
            _state.value = UsbGadgetState.Configuring

            if (!_isRootAvailable.value) {
                _isRootAvailable.value = checkRoot()
                if (!_isRootAvailable.value) {
                    _state.value = UsbGadgetState.Error("Root access required")
                    return@launch
                }
            }

            val result = withTimeoutOrNull(25_000L) {
                withContext(Dispatchers.IO) {
                    synchronized(operationLock) { setupGadget() }
                }
            }

            when {
                result == true -> {
                    _state.value = UsbGadgetState.Connected
                    Log.d(TAG, "USB HID ACTIVE (keyboard + mouse)!")
                }
                result == null -> {
                    _state.value = UsbGadgetState.Error("Setup timed out")
                    su("setenforce 1")
                }
                else -> _state.value = UsbGadgetState.Error("Setup failed")
            }
        }
    }

    private fun writeDescFile(path: String, data: ByteArray) {
        val tmp = File(context.cacheDir, "hid_desc_${System.nanoTime()}")
        tmp.writeBytes(data)
        su("cp ${tmp.absolutePath} $path")
        tmp.delete()
    }

    private fun setupGadget(): Boolean {
        val udcName = suOut("ls /sys/class/udc/ | head -1")
        if (udcName.isBlank()) { Log.e(TAG, "No UDC!"); return false }

        // STEP 0: SELinux + disable init
        Log.d(TAG, "Preparing...")
        suBatch("setenforce 0", "setprop sys.usb.configfs 0")
        Thread.sleep(300)

        // STEP 1: Unbind UDC
        su("echo > $GADGET/UDC")
        Thread.sleep(500)

        // STEP 2: Remove all function links
        suBatch("rm $CONFIG/f1 2>/dev/null", "rm $CONFIG/f2 2>/dev/null",
                "rm $CONFIG/f3 2>/dev/null", "rm $CONFIG/f4 2>/dev/null",
                "rm $CONFIG/f5 2>/dev/null")
        Thread.sleep(200)

        // STEP 3: Create mouse function if it doesn't exist
        su("mkdir $MOUSE_FUNC 2>/dev/null")
        Thread.sleep(100)

        // STEP 4: Configure keyboard (hid.gs0)
        Log.d(TAG, "Configuring keyboard...")
        suBatch("echo 1 > $KB_FUNC/protocol",
                "echo 1 > $KB_FUNC/subclass",
                "echo 8 > $KB_FUNC/report_length")
        writeDescFile("$KB_FUNC/report_desc", KEYBOARD_REPORT_DESC)

        // STEP 5: Configure mouse (hid.gs1)
        Log.d(TAG, "Configuring mouse...")
        suBatch("echo 2 > $MOUSE_FUNC/protocol",
                "echo 1 > $MOUSE_FUNC/subclass",
                "echo 4 > $MOUSE_FUNC/report_length")
        writeDescFile("$MOUSE_FUNC/report_desc", MOUSE_REPORT_DESC)

        // STEP 6: Link both into config
        Log.d(TAG, "Linking functions...")
        su("ln -s $KB_FUNC $CONFIG/f1")
        su("ln -s $MOUSE_FUNC $CONFIG/f2")

        val link1 = suOut("readlink $CONFIG/f1")
        val link2 = suOut("readlink $CONFIG/f2")
        Log.d(TAG, "f1 -> $link1, f2 -> $link2")

        if (!link1.contains("hid")) {
            Log.e(TAG, "Keyboard link failed!")
            suBatch("setenforce 1", "setprop sys.usb.configfs 1")
            return false
        }

        // STEP 7: Bind to UDC
        Log.d(TAG, "Binding to UDC...")
        su("echo '$udcName' > $GADGET/UDC")
        Thread.sleep(1000)

        // STEP 8: Open /dev/hidg0 (keyboard) and /dev/hidg1 (mouse)
        kbDevPath = suOut("ls /dev/hidg0 2>/dev/null")
        mouseDevPath = suOut("ls /dev/hidg1 2>/dev/null")
        Log.d(TAG, "Keyboard: $kbDevPath, Mouse: $mouseDevPath")

        if (kbDevPath.isBlank()) {
            Log.e(TAG, "No /dev/hidg0!")
            suBatch("setenforce 1", "setprop sys.usb.configfs 1")
            return false
        }

        suBatch("chmod 666 /dev/hidg0 2>/dev/null", "chmod 666 /dev/hidg1 2>/dev/null")
        Thread.sleep(100)

        // ── Open Keyboard Device ──
        // Try 1: Direct FileOutputStream (fastest, but may fail due to SELinux)
        try {
            kbOutputStream = FileOutputStream(File(kbDevPath))
            kbOutputStream!!.write(ByteArray(8))
            kbOutputStream!!.flush()
            Log.d(TAG, "Keyboard: direct write OK")
        } catch (e: Exception) {
            Log.w(TAG, "Keyboard direct failed: ${e.message}")
            kbOutputStream = null
            // Try 2: Persistent root pipe (fast fallback)
            val pipe = openRootPipe(kbDevPath)
            if (pipe != null) {
                kbPipeProcess = pipe.first
                kbPipeStream = pipe.second
            }
        }

        // ── Open Mouse Device ──
        if (mouseDevPath.isNotBlank()) {
            try {
                mouseOutputStream = FileOutputStream(File(mouseDevPath))
                mouseOutputStream!!.write(ByteArray(4))
                mouseOutputStream!!.flush()
                Log.d(TAG, "Mouse: direct write OK")
            } catch (e: Exception) {
                Log.w(TAG, "Mouse direct failed: ${e.message}")
                mouseOutputStream = null
                val pipe = openRootPipe(mouseDevPath)
                if (pipe != null) {
                    mousePipeProcess = pipe.first
                    mousePipeStream = pipe.second
                }
            }
        }

        // Reset mouse accumulator
        mouseAccumX = 0f
        mouseAccumY = 0f

        return true
    }

    // ═══ Disconnect ═══

    fun disconnect() {
        currentJob?.cancel()
        currentJob = scope.launch(Dispatchers.IO) {
            synchronized(operationLock) {
                // Close all streams and pipes
                try { kbOutputStream?.close() } catch (_: Exception) {}
                try { mouseOutputStream?.close() } catch (_: Exception) {}
                closeRootPipe(kbPipeProcess, kbPipeStream)
                closeRootPipe(mousePipeProcess, mousePipeStream)
                kbOutputStream = null
                mouseOutputStream = null
                kbPipeProcess = null
                kbPipeStream = null
                mousePipeProcess = null
                mousePipeStream = null
                kbDevPath = ""
                mouseDevPath = ""

                suBatch("setenforce 0",
                        "echo > $GADGET/UDC 2>/dev/null",
                        "rm $CONFIG/f1 2>/dev/null",
                        "rm $CONFIG/f2 2>/dev/null")
                Thread.sleep(300)
                suBatch("setprop sys.usb.configfs 1",
                        "setprop sys.usb.ffs.ready 1",
                        "setprop sys.usb.config mtp,adb",
                        "setenforce 1")

                _state.value = UsbGadgetState.Disconnected
                Log.d(TAG, "USB restored")
            }
        }
    }

    // ═══ Keyboard Reports ═══

    private fun sendKeyboardReport(report: ByteArray) {
        if (_state.value != UsbGadgetState.Connected) return
        synchronized(kbLock) {
            try {
                // Priority 1: Direct FileOutputStream (fastest)
                val direct = kbOutputStream
                if (direct != null) {
                    direct.write(report)
                    direct.flush()
                    return
                }
                // Priority 2: Persistent root pipe (fast)
                val pipe = kbPipeStream
                if (pipe != null) {
                    pipe.write(report)
                    pipe.flush()
                    return
                }
                // Priority 3: One-shot su (slow, last resort)
                if (kbDevPath.isNotBlank()) {
                    val tmp = File(context.cacheDir, "kb_rpt")
                    tmp.writeBytes(report)
                    su("dd if=${tmp.absolutePath} of=$kbDevPath bs=8 count=1 2>/dev/null")
                    tmp.delete()
                }
            } catch (e: Exception) {
                Log.e(TAG, "KB report failed: ${e.message}")
                // Try to recover: reopen direct stream
                try {
                    kbOutputStream?.close(); kbOutputStream = null
                    closeRootPipe(kbPipeProcess, kbPipeStream)
                    kbPipeProcess = null; kbPipeStream = null
                    if (kbDevPath.isNotBlank()) {
                        try {
                            kbOutputStream = FileOutputStream(File(kbDevPath))
                        } catch (_: Exception) {
                            val pipe = openRootPipe(kbDevPath)
                            if (pipe != null) {
                                kbPipeProcess = pipe.first
                                kbPipeStream = pipe.second
                            }
                        }
                    }
                } catch (_: Exception) {}
            }
        }
    }

    fun sendKeyPress(keyCode: Byte, modifier: Byte = 0, useSticky: Boolean = true) {
        if (keyCode == 0.toByte() && modifier == 0.toByte()) {
            sendKeyboardReport(ByteArray(8))
            return
        }
        val press = ByteArray(8).apply { this[0] = modifier; this[2] = keyCode }
        sendKeyboardReport(press)
    }

    // ═══ Mouse Reports ═══

    private fun sendMouseReport(report: ByteArray) {
        if (_state.value != UsbGadgetState.Connected) return
        synchronized(mouseLock) {
            try {
                val direct = mouseOutputStream
                if (direct != null) {
                    direct.write(report)
                    direct.flush()
                    return
                }
                val pipe = mousePipeStream
                if (pipe != null) {
                    pipe.write(report)
                    pipe.flush()
                    return
                }
                if (mouseDevPath.isNotBlank()) {
                    val tmp = File(context.cacheDir, "mouse_rpt")
                    tmp.writeBytes(report)
                    su("dd if=${tmp.absolutePath} of=$mouseDevPath bs=4 count=1 2>/dev/null")
                    tmp.delete()
                }
            } catch (e: Exception) {
                Log.e(TAG, "Mouse report failed: ${e.message}")
                try {
                    mouseOutputStream?.close(); mouseOutputStream = null
                    closeRootPipe(mousePipeProcess, mousePipeStream)
                    mousePipeProcess = null; mousePipeStream = null
                    if (mouseDevPath.isNotBlank()) {
                        try {
                            mouseOutputStream = FileOutputStream(File(mouseDevPath))
                        } catch (_: Exception) {
                            val pipe = openRootPipe(mouseDevPath)
                            if (pipe != null) {
                                mousePipeProcess = pipe.first
                                mousePipeStream = pipe.second
                            }
                        }
                    }
                } catch (_: Exception) {}
            }
        }
    }

    /**
     * Send mouse movement with fractional accumulation.
     * Small deltas (0.3, 0.7, etc.) are accumulated and sent when they
     * reach at least 1 pixel, ensuring smooth cursor movement.
     */
    fun sendMouseMove(dx: Float, dy: Float, buttons: Int = 0, wheel: Int = 0) {
        mouseAccumX += dx
        mouseAccumY += dy

        // roundToInt() preserves sub-pixel residuals symmetrically; see
        // HidDeviceManager.sendMouseMove for the full explanation.
        val sendX = mouseAccumX.roundToInt()
        val sendY = mouseAccumY.roundToInt()

        // Only send if there's at least 1 pixel of movement or button/wheel activity
        if (sendX == 0 && sendY == 0 && buttons == 0 && wheel == 0) return

        mouseAccumX -= sendX
        mouseAccumY -= sendY

        val clampedDx = sendX.coerceIn(-127, 127).toByte()
        val clampedDy = sendY.coerceIn(-127, 127).toByte()
        val clampedWheel = wheel.coerceIn(-127, 127).toByte()
        val report = byteArrayOf(buttons.toByte(), clampedDx, clampedDy, clampedWheel)
        sendMouseReport(report)
    }

    // Overload for Int callers
    fun sendMouseMove(dx: Int, dy: Int, buttons: Int = 0, wheel: Int = 0) {
        sendMouseMove(dx.toFloat(), dy.toFloat(), buttons, wheel)
    }

    // ═══ Text Sending ═══

    fun sendText(text: String): Job {
        return scope.launch {
            text.forEach { char ->
                val model = com.example.rabit.domain.model.HidKeyCodes.getHidCode(char)
                if (model.keyCode != 0.toByte() || model.modifier != 0.toByte()) {
                    val press = ByteArray(8).apply { this[0] = model.modifier; this[2] = model.keyCode }
                    sendKeyboardReport(press)
                    delay(50)
                    sendKeyboardReport(ByteArray(8)) // release
                    delay(80)
                }
            }
        }
    }

    fun cleanup() {
        try { kbOutputStream?.close() } catch (_: Exception) {}
        try { mouseOutputStream?.close() } catch (_: Exception) {}
        closeRootPipe(kbPipeProcess, kbPipeStream)
        closeRootPipe(mousePipeProcess, mousePipeStream)
        kbOutputStream = null
        mouseOutputStream = null
        kbPipeProcess = null
        kbPipeStream = null
        mousePipeProcess = null
        mousePipeStream = null
        scope.cancel()
    }
}
