package com.camsense.core

import kotlinx.coroutines.flow.StateFlow

/**
 * The one interface every detection module implements. The RiskEngine and the UI
 * only ever talk to modules through this, so each module can be built and
 * tested on its own. Don't change it without updating every module.
 */
interface DetectionModule {
    val id: ModuleId
    val weight: Float                    // from the PRD scoring table, via ScanConfig
    fun isSupported(): Boolean           // e.g. IR calibration failed -> false
    val results: StateFlow<ModuleResult>
    suspend fun start()
    fun stop()
}
