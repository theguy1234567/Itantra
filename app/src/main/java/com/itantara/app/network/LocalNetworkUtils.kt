package com.itantara.app.network

import java.net.Inet4Address
import java.net.NetworkInterface
import java.util.Collections

object LocalNetworkUtils {
    fun getLocalIpAddress(): String? {
        val wifiCandidates = mutableListOf<String>()
        val fallbackCandidates = mutableListOf<String>()

        try {
            val interfaces = Collections.list(NetworkInterface.getNetworkInterfaces())
            for (iface in interfaces) {
                if (!iface.isUp || iface.isLoopback) continue

                val ifaceName = iface.name.lowercase()
                val isPreferredInterface = ifaceName.contains("wlan") ||
                        ifaceName.contains("ap") ||
                        ifaceName.contains("softap") ||
                        ifaceName.contains("p2p") ||
                        ifaceName.contains("eth")

                val addresses = Collections.list(iface.inetAddresses)
                for (addr in addresses) {
                    if (!addr.isLoopbackAddress && addr is Inet4Address) {
                        val ip = addr.hostAddress
                        if (!ip.isNullOrBlank() && ip != "127.0.0.1") {
                            if (isPreferredInterface) {
                                wifiCandidates.add(ip)
                            } else {
                                fallbackCandidates.add(ip)
                            }
                        }
                    }
                }
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }

        return wifiCandidates.firstOrNull() ?: fallbackCandidates.firstOrNull()
    }
}
