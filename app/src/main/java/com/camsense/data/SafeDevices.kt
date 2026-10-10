package com.camsense.data

import com.camsense.core.Finding
import com.camsense.core.ModuleId
import com.camsense.core.ModuleResult

/**
 * Devices the user marked "Checked, it's fine" are remembered (on the phone, in
 * settings) and not flagged again: Wi-Fi devices, camera-like hotspots and Bluetooth
 * devices. Light spots (glints, glows) can't be recognised again reliably, so they
 * aren't remembered. Pure Kotlin for unit tests.
 */
object SafeDevices {

    /** Wi-Fi device: its announced name or service; the IP only as a last resort (it changes). */
    fun lan(ip: String, mdnsName: String?, ssdpServer: String?, banner: String?): String =
        "lan:" + (mdnsName ?: ssdpServer ?: banner?.take(40) ?: ip).trim().lowercase()

    fun hotspot(bssid: String): String = "ap:" + bssid.trim().lowercase()

    fun ble(address: String): String = "ble:" + address.trim().uppercase()

    /** Hotspots are saved with their BSSID (a MAC address) in the ip column. */
    fun isHotspotAddress(address: String): Boolean = address.count { it == ':' } == 5

    /** A grouped finding ("3 unknown Bluetooth devices") keeps every address, comma-separated. */
    fun joinAddresses(addresses: List<String>): String = addresses.joinToString(",")

    fun addresses(deviceAddress: String?): List<String> =
        deviceAddress?.split(',')?.map { it.trim() }?.filter { it.isNotEmpty() } ?: emptyList()

    /**
     * Drops findings for remembered devices and recomputes the Wi-Fi and Bluetooth
     * scores (each is the strongest remaining finding). In a grouped finding only the
     * remembered devices are removed; [regroup] rewrites it for the ones left. Returns
     * the results and how many findings were dropped.
     */
    fun filter(
        results: List<ModuleResult>,
        safe: Set<String>,
        regroup: (Finding, List<String>) -> Finding = { f, left -> f.copy(deviceAddress = joinAddresses(left)) },
        signatureOf: (ModuleId, String) -> String?,
    ): Pair<List<ModuleResult>, Int> {
        if (safe.isEmpty()) return results to 0
        var dropped = 0
        val out = results.map { r ->
            if (r.moduleId != ModuleId.NETWORK && r.moduleId != ModuleId.BLE) return@map r
            val kept = r.findings.mapNotNull { f ->
                val all = addresses(f.deviceAddress)
                val left = all.filter { a -> signatureOf(r.moduleId, a)?.let { it !in safe } ?: true }
                when {
                    left.size == all.size -> f
                    left.isEmpty() -> { dropped++; null }
                    else -> regroup(f, left)
                }
            }
            if (kept == r.findings) r
            else r.copy(findings = kept, score = kept.maxOfOrNull { it.confidence } ?: 0f)
        }
        return out to dropped
    }
}
