package com.example.rabit.data.bluetooth

import android.content.Context
import android.util.Log
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import java.io.File
import java.io.FileOutputStream
import java.util.concurrent.TimeUnit

/**
 * UsbHidGadgetManager — Converts phone into USB HID keyboard on rooted Android.
 *
 * Key fixes applied based on CMF Phone 1 (MediaTek mt6878) testing:
 *   - ConfigFS at /config/ (not /sys/kernel/config/)
 *   - SELinux must be permissive for ConfigFS operations
 *   - sys.usb.configfs must be 0 to stop init from fighting us
 *   - UDC unbind kills ADB (expected)
 *   - All operations serialized to prevent race conditions
 */
class UsbHidGadgetManager private constructor(private val context: Context) {

    companion object {
        private const val TAG = "UsbHidGadget"
        private const val CMD_TIMEOUT = 10L
        private const val GADGET = "/config/usb_gadget/g1"
        private const val HID_FUNC = "$GADGET/functions/hid.gs0"
        private const val CONFIG = "$GADGET/configs/b.1"

        @Volatile
        private var INSTANCE: UsbHidGadgetManager? = null

        fun getInstance(context: Context): UsbHidGadgetManager {
            return INSTANCE ?: synchronized(this) {
                INSTANCE ?: UsbHidGadgetManager(context.applicationContext).also { INSTANCE = it }
            }
        }

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
    private var keyboardOutputStream: FileOutputStream? = null
    private val reportLock = Any()
    private var hidDevPath = ""

    // Mutex to prevent concurrent connect/disconnect
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
        } catch (e: Exception) { false }
    }

    /** Batch multiple commands in one su shell — much faster than spawning N processes */
    private fun suBatch(vararg cmds: String): Boolean {
        val script = cmds.joinToString(" ; ")
        return su(script)
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

    // ═══ Connect ═══

    fun connect() {
        // Cancel any pending operation
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
                    synchronized(operationLock) {
                        setupGadget()
                    }
                }
            }

            when {
                result == true -> {
                    _state.value = UsbGadgetState.Connected
                    Log.d(TAG, "USB HID ACTIVE!")
                }
                result == null -> {
                    _state.value = UsbGadgetState.Error("Setup timed out")
                    su("setenforce 1")
                }
                else -> _state.value = UsbGadgetState.Error("Setup failed")
            }
        }
    }

    private fun setupGadget(): Boolean {
        val udcName = suOut("ls /sys/class/udc/ | head -1")
        if (udcName.isBlank()) { Log.e(TAG, "No UDC!"); return false }

        // STEP 0: SELinux permissive + disable init (batched = 1 process)
        Log.d(TAG, "Preparing USB subsystem...")
        suBatch("setenforce 0", "setprop sys.usb.configfs 0")
        Thread.sleep(300)

        // STEP 1: Unbind UDC
        su("echo > $GADGET/UDC")
        Thread.sleep(500)

        // STEP 2: Remove function links (batched = 1 process)
        suBatch("rm $CONFIG/f1 2>/dev/null", "rm $CONFIG/f2 2>/dev/null",
                "rm $CONFIG/f3 2>/dev/null", "rm $CONFIG/f4 2>/dev/null",
                "rm $CONFIG/f5 2>/dev/null")
        Thread.sleep(200)

        // STEP 3: Configure HID (batched = 1 process)
        Log.d(TAG, "Configuring HID keyboard...")
        suBatch("echo 1 > $HID_FUNC/protocol",
                "echo 1 > $HID_FUNC/subclass",
                "echo 8 > $HID_FUNC/report_length")

        val tmp = File(context.cacheDir, "hid_desc")
        tmp.writeBytes(KEYBOARD_REPORT_DESC)
        su("cp ${tmp.absolutePath} $HID_FUNC/report_desc")
        tmp.delete()

        // STEP 4: Link HID function
        Log.d(TAG, "Linking HID...")
        su("ln -s $HID_FUNC $CONFIG/f1")
        val linkTarget = suOut("readlink $CONFIG/f1")
        if (!linkTarget.contains("hid")) {
            Log.e(TAG, "HID link failed! f1 -> $linkTarget")
            suBatch("setenforce 1", "setprop sys.usb.configfs 1")
            return false
        }

        // STEP 5: Bind to UDC
        Log.d(TAG, "Binding to UDC...")
        su("echo '$udcName' > $GADGET/UDC")
        Thread.sleep(1000)

        // STEP 6: Open /dev/hidgX
        hidDevPath = suOut("ls /dev/hidg* 2>/dev/null | head -1")
        if (hidDevPath.isBlank()) {
            Log.e(TAG, "No /dev/hidg*!")
            suBatch("setenforce 1", "setprop sys.usb.configfs 1")
            return false
        }

        su("chmod 666 $hidDevPath")
        Thread.sleep(100)

        // Open the HID device. Try direct first, fall back to root pipe.
        try {
            val devFile = File(hidDevPath)
            keyboardOutputStream = FileOutputStream(devFile)
            // Test write — send empty report
            keyboardOutputStream!!.write(ByteArray(8))
            keyboardOutputStream!!.flush()
            Log.d(TAG, "Direct write OK: $hidDevPath")
        } catch (e: Exception) {
            Log.w(TAG, "Direct open failed (${e.message}), using su dd fallback")
            keyboardOutputStream = null
        }

        return true
    }

    // ═══ Disconnect ═══

    fun disconnect() {
        currentJob?.cancel()
        currentJob = scope.launch(Dispatchers.IO) {
            synchronized(operationLock) {
                try { keyboardOutputStream?.close() } catch (_: Exception) {}
                keyboardOutputStream = null
                hidDevPath = ""

                suBatch("setenforce 0",
                        "echo > $GADGET/UDC 2>/dev/null",
                        "rm $CONFIG/f1 2>/dev/null")
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

    // ═══ HID Report Sending ═══

    private fun sendKeyboardReport(report: ByteArray) {
        if (_state.value != UsbGadgetState.Connected) return
        synchronized(reportLock) {
            try {
                val stream = keyboardOutputStream
                if (stream != null) {
                    stream.write(report)
                    stream.flush()
                } else if (hidDevPath.isNotBlank()) {
                    // Fallback: write binary via su dd (much more reliable than echo -ne)
                    val tmp = File(context.cacheDir, "hid_report")
                    tmp.writeBytes(report)
                    su("dd if=${tmp.absolutePath} of=$hidDevPath bs=8 count=1 2>/dev/null")
                    tmp.delete()
                }
            } catch (e: Exception) {
                Log.e(TAG, "Report failed: ${e.message}")
                // Try to reopen
                try {
                    keyboardOutputStream?.close()
                    keyboardOutputStream = null
                    if (hidDevPath.isNotBlank()) {
                        keyboardOutputStream = FileOutputStream(File(hidDevPath))
                    }
                } catch (_: Exception) { keyboardOutputStream = null }
            }
        }
    }

    /**
     * Send a key press + release. The release is sent synchronously
     * so callers (like CodeTyper) don't need to manage timing.
     */
    fun sendKeyPress(keyCode: Byte, modifier: Byte = 0, useSticky: Boolean = true) {
        if (keyCode == 0.toByte() && modifier == 0.toByte()) {
            // This IS a release — just send the empty report
            sendKeyboardReport(ByteArray(8))
            return
        }
        val press = ByteArray(8).apply { this[0] = modifier; this[2] = keyCode }
        sendKeyboardReport(press)
        // Don't auto-release here — let the caller handle timing.
        // CodeTyper sends its own release after a hold delay.
    }

    fun sendMouseMove(dx: Int, dy: Int, buttons: Int = 0, wheel: Int = 0) {}

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
        try { keyboardOutputStream?.close() } catch (_: Exception) {}
        keyboardOutputStream = null
        scope.cancel()
    }
}
