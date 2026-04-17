package com.example.rabit.data.adb

import android.hardware.usb.UsbConstants
import android.hardware.usb.UsbDevice
import android.hardware.usb.UsbDeviceConnection
import android.hardware.usb.UsbEndpoint
import android.hardware.usb.UsbInterface
import java.io.InputStream
import java.io.OutputStream
import java.net.InetAddress
import java.net.Socket
import java.net.SocketAddress

/**
 * A Virtual Socket that bridges Android USB Bulk transfers to the Java Socket API.
 * This allows adblib to communicate over USB as if it were a network socket.
 */
class UsbAdbSocket(
    private val connection: UsbDeviceConnection,
    private val usbInterface: UsbInterface,
    private val inEndpoint: UsbEndpoint,
    private val outEndpoint: UsbEndpoint
) : Socket() {

    private val inputStream = UsbInputStream(connection, inEndpoint)
    private val outputStream = UsbOutputStream(connection, outEndpoint)

    override fun getInputStream(): InputStream = inputStream
    override fun getOutputStream(): OutputStream = outputStream

    override fun close() {
        // We don't close the connection here as it might be managed by UsbAdbManager
        // But we release the interface
        connection.releaseInterface(usbInterface)
    }

    override fun isConnected(): Boolean = true
    override fun isClosed(): Boolean = false

    // Stub methods for Socket compatibility
    override fun getLocalAddress(): InetAddress = InetAddress.getLoopbackAddress()
    override fun getLocalPort(): Int = 0
    override fun getInetAddress(): InetAddress = InetAddress.getLoopbackAddress()
    override fun getPort(): Int = 0
    override fun bind(bindpoint: SocketAddress?) {}
    override fun connect(endpoint: SocketAddress?) {}
    override fun connect(endpoint: SocketAddress?, timeout: Int) {}

    private class UsbInputStream(
        private val connection: UsbDeviceConnection,
        private val endpoint: UsbEndpoint
    ) : InputStream() {
        private val timeout = 10000

        override fun read(): Int {
            val buf = ByteArray(1)
            val read = connection.bulkTransfer(endpoint, buf, 1, timeout)
            return if (read > 0) buf[0].toInt() and 0xFF else -1
        }

        override fun read(b: ByteArray, off: Int, len: Int): Int {
            return connection.bulkTransfer(endpoint, b, off, len, timeout)
        }
    }

    private class UsbOutputStream(
        private val connection: UsbDeviceConnection,
        private val endpoint: UsbEndpoint
    ) : OutputStream() {
        private val timeout = 10000

        override fun write(b: Int) {
            val buf = byteArrayOf(b.toByte())
            connection.bulkTransfer(endpoint, buf, 1, timeout)
        }

        override fun write(b: ByteArray, off: Int, len: Int) {
            connection.bulkTransfer(endpoint, b, off, len, timeout)
        }
    }
}
