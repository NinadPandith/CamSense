package com.camsense.data

import com.camsense.data.db.BleDeviceEntity
import com.camsense.data.db.NetworkDeviceEntity

/**
 * "Since your last scan here": devices that weren't there last time. Wi-Fi devices
 * are matched by name or service (addresses change), camera hotspots by BSSID, and
 * Bluetooth only by name (most Bluetooth addresses are random and change).
 * Pure Kotlin for unit tests.
 */
object ScanDiff {

    data class Result(val newWifi: Int, val newHotspots: Int, val newBluetooth: Int) {
        val total: Int get() = newWifi + newHotspots + newBluetooth
    }

    fun diff(
        previousNet: List<NetworkDeviceEntity>, previousBle: List<BleDeviceEntity>,
        currentNet: List<NetworkDeviceEntity>, currentBle: List<BleDeviceEntity>,
    ): Result {
        val before = keys(previousNet, previousBle)
        val now = keys(currentNet, currentBle)
        val added = now - before
        return Result(
            newWifi = added.count { it.startsWith("lan:") },
            newHotspots = added.count { it.startsWith("ap:") },
            newBluetooth = added.count { it.startsWith("ble-name:") },
        )
    }

    fun keys(net: List<NetworkDeviceEntity>, ble: List<BleDeviceEntity>): Set<String> =
        net.filter { !it.isSelf && !it.isGateway }.map {
            if (SafeDevices.isHotspotAddress(it.ip)) SafeDevices.hotspot(it.ip)
            else SafeDevices.lan(it.ip, it.mdnsName, it.ssdpServer, it.banner)
        }.toSet() + ble.mapNotNull { b -> b.name?.trim()?.takeIf { it.isNotEmpty() }?.let { "ble-name:${it.lowercase()}" } }
}
