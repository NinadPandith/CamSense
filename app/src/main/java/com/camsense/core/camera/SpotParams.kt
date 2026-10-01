package com.camsense.core.camera

import com.camsense.modules.glint.Blob

/** Tuning for [FrameOps.brightSpots]; one set each for glint, IR and IR calibration (ScanConfig). */
data class SpotParams(
    /** The spot itself must be at least this bright (0–255). */
    val minLuma: Double,
    /** ...and this much brighter than the mean of the box around it. */
    val minContrast: Double,
    /** Side of that box in pixels; about twice the largest spot. */
    val windowPx: Int,
    val minDiameter: Float,
    val maxDiameter: Float,
    val minCircularity: Float,
    val circularityMinArea: Double,
    val minAspect: Float,
    /** Keep at most this many spots per frame, strongest first. */
    val maxSpots: Int,
    /**
     * Skip spots whose surroundings (the box mean) are brighter than this: a dot on a
     * lit screen or white wall isn't a lens. 255 turns the check off.
     */
    val maxSurround: Double = 255.0,
)

/** Spots kept, and how many raw candidates there were (for the diagnostics line). */
data class SpotResult(val spots: List<Blob>, val candidates: Int)
