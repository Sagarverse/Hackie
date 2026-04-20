package com.example.rabit.data.network

import android.content.Context
import android.net.wifi.WifiManager
import java.net.Inet4Address
import java.net.NetworkInterface
import java.util.Locale

/**
 * Picks the best non-loopback IPv4 for LAN / mDNS (prefers typical private ranges).
 */
object LanIpResolver {

    fun preferredLanIpv4String(context: Context? = null): String? =
        preferredLanIpv4(context)?.hostAddress

    fun preferredLanIpv4(context: Context? = null): Inet4Address? {
        try {
            val candidates = mutableListOf<String>()
            val en = NetworkInterface.getNetworkInterfaces()
            while (en.hasMoreElements()) {
                val intf = en.nextElement()
                if (!intf.isUp || intf.isLoopback) continue
                val addrs = intf.inetAddresses
                while (addrs.hasMoreElements()) {
                    val addr = addrs.nextElement()
                    if (addr is Inet4Address && !addr.isLoopbackAddress) {
                        addr.hostAddress?.takeIf { it.isNotBlank() }?.let { candidates.add(it) }
                    }
                }
            }
            fun score(ip: String): Int = when {
                ip.startsWith("192.168.") -> 300
                ip.startsWith("10.") -> 250
                ip.startsWith("172.") -> {
                    val sec = ip.split('.').getOrNull(1)?.toIntOrNull() ?: -1
                    if (sec in 16..31) 260 else 50
                }
                else -> 100
            }
            val best = candidates.maxByOrNull { score(it) } ?: return null
            return Inet4Address.getByName(best) as Inet4Address
        } catch (_: Exception) { }

        if (context != null) {
            val wm = context.getSystemService(Context.WIFI_SERVICE) as? WifiManager ?: return null
            try {
                @Suppress("DEPRECATION")
                val ipAddress = wm.connectionInfo.ipAddress
                if (ipAddress != 0) {
                    val ip = String.format(
                        Locale.US, "%d.%d.%d.%d",
                        ipAddress and 0xff,
                        ipAddress shr 8 and 0xff,
                        ipAddress shr 16 and 0xff,
                        ipAddress shr 24 and 0xff
                    )
                    return Inet4Address.getByName(ip) as Inet4Address
                }
            } catch (_: Exception) { }
        }
        return null
    }
}
