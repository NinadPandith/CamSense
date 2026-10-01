package com.camsense.modules.bluetooth

/** One Bluetooth device seen during a scan; mirrors the ble_devices table. */
data class BleDevice(
    val address: String,
    val name: String? = null,
    val rssi: Int,
    val manufacturerId: Int? = null,
    /** Vendor from the bundled OUI list; only for public (non-random) addresses. */
    val vendor: String? = null,
    val suspicionScore: Float = 0f,
    val reason: String? = null,
    val id: String = java.util.UUID.randomUUID().toString(),
)
