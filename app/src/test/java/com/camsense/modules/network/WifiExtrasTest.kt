package com.camsense.modules.network

import com.camsense.core.Reasons
import com.camsense.scoring.ScanConfig
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class WifiExtrasTest {

    @Test
    fun cameraHotspotNames() {
        listOf("IPC-3F2A9B", "V380-123456", "HDWiFiCam_01", "Cam_88AB", "MV-1A2B3C", "A9_8812", "yoosee-xx", "My Spy Cam")
            .forEach { assertTrue(it, HotspotClassifier.isCameraLikeName(it)) }
    }

    @Test
    fun ordinaryNetworksAreNotFlagged() {
        listOf("Campus WiFi", "Hotel Sunrise Guest", "JioFiber-5G", "AndroidAP_1234", "Camden Lodge", "TP-Link_8812", "")
            .forEach { assertFalse(it, HotspotClassifier.isCameraLikeName(it)) }
    }

    @Test
    fun hotspotScores() {
        assertEquals(ScanConfig.HOTSPOT_SCORE_CAMERA_NAME, HotspotClassifier.classify("IPC-3F2A", false).score)
        assertEquals(Reasons.HOTSPOT_CAMERA_VENDOR, HotspotClassifier.classify("Home", cameraVendor = true).reason)
        assertNull(HotspotClassifier.classify("Home", false).reason)
        // A hotspot alone stays below the "confirmed stream" High floor.
        assertTrue(ScanConfig.HOTSPOT_SCORE_CAMERA_NAME < ScanConfig.NETWORK_STREAM_SCORE)
    }

    @Test
    fun onvifProbeMatchIsReadAsCamera() {
        val reply = """<SOAP-ENV:Envelope><SOAP-ENV:Body><d:ProbeMatches><d:ProbeMatch>
            <d:Scopes>onvif://www.onvif.org/type/video_encoder onvif://www.onvif.org/name/IPCAM onvif://www.onvif.org/hardware/C6F0SgZ3N0P6L0 onvif://www.onvif.org/location/country/china</d:Scopes>
            <d:XAddrs>http://192.168.1.20:8899/onvif/device_service</d:XAddrs></d:ProbeMatch></d:ProbeMatches></SOAP-ENV:Body></SOAP-ENV:Envelope>"""
        assertEquals("ONVIF camera: IPCAM C6F0SgZ3N0P6L0", OnvifParser.summary(reply))
        assertNull(OnvifParser.summary("<Envelope><Hello/></Envelope>"))
        assertTrue(OnvifParser.probe("abc").contains("NetworkVideoTransmitter"))
    }

    @Test
    fun onvifDeviceCountsAsConfirmedCamera() {
        val v = DeviceClassifier.classify(NetworkDevice("192.168.1.20", onvif = "ONVIF camera"))
        assertEquals(Reasons.NET_ONVIF, v.reason)
        assertTrue(v.score >= ScanConfig.NETWORK_STREAM_SCORE)
    }
}
