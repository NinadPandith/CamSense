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

    /**
     * Drops findings for remembered devices and recomputes the Wi-Fi and Bluetooth
     * scores (each is the strongest remaining finding). Returns the results and how
     * many findings were dropped.
     */
    fun filter(
        results: List<ModuleResult>,
        safe: Set<String>,
        signatureOf: (ModuleId, Finding) -> String?,
    ): Pair<List<ModuleResult>, Int> {
        if (safe.isEmpty()) return results to 0
        var dropped = 0
        val out = results.map { r ->
            if (r.moduleId != ModuleId.NETWORK && r.moduleId != ModuleId.BLE) return@map r
            val kept = r.findings.filter { f ->
                val sig = signatureOf(r.moduleId, f)
                (sig == null || sig !in safe).also { if (!it) dropped++ }
            }
            if (kept.size == r.findings.size) r
            else r.copy(findings = kept, score = kept.maxOfOrNull { it.confidence } ?: 0f)
        }
        return out to dropped
    }
}
