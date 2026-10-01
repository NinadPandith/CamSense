package com.camsense.modules.network

import android.util.Log
import com.camsense.scoring.ScanConfig
import java.net.DatagramPacket
import java.net.DatagramSocket
import java.net.InetAddress
import java.net.SocketTimeoutException
import java.util.UUID

/**
 * ONVIF WS-Discovery: one multicast probe asking "any video transmitters here?".
 * Most IP cameras answer, whatever port their stream is on. Read-only.
 */
class OnvifDiscovery {

    /** Blocking; call on Dispatchers.IO with a MulticastLock held. Returns summaries keyed by IPv4. */
    fun discover(): Map<String, String> {
        val result = mutableMapOf<String, String>()
        try {
            DatagramSocket().use { socket ->
                socket.soTimeout = 500
                val bytes = OnvifParser.probe(UUID.randomUUID().toString()).toByteArray()
                socket.send(DatagramPacket(bytes, bytes.size, InetAddress.getByName("239.255.255.250"), 3702))
                val end = System.currentTimeMillis() + ScanConfig.ONVIF_LISTEN_MS
                val buf = ByteArray(8192)
                while (System.currentTimeMillis() < end) {
                    val packet = DatagramPacket(buf, buf.size)
                    try { socket.receive(packet) } catch (_: SocketTimeoutException) { continue }
                    val ip = packet.address.hostAddress ?: continue
                    OnvifParser.summary(String(packet.data, 0, packet.length))?.let { result[ip] = it }
                }
            }
        } catch (e: Exception) {
            Log.w("CamSense/Network", "ONVIF discovery failed", e)
        }
        return result
    }
}
