package com.camsense.modules.glint

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class BlobTrackerTest {

    private fun tracker() = BlobTracker(
        confirmFrames = 5, maxMissed = 3, matchDistance = 0.04f, maxSizeRatio = 2.5f, maxDiameterPx = 15f,
    )

    @Test
    fun steadySmallSpotConfirmsOnFifthFrame() {
        val t = tracker()
        repeat(4) { i ->
            val u = t.update(listOf(Blob(0.5f + i * 0.005f, 0.5f, 4f)))
            assertTrue(u.newlyConfirmed.isEmpty())
        }
        val u = t.update(listOf(Blob(0.52f, 0.5f, 4f)))
        assertEquals(1, u.newlyConfirmed.size)
        assertEquals(1, t.visibleConfirmed().size)
        // Confirmation is reported once only.
        assertTrue(t.update(listOf(Blob(0.52f, 0.5f, 4f))).newlyConfirmed.isEmpty())
    }

    @Test
    fun spotThatChangesSizeNeverConfirms() {
        val t = tracker()
        val sizes = listOf(3f, 6f, 9f, 12f, 14f, 14f, 14f)
        sizes.forEach { t.update(listOf(Blob(0.3f, 0.3f, it))) }
        assertTrue(t.visibleConfirmed().isEmpty())
    }

    @Test
    fun largeSpotIsRejected() {
        val t = tracker()
        repeat(8) { t.update(listOf(Blob(0.3f, 0.3f, 20f))) }
        assertTrue(t.visibleConfirmed().isEmpty())
    }

    @Test
    fun trackSurvivesShortGapsAndDropsAfterMaxMissed() {
        val t = tracker()
        repeat(3) { t.update(listOf(Blob(0.4f, 0.4f, 5f))) }
        repeat(3) { t.update(emptyList()) }
        repeat(2) { t.update(listOf(Blob(0.4f, 0.4f, 5f))) }
        assertEquals(1, t.visibleConfirmed().size)

        repeat(4) { t.update(emptyList()) }
        assertTrue(t.update(emptyList()).tracks.isEmpty())
    }

    @Test
    fun jumpingSpotStartsANewTrack() {
        val t = tracker()
        repeat(4) { t.update(listOf(Blob(0.1f, 0.1f, 5f))) }
        val u = t.update(listOf(Blob(0.6f, 0.6f, 5f)))
        assertTrue(u.newlyConfirmed.isEmpty())
        assertEquals(2, u.tracks.size)
    }

    @Test
    fun twoSpotsAreTrackedSeparately() {
        val t = tracker()
        repeat(5) { t.update(listOf(Blob(0.2f, 0.2f, 4f), Blob(0.8f, 0.8f, 4f))) }
        assertEquals(2, t.visibleConfirmed().size)
    }
}
