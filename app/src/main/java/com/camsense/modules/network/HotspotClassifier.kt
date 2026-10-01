package com.camsense.modules.network

import com.camsense.core.Reasons
import com.camsense.scoring.ScanConfig

/**
 * Cheap Wi-Fi spy cameras that aren't joined to the room's Wi-Fi broadcast their own
 * network for setup, with names like "IPC-3F2A", "V380-xxxx" or "HDWiFiCam". This
 * flags such names, and networks whose maker (from the BSSID) is a camera maker.
 * Pure Kotlin for unit tests.
 */
object HotspotClassifier {

    data class Verdict(val score: Float, val reason: String?)

    /** Whole-word camera terms, so "Campus" or "Camden" don't match "cam". */
    private val CAMERA_WORD = Regex(
        "(?<![a-z])(" + ScanConfig.HOTSPOT_CAMERA_WORDS.joinToString("|") { Regex.escape(it) } + ")(?![a-z])"
    )
    /** Setup networks of common spy-camera apps: "MV-1A2B3C", "A9_12345", "HD-xxxx". */
    private val CAMERA_PREFIX = Regex("^(mv|a9|hd|bk|wd)[-_][0-9a-z]{4,}$")

    fun isCameraLikeName(ssid: String): Boolean {
        val s = ssid.lowercase().trim()
        if (s.isEmpty()) return false
        return CAMERA_WORD.containsMatchIn(s) || CAMERA_PREFIX.matches(s)
    }

    fun classify(ssid: String, cameraVendor: Boolean): Verdict = when {
        isCameraLikeName(ssid) -> Verdict(ScanConfig.HOTSPOT_SCORE_CAMERA_NAME, Reasons.HOTSPOT_CAMERA_NAME)
        cameraVendor -> Verdict(ScanConfig.HOTSPOT_SCORE_CAMERA_VENDOR, Reasons.HOTSPOT_CAMERA_VENDOR)
        else -> Verdict(0f, null)
    }
}
