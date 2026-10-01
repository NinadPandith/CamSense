package com.camsense.modules.bluetooth

import android.content.Context
import android.util.Log

/**
 * Bundled vendor list (assets/oui/iot_vendors.csv), trimmed to camera and IoT chip
 * makers. Since Android 10 apps can't see other Wi-Fi devices' MAC addresses, so
 * this is used only for Bluetooth devices that expose a public address.
 */
class OuiDatabase(private val context: Context) {

    data class Vendor(val name: String, val cameraMaker: Boolean)

    private val table: Map<String, Vendor> by lazy { load() }

    fun lookup(address: String): Vendor? {
        if (!looksPublic(address)) return null
        return BleClassifier.ouiOf(address)?.let(table::get)
    }

    /** Random BLE addresses set the "locally administered" bit or the two top bits. */
    private fun looksPublic(address: String): Boolean {
        val first = address.take(2).toIntOrNull(16) ?: return false
        return first and 0x02 == 0 && first and 0xC0 != 0xC0
    }

    private fun load(): Map<String, Vendor> = try {
        context.assets.open("oui/iot_vendors.csv").bufferedReader().useLines { lines ->
            lines.filter { it.isNotBlank() && !it.startsWith("#") && !it.startsWith("prefix") }
                .mapNotNull { line ->
                    val parts = line.split(',')
                    if (parts.size < 3) null
                    else parts[0].trim().uppercase() to Vendor(parts[1].trim(), parts[2].trim() == "1")
                }.toMap()
        }
    } catch (e: Exception) {
        Log.w("CamSense/Ble", "OUI list missing", e)
        emptyMap()
    }
}
