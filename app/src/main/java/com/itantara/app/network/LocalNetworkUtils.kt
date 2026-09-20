package com.itantara.app.network

import android.util.Log
import java.net.Inet4Address
import java.net.NetworkInterface
import java.util.Collections

object LocalNetworkUtils {
    private fun wifiOrdinal(interfaceName: String): Int {
        val match = Regex("^wlan(\\d+)$").find(interfaceName.lowercase())
        return match?.groupValues?.get(1)?.toIntOrNull() ?: Int.MAX_VALUE
    }

    fun getLocalIpAddress(): String? {
        data class InterfaceCandidate(
            val ip: String,
            val score: Int,
            val interfaceName: String,
            val isUp: Boolean,
            val isVirtual: Boolean
        )

        val candidates = mutableListOf<InterfaceCandidate>()

        try {
            val interfaces = Collections.list(NetworkInterface.getNetworkInterfaces())
            for (iface in interfaces) {
                val ifaceName = iface.name ?: "unknown"
                val nameLower = ifaceName.lowercase()
                val isUp = iface.isUp
                val isLoopback = iface.isLoopback
                val isVirtual = iface.isVirtual

                if (!isUp || isLoopback) {
                    Log.d("ITANTRA_NET", "interface=${ifaceName} ip=n/a up=${isUp} virtual=${isVirtual} skipped=loopback_or_down")
                    continue
                }

                val addresses = Collections.list(iface.inetAddresses)
                for (addr in addresses) {
                    if (addr !is Inet4Address) continue

                    val ip = addr.hostAddress ?: continue
                    if (ip == "0.0.0.0" || ip == "127.0.0.1" || addr.isLoopbackAddress) {
                        Log.d("ITANTRA_NET", "interface=${ifaceName} ip=${ip} up=${isUp} virtual=${isVirtual} skipped=loopback_or_invalid")
                        continue
                    }

                    var score = 0
                    when {
                        nameLower.contains("wlan") || nameLower.contains("softap") || nameLower.contains("ap") || nameLower.contains("p2p") -> score += 100
                        nameLower.contains("eth") -> score += 80
                        nameLower.contains("rmnet") || nameLower.contains("ccmni") || nameLower.contains("tun") || nameLower.contains("tap") || nameLower.contains("vpn") || nameLower.contains("ppp") -> score -= 120
                        else -> score += 20
                    }

                    if (!isVirtual) score += 20
                    if (isUp) score += 10

                    Log.d("ITANTRA_NET", "interface=${ifaceName} ip=${ip} up=${isUp} virtual=${isVirtual} score=${score}")
                    candidates.add(InterfaceCandidate(ip, score, ifaceName, isUp, isVirtual))
                }
            }
        } catch (e: Exception) {
            Log.e("ITANTRA_NET", "Local IPv4 detection failed", e)
        }

        val selected = candidates
            .sortedWith(
                compareByDescending<InterfaceCandidate> { it.score }
                    .thenBy { if (it.interfaceName.lowercase().startsWith("wlan")) wifiOrdinal(it.interfaceName) else Int.MAX_VALUE }
                    .thenBy { it.interfaceName.lowercase() }
                    .thenBy { it.ip }
            )
            .firstOrNull()

        Log.d("ITANTRA_NET", "SELECTED_HOST_IP=${selected?.ip ?: "none"}")
        return selected?.ip
    }
}
