package com.camsense.modules.objects

import com.camsense.core.NormRect

/**
 * Gives each detected object a stable target_key ("object-1", "object-2", …) so
 * findings from different modules about the same object can be linked (PRD
 * agreement bonus). Matches by box overlap frame to frame. When the phone moves
 * on and later sees an object of the same kind again, the earlier key is reused,
 * so a tissue box marked in the room sweep keeps its key in the lens check.
 * Pure Kotlin for unit tests.
 */
class ObjectTracker(
    private val matchIou: Float,
    private val maxMissed: Int,
    private val confirmFrames: Int,
) {
    data class TrackedObject(
        val key: String,
        val label: String,
        val box: NormRect,
        val bestConfidence: Float,
        val hits: Int,
        val missed: Int,
        val confirmed: Boolean,
    )

    private var nextId = 1
    private var pendingId = 1
    private val active = mutableListOf<TrackedObject>()
    /** Every object confirmed during the scan, latest box per key. */
    private val confirmedByKey = linkedMapOf<String, TrackedObject>()

    fun update(detections: List<Detection>): List<TrackedObject> {
        val used = BooleanArray(detections.size)
        val next = mutableListOf<TrackedObject>()

        for (t in active) {
            var bestIdx = -1
            var bestIou = matchIou
            detections.forEachIndexed { i, d ->
                if (!used[i] && d.label == t.label) {
                    val iou = t.box.iou(d.box)
                    if (iou >= bestIou) { bestIou = iou; bestIdx = i }
                }
            }
            if (bestIdx >= 0) {
                used[bestIdx] = true
                val d = detections[bestIdx]
                var nt = t.copy(box = d.box, bestConfidence = maxOf(t.bestConfidence, d.confidence),
                    hits = t.hits + 1, missed = 0)
                if (!nt.confirmed && nt.hits >= confirmFrames) nt = confirm(nt)
                if (nt.confirmed) confirmedByKey[nt.key] = nt
                next += nt
            } else if (t.missed + 1 <= maxMissed) {
                next += t.copy(missed = t.missed + 1)
            }
        }

        detections.forEachIndexed { i, d ->
            if (!used[i]) {
                var t = TrackedObject("pending-${pendingId++}", d.label, d.box, d.confidence, 1, 0, false)
                if (confirmFrames <= 1) t = confirm(t).also { confirmedByKey[it.key] = it }
                next += t
            }
        }

        active.clear()
        active += next
        return active.toList()
    }

    private fun confirm(t: TrackedObject): TrackedObject {
        val activeKeys = active.map { it.key }.toSet()
        // Reuse the key of an earlier object of the same kind that is out of view.
        val reuse = confirmedByKey.values.lastOrNull { it.label == t.label && it.key !in activeKeys }
        val key = reuse?.key ?: "object-${nextId++}"
        return t.copy(key = key, confirmed = true,
            bestConfidence = maxOf(t.bestConfidence, reuse?.bestConfidence ?: 0f))
    }

    /** Objects in the latest frame that are confirmed; these get lime boxes. */
    fun visibleConfirmed(): List<TrackedObject> = active.filter { it.confirmed && it.missed == 0 }

    fun allConfirmed(): List<TrackedObject> = confirmedByKey.values.toList()

    /** The visible confirmed object whose (slightly grown) box holds the point, if any. */
    fun objectAt(x: Float, y: Float, margin: Float): TrackedObject? = pickAt(active, x, y, margin)

    /** Snapshot of the current tracks, safe to read from another thread. */
    fun activeSnapshot(): List<TrackedObject> = active.toList()

    companion object {
        /** Smallest recently seen confirmed object whose grown box holds the point. */
        fun pickAt(objects: List<TrackedObject>, x: Float, y: Float, margin: Float): TrackedObject? =
            objects.filter { it.confirmed && it.missed <= 2 && it.box.expand(margin).contains(x, y) }
                .minByOrNull { it.box.area }
    }

    fun reset() {
        active.clear()
        confirmedByKey.clear()
        nextId = 1
        pendingId = 1
    }
}
