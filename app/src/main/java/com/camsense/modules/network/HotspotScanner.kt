package com.camsense.modules.network

import android.annotation.SuppressLint
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.net.wifi.WifiManager
import android.util.Log
import androidx.core.content.ContextCompat
import com.camsense.modules.bluetooth.OuiDatabase
import com.camsense.scoring.ScanConfig
import kotlinx.coroutines.withTimeoutOrNull

/**
 * Lists nearby Wi-Fi networks and flags camera-like ones (HotspotClassifier). Unlike
 * the LAN sweep it works without joining any network. Unlike other Wi-Fi devices,
 * access points do expose their MAC (BSSID), so the OUI vendor list applies here.
 */
class HotspotScanner(private val context: Context, private val oui: OuiDatabase) {

    @SuppressLint("MissingPermission") // checked through Permissions.hasWifiScan
    suspend fun scan(): List<Hotspot> {
        val wifi = context.applicationContext.getSystemService(WifiManager::class.java) ?: return emptyList()
        // Ask for fresh results; Android throttles scans, so fall back to the cached ones.
        val done = kotlinx.coroutines.CompletableDeferred<Unit>()
        val receiver = object : BroadcastReceiver() {
            override fun onReceive(c: Context, intent: Intent) { done.complete(Unit) }
        }
        ContextCompat.registerReceiver(context, receiver,
            IntentFilter(WifiManager.SCAN_RESULTS_AVAILABLE_ACTION), ContextCompat.RECEIVER_NOT_EXPORTED)
        try {
            @Suppress("DEPRECATION")
            val started = runCatching { wifi.startScan() }.getOrDefault(false)
            if (started) withTimeoutOrNull(ScanConfig.HOTSPOT_SCAN_MS) { done.await() }
        } finally {
            runCatching { context.unregisterReceiver(receiver) }
        }
        val results = try { wifi.scanResults.orEmpty() } catch (e: SecurityException) {
            Log.w(TAG, "No permission for Wi-Fi scan results", e)
            return emptyList()
        }
        return results.mapNotNull { r ->
            @Suppress("DEPRECATION")
            val ssid = r.SSID?.trim()?.removeSurrounding("\"").orEmpty()
            val bssid = r.BSSID ?: return@mapNotNull null
            if (ssid.isEmpty()) return@mapNotNull null // hidden network
            val vendor = oui.lookup(bssid)
            val v = HotspotClassifier.classify(ssid, vendor?.cameraMaker == true)
            Hotspot(ssid, bssid, r.level, vendor?.name, v.score, v.reason)
        }.distinctBy { it.bssid }.sortedWith(compareByDescending<Hotspot> { it.suspicionScore }.thenByDescending { it.level })
            .also { list -> Log.i(TAG, "Wi-Fi networks nearby: ${list.size}, camera-like: ${list.count { it.suspicionScore > 0 }}") }
    }

    private companion object { const val TAG = "CamSense/Network" }
}
