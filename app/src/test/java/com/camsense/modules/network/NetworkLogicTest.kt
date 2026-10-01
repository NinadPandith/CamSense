package com.camsense.modules.network

import com.camsense.core.Reasons
import com.camsense.scoring.ScanConfig
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class NetworkLogicTest {

    @Test
    fun parsesAndFormatsIpv4() {
        val v = SubnetMath.parseIpv4("192.168.43.14")!!
        assertEquals("192.168.43.14", SubnetMath.formatIpv4(v))
        assertNull(SubnetMath.parseIpv4("192.168.1"))
        assertNull(SubnetMath.parseIpv4("192.168.1.300"))
    }

    @Test
    fun sweepsOwnSlash24WithoutSelf() {
        val hosts = SubnetMath.hostsToScan("192.168.43.22", 24)
        assertEquals(253, hosts.size)
        assertEquals("192.168.43.1", hosts.first())
        assertEquals("192.168.43.254", hosts.last())
        assertFalse("192.168.43.22" in hosts)
    }

    @Test
    fun widerNetworksAreLimitedToSlash24() {
        assertEquals(253, SubnetMath.hostsToScan("10.0.5.9", 16).size)
        assertTrue(SubnetMath.hostsToScan("10.0.5.9", 16).all { it.startsWith("10.0.5.") })
    }

    @Test
    fun ipWebcamBannerIsAStream() {
        val v = DeviceClassifier.classify(
            NetworkDevice("192.168.43.14", openPorts = listOf(8080), banner = "HTTP/1.1 200 OK ... IP Webcam")
        )
        assertEquals(ScanConfig.NET_SCORE_STREAM, v.score)
        assertEquals(Reasons.NET_STREAM, v.reason)
        assertTrue(v.score >= ScanConfig.NETWORK_STREAM_SCORE)
    }

    @Test
    fun rtspReplyIsAStream() {
        val v = DeviceClassifier.classify(
            NetworkDevice("10.0.0.5", openPorts = listOf(554), banner = "RTSP/1.0 200 OK CSeq: 1")
        )
        assertEquals(ScanConfig.NET_SCORE_STREAM, v.score)
    }

    @Test
    fun openRtspPortWithoutBannerIsVideoPort() {
        val v = DeviceClassifier.classify(NetworkDevice("10.0.0.5", openPorts = listOf(554)))
        assertEquals(ScanConfig.NET_SCORE_VIDEO_PORT, v.score)
    }

    @Test
    fun routerAndSelfAreIgnored() {
        val ports = listOf(80, 554)
        assertEquals(0f, DeviceClassifier.classify(NetworkDevice("a", openPorts = ports, isGateway = true)).score)
        assertEquals(0f, DeviceClassifier.classify(NetworkDevice("a", openPorts = ports, isSelf = true)).score)
    }

    @Test
    fun cameraLikeMdnsName() {
        val v = DeviceClassifier.classify(NetworkDevice("a", mdnsName = "IPCam-3F2A.local"))
        assertEquals(ScanConfig.NET_SCORE_CAMERA_NAME, v.score)
    }

    @Test
    fun plainWebServerIsLow() {
        assertEquals(ScanConfig.NET_SCORE_HTTP_ONLY,
            DeviceClassifier.classify(NetworkDevice("a", openPorts = listOf(80), banner = "HTTP/1.1 200 OK nginx")).score)
        assertEquals(ScanConfig.NET_SCORE_HTTP_ALT,
            DeviceClassifier.classify(NetworkDevice("a", openPorts = listOf(8000))).score)
    }

    @Test
    fun bannersAreCleanedAndTrimmed() {
        val raw = "HTTP/1.1 200 OK\r\nServer: x\r\n" + "a".repeat(500)
        val b = DeviceClassifier.cleanBanner(raw)
        assertEquals(ScanConfig.BANNER_MAX_CHARS, b.length)
        assertFalse(b.contains('\n'))
    }
}
