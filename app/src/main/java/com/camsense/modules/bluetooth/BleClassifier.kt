package com.camsense.modules.bluetooth

import com.camsense.core.Reasons
import com.camsense.scoring.ScanConfig

/** PRD FR-4.2: camera-like names or vendors, or unknown devices that are very close. */
object BleClassifier {

    data class Verdict(val score: Float, val reason: String?)

    fun classify(name: String?, rssi: Int, cameraVendor: Boolean): Verdict {
        val n = name?.lowercase()?.trim().orEmpty()
        if (n.isNotEmpty() && ScanConfig.BLE_CAMERA_KEYWORDS.any { it in n }) {
            return Verdict(ScanConfig.BLE_SCORE_CAMERA_NAME, Reasons.BLE_CAMERA_NAME)
        }
        if (cameraVendor) return Verdict(ScanConfig.BLE_SCORE_CAMERA_VENDOR, Reasons.BLE_CAMERA_VENDOR)
        if (n.isEmpty() && rssi > ScanConfig.BLE_STRONG_RSSI) {
            return Verdict(ScanConfig.BLE_SCORE_STRONG_UNKNOWN, Reasons.BLE_STRONG_UNKNOWN)
        }
        return Verdict(0f, null)
    }

    /** OUI prefix ("AA:BB:CC") of a MAC address, upper case. */
    fun ouiOf(address: String): String? {
        val parts = address.split(':', '-')
        if (parts.size < 3) return null
        return parts.take(3).joinToString(":") { it.uppercase().padStart(2, '0') }
    }
}
