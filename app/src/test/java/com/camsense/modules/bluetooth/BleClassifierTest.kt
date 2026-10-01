package com.camsense.modules.bluetooth

import com.camsense.scoring.ScanConfig
import org.junit.Assert.assertEquals
import org.junit.Test

class BleClassifierTest {
    @Test
    fun cameraNameIsFlagged() {
        assertEquals(ScanConfig.BLE_SCORE_CAMERA_NAME, BleClassifier.classify("Mini Cam 01", -80, false).score)
    }

    @Test
    fun strongUnknownIsFlaggedWeakIsNot() {
        assertEquals(ScanConfig.BLE_SCORE_STRONG_UNKNOWN, BleClassifier.classify(null, -45, false).score)
        assertEquals(0f, BleClassifier.classify(null, -55, false).score)
        assertEquals(0f, BleClassifier.classify("Galaxy Buds", -40, false).score)
    }

    @Test
    fun ouiPrefix() {
        assertEquals("A4:CF:12", BleClassifier.ouiOf("a4:cf:12:34:56:78"))
    }
}
