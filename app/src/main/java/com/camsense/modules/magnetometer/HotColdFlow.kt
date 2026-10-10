package com.camsense.modules.magnetometer

import com.camsense.scoring.ScanConfig
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn

/** Hot–cold state that follows the magnetometer's live readings (close check and re-check). */
fun hotColdFlow(live: StateFlow<MagnetSweepModule.Live>, scope: CoroutineScope): StateFlow<HotCold.State> {
    val tracker = HotCold(
        ScanConfig.HOTCOLD_FAST_ALPHA, ScanConfig.HOTCOLD_SLOW_ALPHA, ScanConfig.HOTCOLD_TREND_UT,
        ScanConfig.HOTCOLD_PASSED_DROP, ScanConfig.HOTCOLD_PEAK_WINDOW_MS, ScanConfig.HOTCOLD_MIN_PEAK_UT,
    )
    return live.map { l ->
        if (l.calibrating) { tracker.reset(); HotCold.State() }
        else tracker.update(l.delta, System.currentTimeMillis())
    }.stateIn(scope, SharingStarted.Eagerly, HotCold.State())
}
