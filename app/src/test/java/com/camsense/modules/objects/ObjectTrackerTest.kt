package com.camsense.modules.objects

import com.camsense.core.NormRect
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class ObjectTrackerTest {

    private fun tracker() = ObjectTracker(matchIou = 0.3f, maxMissed = 2, confirmFrames = 3)
    private val box = NormRect(0.4f, 0.4f, 0.6f, 0.6f)

    @Test
    fun objectGetsKeyAfterThreeFrames() {
        val t = tracker()
        repeat(2) { t.update(listOf(Detection("tissue_box", 0.7f, box))) }
        assertTrue(t.visibleConfirmed().isEmpty())
        t.update(listOf(Detection("tissue_box", 0.8f, box)))
        assertEquals("object-1", t.visibleConfirmed().single().key)
        assertEquals(0.8f, t.visibleConfirmed().single().bestConfidence, 1e-6f)
    }

    @Test
    fun differentLabelsDontMatch() {
        val t = tracker()
        repeat(3) { t.update(listOf(Detection("clock", 0.7f, box), Detection("vase", 0.7f, box))) }
        assertEquals(setOf("object-1", "object-2"), t.visibleConfirmed().map { it.key }.toSet())
    }

    @Test
    fun sameKindSeenAgainLaterReusesKey() {
        val t = tracker()
        repeat(3) { t.update(listOf(Detection("tissue_box", 0.7f, box))) }
        repeat(4) { t.update(emptyList()) } // out of view, track dropped
        val elsewhere = NormRect(0.0f, 0.0f, 0.2f, 0.2f)
        repeat(3) { t.update(listOf(Detection("tissue_box", 0.6f, elsewhere))) }
        assertEquals("object-1", t.visibleConfirmed().single().key)
        assertEquals(1, t.allConfirmed().size)
    }

    @Test
    fun glintInsideBoxFindsObject() {
        val t = tracker()
        repeat(3) { t.update(listOf(Detection("smoke_detector", 0.7f, box))) }
        assertNotNull(t.objectAt(0.5f, 0.5f, 0.15f))
        assertNotNull(t.objectAt(0.62f, 0.5f, 0.15f)) // just outside, inside the margin
        assertNull(t.objectAt(0.9f, 0.9f, 0.15f))
    }
}
