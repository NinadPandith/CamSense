package com.camsense.core.permissions

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.os.Build
import androidx.core.content.ContextCompat

/** Which runtime permissions each check needs on this Android version (Tech Stack doc). */
object Permissions {

    /** Android 17 (API 37) runtime permission for LAN sockets, in the Nearby devices group. */
    const val ACCESS_LOCAL_NETWORK = "android.permission.ACCESS_LOCAL_NETWORK"
    private const val API_LOCAL_NETWORK = 37

    val camera: List<String> = listOf(Manifest.permission.CAMERA)

    /** Bluetooth scan: BLUETOOTH_SCAN from Android 12, fine location on Android 10–11. */
    val bluetooth: List<String>
        get() = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) listOf(Manifest.permission.BLUETOOTH_SCAN)
        else listOf(Manifest.permission.ACCESS_FINE_LOCATION)

    /** Wi-Fi scan: only needs a runtime permission from Android 17. */
    val localNetwork: List<String>
        get() = if (Build.VERSION.SDK_INT >= API_LOCAL_NETWORK) listOf(ACCESS_LOCAL_NETWORK) else emptyList()

    /** Bluetooth and local network share the Nearby devices prompt, so ask together. */
    val nearbyDevices: List<String> get() = bluetooth + localNetwork

    /** On Android 10–11 the Bluetooth scan needs Location, so the row says so. */
    val nearbyUsesLocation: Boolean get() = Build.VERSION.SDK_INT < Build.VERSION_CODES.S

    fun granted(context: Context, permissions: List<String>): Boolean =
        permissions.all { ContextCompat.checkSelfPermission(context, it) == PackageManager.PERMISSION_GRANTED }

    fun hasCamera(context: Context) = granted(context, camera)
    fun hasBluetooth(context: Context) = granted(context, bluetooth)
    fun hasLocalNetwork(context: Context) = granted(context, localNetwork)
    fun hasNearby(context: Context) = granted(context, nearbyDevices)
}
