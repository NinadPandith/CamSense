package com.camsense.modules.network

import android.util.Log
import com.camsense.scoring.ScanConfig
import java.net.DatagramPacket
import java.net.DatagramSocket
import java.net.HttpURLConnection
import java.net.InetAddress
import java.net.SocketTimeoutException
import java.net.URI

/**
 * UPnP/SSDP discovery: one multicast M-SEARCH, then read replies for a few seconds.
 * For each reply we keep the SERVER header and the friendly/model name from the
 * device's description file, which often names IP cameras outright.
 */
class SsdpDiscovery {

    /** Blocking; call on Dispatchers.IO with a MulticastLock held. Returns text keyed by IPv4. */
    fun discover(): Map<String, String> {
        val result = mutableMapOf<String, String>()
        val locations = mutableMapOf<String, String>()
        try {
            DatagramSocket().use { socket ->
                socket.soTimeout = 500
                val msg = "M-SEARCH * HTTP/1.1\r\nHOST: 239.255.255.250:1900\r\n" +
                    "MAN: \"ssdp:discover\"\r\nMX: 2\r\nST: ssdp:all\r\n\r\n"
                val bytes = msg.toByteArray()
                socket.send(DatagramPacket(bytes, bytes.size, InetAddress.getByName("239.255.255.250"), 1900))
                val end = System.currentTimeMillis() + ScanConfig.SSDP_LISTEN_MS
                val buf = ByteArray(2048)
                while (System.currentTimeMillis() < end) {
                    val packet = DatagramPacket(buf, buf.size)
                    try { socket.receive(packet) } catch (_: SocketTimeoutException) { continue }
                    val ip = packet.address.hostAddress ?: continue
                    val text = String(packet.data, 0, packet.length)
                    header(text, "SERVER")?.let { server ->
                        result[ip] = listOfNotNull(result[ip], server).distinct().joinToString(" | ")
                    }
                    header(text, "LOCATION")?.let { locations.putIfAbsent(ip, it) }
                }
            }
        } catch (e: Exception) {
            Log.w(TAG, "SSDP failed", e)
        }
        for ((ip, location) in locations) {
            val names = describe(location) ?: continue
            result[ip] = listOfNotNull(result[ip], names).distinct().joinToString(" | ")
        }
        return result.mapValues { it.value.take(ScanConfig.BANNER_MAX_CHARS) }
    }

    private fun header(text: String, name: String): String? =
        text.lineSequence().firstOrNull { it.startsWith("$name:", ignoreCase = true) }
            ?.substringAfter(':')?.trim()?.ifBlank { null }

    /** friendlyName and modelName from the UPnP description XML (local network only). */
    private fun describe(location: String): String? = try {
        val url = URI(location).toURL()
        if (!InetAddress.getByName(url.host).isSiteLocalAddress) null
        else (url.openConnection() as HttpURLConnection).run {
            connectTimeout = ScanConfig.BANNER_TIMEOUT_MS
            readTimeout = ScanConfig.BANNER_TIMEOUT_MS
            try {
                val xml = inputStream.bufferedReader().use { it.readText().take(8_000) }
                listOf("friendlyName", "manufacturer", "modelName")
                    .mapNotNull { tag -> Regex("<$tag>([^<]*)</$tag>").find(xml)?.groupValues?.get(1)?.trim() }
                    .filter { it.isNotEmpty() }
                    .joinToString(" ")
                    .ifBlank { null }
            } finally {
                disconnect()
            }
        }
    } catch (_: Exception) {
        null
    }

    private companion object { const val TAG = "CamSense/Network" }
}
