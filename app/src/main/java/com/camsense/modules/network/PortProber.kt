package com.camsense.modules.network

import com.camsense.scoring.ScanConfig
import java.io.IOException
import java.net.InetAddress
import java.net.InetSocketAddress
import java.net.Socket

/**
 * Read-only checks against one host: is a port open, and what does the service say?
 * Never logs in or sends anything beyond a plain request for the front page
 * (PRD ethics requirement).
 */
object PortProber {

    fun isOpen(ip: String, port: Int, timeoutMs: Int = ScanConfig.SOCKET_TIMEOUT_MS): Boolean = try {
        Socket().use { it.connect(InetSocketAddress(ip, port), timeoutMs); true }
    } catch (_: IOException) {
        false
    }

    /** Liveness fallback; a refused TCP connection still counts as "host is there". */
    fun isReachable(ip: String, timeoutMs: Int = ScanConfig.SOCKET_TIMEOUT_MS): Boolean = try {
        InetAddress.getByName(ip).isReachable(timeoutMs)
    } catch (_: IOException) {
        false
    }

    /** First [ScanConfig.BANNER_MAX_CHARS] printable characters the service sends back. */
    fun banner(ip: String, port: Int): String? = try {
        Socket().use { s ->
            s.connect(InetSocketAddress(ip, port), ScanConfig.SOCKET_TIMEOUT_MS)
            s.soTimeout = ScanConfig.BANNER_TIMEOUT_MS
            DeviceClassifier.probeFor(port, ip)?.let { req ->
                s.getOutputStream().apply { write(req.toByteArray()); flush() }
            }
            val buf = ByteArray(1024)
            val sb = StringBuilder()
            val input = s.getInputStream()
            while (sb.length < 1024) {
                val n = try { input.read(buf) } catch (_: IOException) { -1 }
                if (n <= 0) break
                sb.append(String(buf, 0, n, Charsets.ISO_8859_1))
            }
            DeviceClassifier.cleanBanner(sb.toString()).ifBlank { null }
        }
    } catch (_: IOException) {
        null
    }
}
