package com.camsense.core

import android.util.Log
import com.camsense.scoring.ScanConfig
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

/** Shared bookkeeping for modules: result flow, timing and skip/fail helpers. */
abstract class BaseModule(final override val id: ModuleId) : DetectionModule {

    override val weight: Float get() = ScanConfig.weightOf(id)

    protected val state = MutableStateFlow(ModuleResult.idle(id))
    override val results: StateFlow<ModuleResult> = state.asStateFlow()

    protected val tag = "CamSense/${id.name.lowercase().replaceFirstChar { it.uppercase() }}"
    private var startedAt = 0L

    protected fun markRunning() {
        if (startedAt == 0L) startedAt = System.currentTimeMillis()
        state.value = state.value.copy(status = ModuleResult.Status.RUNNING, skipReason = null)
    }

    protected fun publish(score: Float, findings: List<Finding>, done: Boolean = false) {
        val current = state.value
        if (current.status == ModuleResult.Status.SKIPPED || current.status == ModuleResult.Status.FAILED) return
        state.value = ModuleResult(
            moduleId = id,
            status = if (done) ModuleResult.Status.DONE else ModuleResult.Status.RUNNING,
            score = score.coerceIn(0f, 1f),
            findings = findings,
            durationMs = if (startedAt > 0) System.currentTimeMillis() - startedAt else null,
        )
    }

    fun skip(reason: SkipReason) {
        Log.i(tag, "Skipped: $reason")
        state.value = ModuleResult.skipped(id, reason)
    }

    protected fun fail(reason: SkipReason, error: Throwable? = null) {
        Log.e(tag, "Failed: $reason", error)
        if (state.value.status == ModuleResult.Status.DONE) return
        state.value = ModuleResult.failed(id, reason)
    }

    /** Marks a running module DONE with whatever it has found so far. */
    protected fun finish() {
        val r = state.value
        if (r.status == ModuleResult.Status.RUNNING) publish(r.score, r.findings, done = true)
    }
}
