package com.camsense.ui.scan

import com.camsense.core.ScanMode

/** Steps of the guided scan; which ones run depends on the ScanMode. */
enum class ScanStep { PLACE, NETWORK, ROOM, MIRROR, IR, LENS, CLOSE, RESULTS }

object ScanSteps {
    /** The steps of each scan type, in order (IR and camera steps may still be skipped). */
    fun of(mode: ScanMode): List<ScanStep> = when (mode) {
        ScanMode.FULL -> listOf(ScanStep.PLACE, ScanStep.NETWORK, ScanStep.ROOM, ScanStep.IR, ScanStep.LENS, ScanStep.CLOSE, ScanStep.RESULTS)
        ScanMode.QUICK -> listOf(ScanStep.PLACE, ScanStep.NETWORK, ScanStep.LENS, ScanStep.RESULTS)
        ScanMode.SMALL_ROOM -> listOf(ScanStep.PLACE, ScanStep.NETWORK, ScanStep.MIRROR, ScanStep.IR, ScanStep.LENS, ScanStep.CLOSE, ScanStep.RESULTS)
    }

    /** Number shown in the step dots (1–6); the IR and lens checks share a number. */
    fun dotNumber(step: ScanStep): Int = when (step) {
        ScanStep.PLACE -> 1
        ScanStep.NETWORK -> 2
        ScanStep.ROOM, ScanStep.MIRROR -> 3
        ScanStep.IR, ScanStep.LENS -> 4
        ScanStep.CLOSE -> 5
        ScanStep.RESULTS -> 6
    }

    val cameraSteps = setOf(ScanStep.ROOM, ScanStep.IR, ScanStep.LENS)
}
