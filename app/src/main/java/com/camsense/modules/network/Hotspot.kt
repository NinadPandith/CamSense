package com.camsense.modules.network

/** A Wi-Fi network broadcast nearby (not necessarily the one the phone is on). */
data class Hotspot(
    val ssid: String,
    val bssid: String,
    /** Signal in dBm; higher (closer to 0) is nearer. */
    val level: Int,
    val vendor: String? = null,
    val suspicionScore: Float = 0f,
    val reason: String? = null,
)

/**
 * Camera-like networks are saved in the network_devices table: the BSSID goes in
 * the ip column (it's the address findings point to) and the name in hostname.
 */
fun Hotspot.toNetworkDevice() = NetworkDevice(
    ip = bssid,
    hostname = ssid,
    ssdpServer = vendor,
    banner = "Wi-Fi network, signal $level dBm",
    suspicionScore = suspicionScore,
    reason = reason,
)
