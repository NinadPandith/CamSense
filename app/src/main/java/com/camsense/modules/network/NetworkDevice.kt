package com.camsense.modules.network

/** One device found on the Wi-Fi during a scan; mirrors the network_devices table. */
data class NetworkDevice(
    val ip: String,
    val hostname: String? = null,
    val mdnsName: String? = null,
    val ssdpServer: String? = null,
    val openPorts: List<Int> = emptyList(),
    val banner: String? = null,
    val isSelf: Boolean = false,
    val isGateway: Boolean = false,
    val suspicionScore: Float = 0f,
    val reason: String? = null,
    val id: String = java.util.UUID.randomUUID().toString(),
)
