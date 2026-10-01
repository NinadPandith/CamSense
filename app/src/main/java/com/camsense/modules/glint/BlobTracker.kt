package com.camsense.modules.glint

import kotlin.math.hypot

/**
 * Follows bright spots across frames (PRD FR-1.3/1.4). A spot is confirmed after
 * [confirmFrames] sightings in which it stays small and keeps a steady size; it is
 * dropped after [maxMissed] frames without a match. Reflections from glass and metal
 * smear and change size as the phone moves, so they never confirm.
 *
 * Pure Kotlin so it can be unit-tested with synthetic blob sequences.
 */
class BlobTracker(
    private val confirmFrames: Int,
    private val maxMissed: Int,
    private val matchDistance: Float,
    private val maxSizeRatio: Float,
    private val maxDiameterPx: Float,
) {
    data class Track(
        val id: Int,
        val x: Float,
        val y: Float,
        val diameterPx: Float,
        val minDiameter: Float,
        val maxDiameter: Float,
        val hits: Int,
        val missed: Int,
        val confirmed: Boolean,
        /** Set once the size became unsteady; a rejected track never confirms. */
        val rejected: Boolean,
    )

    data class Update(val tracks: List<Track>, val newlyConfirmed: List<Track>)

    private var nextId = 1
    private var tracks = listOf<Track>()

    fun reset() {
        tracks = emptyList()
        nextId = 1
    }

    fun update(blobs: List<Blob>): Update {
        // Greedy nearest-neighbour matching: closest pairs first.
        val pairs = buildList {
            tracks.forEachIndexed { ti, t ->
                blobs.forEachIndexed { bi, b ->
                    val d = hypot(t.x - b.x, t.y - b.y)
                    if (d <= matchDistance) add(Triple(ti, bi, d))
                }
            }
        }.sortedBy { it.third }

        val trackUsed = BooleanArray(tracks.size)
        val blobUsed = BooleanArray(blobs.size)
        val updated = ArrayList<Track>(tracks.size + blobs.size)
        val newlyConfirmed = ArrayList<Track>()

        for ((ti, bi, _) in pairs) {
            if (trackUsed[ti] || blobUsed[bi]) continue
            trackUsed[ti] = true
            blobUsed[bi] = true
            val t = tracks[ti]
            val b = blobs[bi]
            val minD = minOf(t.minDiameter, b.diameterPx)
            val maxD = maxOf(t.maxDiameter, b.diameterPx)
            val rejected = t.rejected || b.diameterPx > maxDiameterPx ||
                (minD > 0f && maxD / minD > maxSizeRatio)
            val hits = t.hits + 1
            val confirmed = t.confirmed || (!rejected && hits >= confirmFrames)
            val nt = t.copy(
                x = b.x, y = b.y, diameterPx = b.diameterPx,
                minDiameter = minD, maxDiameter = maxD,
                hits = hits, missed = 0, confirmed = confirmed, rejected = rejected,
            )
            if (confirmed && !t.confirmed) newlyConfirmed += nt
            updated += nt
        }

        tracks.forEachIndexed { ti, t ->
            if (!trackUsed[ti] && t.missed + 1 <= maxMissed) updated += t.copy(missed = t.missed + 1)
        }

        blobs.forEachIndexed { bi, b ->
            if (!blobUsed[bi]) {
                val tooBig = b.diameterPx > maxDiameterPx
                val t = Track(
                    id = nextId++, x = b.x, y = b.y, diameterPx = b.diameterPx,
                    minDiameter = b.diameterPx, maxDiameter = b.diameterPx,
                    hits = 1, missed = 0, confirmed = !tooBig && confirmFrames <= 1, rejected = tooBig,
                )
                if (t.confirmed) newlyConfirmed += t
                updated += t
            }
        }

        tracks = updated
        return Update(tracks, newlyConfirmed)
    }

    /** Confirmed spots visible in the latest frame; these are circled on screen. */
    fun visibleConfirmed(): List<Track> = tracks.filter { it.confirmed && it.missed == 0 }
}
