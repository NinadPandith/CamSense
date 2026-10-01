package com.camsense.modules.network

import android.content.Context
import android.net.nsd.NsdManager
import android.net.nsd.NsdServiceInfo
import android.util.Log
import com.camsense.scoring.ScanConfig
import kotlinx.coroutines.delay
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.withTimeoutOrNull
import kotlin.coroutines.resume

/** Finds devices that announce themselves with mDNS (Bonjour), e.g. "IPCam-3F2A". */
class MdnsDiscovery(context: Context) {

    private val nsd = context.getSystemService(NsdManager::class.java)
    private val serviceTypes = listOf("_http._tcp.", "_rtsp._tcp.", "_camera._tcp.", "_onvif._tcp.")

    /** Returns service names keyed by IPv4 address. Needs a MulticastLock held by the caller. */
    suspend fun discover(): Map<String, String> {
        if (nsd == null) return emptyMap()
        val found = mutableListOf<NsdServiceInfo>()
        val listeners = serviceTypes.map { type ->
            object : NsdManager.DiscoveryListener {
                override fun onServiceFound(info: NsdServiceInfo) { synchronized(found) { found += info } }
                override fun onServiceLost(info: NsdServiceInfo) {}
                override fun onDiscoveryStarted(type: String) {}
                override fun onDiscoveryStopped(type: String) {}
                override fun onStartDiscoveryFailed(type: String, code: Int) { Log.w(TAG, "mDNS $type failed: $code") }
                override fun onStopDiscoveryFailed(type: String, code: Int) {}
            }.also {
                try { nsd.discoverServices(type, NsdManager.PROTOCOL_DNS_SD, it) } catch (e: Exception) { Log.w(TAG, e) }
            }
        }
        delay(ScanConfig.MDNS_LISTEN_MS)
        listeners.forEach { try { nsd.stopServiceDiscovery(it) } catch (_: Exception) {} }

        val result = mutableMapOf<String, String>()
        val services = synchronized(found) { found.toList() }
        // NsdManager resolves one service at a time.
        for (info in services.distinctBy { it.serviceName + it.serviceType }) {
            val resolved = withTimeoutOrNull(1_500) { resolve(info) } ?: continue
            @Suppress("DEPRECATION")
            val ip = resolved.host?.hostAddress ?: continue
            if (ip.contains(':')) continue
            result[ip] = listOfNotNull(result[ip], resolved.serviceName).distinct().joinToString(", ")
        }
        return result
    }

    @Suppress("DEPRECATION")
    private suspend fun resolve(info: NsdServiceInfo): NsdServiceInfo? = suspendCancellableCoroutine { cont ->
        try {
            nsd!!.resolveService(info, object : NsdManager.ResolveListener {
                override fun onResolveFailed(si: NsdServiceInfo, code: Int) { if (cont.isActive) cont.resume(null) }
                override fun onServiceResolved(si: NsdServiceInfo) { if (cont.isActive) cont.resume(si) }
            })
        } catch (e: Exception) {
            if (cont.isActive) cont.resume(null)
        }
    }

    private companion object { const val TAG = "CamSense/Network" }
}
