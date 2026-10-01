package com.camsense.modules.network

import com.camsense.core.Reasons
import com.camsense.scoring.ScanConfig

/**
 * Turns what the port and service checks learned about a device into a suspicion
 * score and a plain reason (PRD FR-3.3/3.4). Pure Kotlin for unit tests.
 */
object DeviceClassifier {

    data class Verdict(val score: Float, val reason: String?)

    fun classify(device: NetworkDevice): Verdict {
        if (device.isSelf || device.isGateway) return Verdict(0f, null)

        val banner = device.banner?.lowercase().orEmpty()
        val names = listOfNotNull(device.hostname, device.mdnsName, device.ssdpServer)
            .joinToString(" ").lowercase()
        val ports = device.openPorts.toSet()

        val rtspReply = ScanConfig.RTSP_PORT in ports && banner.startsWith("rtsp/1.0")
        val streamBanner = banner.isNotEmpty() && ScanConfig.STREAM_KEYWORDS.any { it in banner }
        if (rtspReply || streamBanner) return Verdict(ScanConfig.NET_SCORE_STREAM, Reasons.NET_STREAM)
        if (device.onvif != null) return Verdict(ScanConfig.NET_SCORE_ONVIF, Reasons.NET_ONVIF)

        if (ScanConfig.RTSP_PORT in ports || ScanConfig.RTMP_PORT in ports) {
            return Verdict(ScanConfig.NET_SCORE_VIDEO_PORT, Reasons.NET_VIDEO_PORT)
        }
        if (names.isNotBlank() && ScanConfig.STREAM_KEYWORDS.any { it in names }) {
            return Verdict(ScanConfig.NET_SCORE_CAMERA_NAME, Reasons.NET_CAMERA_NAME)
        }
        val altHttp = ports.any { it in ScanConfig.HTTP_PORTS && it != 80 }
        if (altHttp) return Verdict(ScanConfig.NET_SCORE_HTTP_ALT, Reasons.NET_WEB_SERVICE)
        if (80 in ports) return Verdict(ScanConfig.NET_SCORE_HTTP_ONLY, null)
        return Verdict(0f, null)
    }

    /** Request sent to an open port to get a banner, or null to just read. */
    fun probeFor(port: Int, ip: String): String? = when (port) {
        ScanConfig.RTSP_PORT -> "OPTIONS rtsp://$ip:$port/ RTSP/1.0\r\nCSeq: 1\r\n\r\n"
        in ScanConfig.HTTP_PORTS -> "GET / HTTP/1.0\r\nHost: $ip\r\nUser-Agent: CamSense\r\n\r\n"
        else -> null
    }

    /** Keeps banners short and printable for storage and display. */
    fun cleanBanner(raw: String): String =
        raw.replace(Regex("[\\r\\n\\t]+"), " ")
            .filter { it.code in 32..126 }
            .trim()
            .take(ScanConfig.BANNER_MAX_CHARS)
}
