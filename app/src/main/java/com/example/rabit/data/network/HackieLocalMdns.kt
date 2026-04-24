package com.example.rabit.data.network

import android.content.Context
import android.net.wifi.WifiManager
import android.util.Log
import java.net.Inet4Address
import java.util.Hashtable
import javax.jmdns.JmDNS
import javax.jmdns.ServiceInfo

/**
 * Advertises an mDNS hostname like [hostLabel].local (e.g. hackie-a3f2.local) pointing at this
 * device on the LAN so other devices can open http://hostLabel.local:port without memorizing IPv4.
 *
 * Browsers still use **http** (not https): trusted HTTPS on private IPs / .local requires custom CA or user prompts.
 */
object HackieLocalMdns {

    private const val TAG = "HackieLocalMdns"

    private var jmdns: JmDNS? = null
    private var multicastLock: WifiManager.MulticastLock? = null

    /**
     * @param hostLabel single DNS label only, e.g. `hackie-a3f2` (no dots). Lowercase a-z, digits, hyphen.
     */
    fun start(context: Context, bindAddress: Inet4Address, servicePort: Int, hostLabel: String): Boolean {
        stop(context.applicationContext)
        return try {
            val app = context.applicationContext
            val wm = app.getSystemService(Context.WIFI_SERVICE) as WifiManager
            multicastLock = wm.createMulticastLock("HackieBridgeMdns").apply {
                setReferenceCounted(false)
                acquire()
            }
            jmdns = JmDNS.create(bindAddress, hostLabel)
            val props = Hashtable<String, String>()
            props["path"] = "/"
            props["app"] = "Hackie"
            val info = ServiceInfo.create(
                "_http._tcp.local.",
                "Hackie Web Bridge",
                servicePort,
                0,
                0,
                props
            )
            jmdns?.registerService(info)
            Log.i(TAG, "mDNS active: http://$hostLabel.local:$servicePort on ${bindAddress.hostAddress}")
            true
        } catch (e: Exception) {
            Log.e(TAG, "mDNS registration failed", e)
            stop(context.applicationContext)
            false
        }
    }

    fun stop(context: Context) {
        try {
            jmdns?.unregisterAllServices()
        } catch (_: Exception) { }
        try {
            jmdns?.close()
        } catch (_: Exception) { }
        jmdns = null
        try {
            multicastLock?.release()
        } catch (_: Exception) { }
        multicastLock = null
    }
}
