package com.camsense.modules.network

/** IPv4 helpers for the /24 sweep. Pure Kotlin for unit tests. */
object SubnetMath {

    fun parseIpv4(ip: String): Int? {
        val parts = ip.trim().split('.')
        if (parts.size != 4) return null
        var v = 0
        for (p in parts) {
            val n = p.toIntOrNull() ?: return null
            if (n !in 0..255) return null
            v = (v shl 8) or n
        }
        return v
    }

    fun formatIpv4(v: Int): String =
        "${(v ushr 24) and 0xFF}.${(v ushr 16) and 0xFF}.${(v ushr 8) and 0xFF}.${v and 0xFF}"

    /**
     * Hosts to sweep: the /24 that contains [ownIp]. Wider networks are limited to
     * that /24 to keep the scan near 30 s (PRD: scan only the network you're on).
     * Excludes the network and broadcast addresses and the phone itself.
     */
    fun hostsToScan(ownIp: String, prefixLength: Int): List<String> {
        val ip = parseIpv4(ownIp) ?: return emptyList()
        val prefix = prefixLength.coerceIn(24, 30)
        val mask = if (prefix == 0) 0 else (-1 shl (32 - prefix))
        val network = ip and mask
        val size = 1 shl (32 - prefix)
        return (1 until size - 1)
            .map { network + it }
            .filter { it != ip }
            .map(::formatIpv4)
    }
}
