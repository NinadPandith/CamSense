package com.camsense.core

/**
 * Plain-English reasons attached to findings. They are saved with each finding
 * (findings.reason) and shown on Results, so they follow the Content Guidelines:
 * calm, everyday words, no jargon.
 */
object Reasons {
    const val GLINT = "Tiny bright reflection, like a camera lens"
    fun glintOn(objectName: String) = "Tiny bright reflection on the ${objectName.lowercase()}, like a camera lens"
    const val IR = "Small glowing dot that only shows in the dark, like a night-vision light"
    const val NET_STREAM = "A device on this Wi-Fi is streaming video"
    const val NET_VIDEO_PORT = "A device on this Wi-Fi is set up to send video"
    const val NET_CAMERA_NAME = "A device on this Wi-Fi calls itself a camera"
    const val NET_WEB_SERVICE = "An unknown device on this Wi-Fi runs a small web server"
    const val BLE_CAMERA_NAME = "A nearby Bluetooth device has a camera-like name"
    const val BLE_CAMERA_VENDOR = "A nearby Bluetooth device is made by a camera chip maker"
    const val BLE_STRONG_UNKNOWN = "An unknown Bluetooth device is very close"
    fun objectHidingSpot(objectName: String) = "$objectName: a common place to hide a camera"
    const val MAGNET = "Strong electronics signal near this spot"
}
