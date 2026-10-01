package com.camsense.scoring

import com.camsense.core.ModuleId
import com.camsense.core.ModuleResult
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine

data class RiskAssessment(
    /** Final score, 0–1, after bonus and floors. */
    val score: Float,
    val level: RiskLevel,
    /** Sum of weight × score over the modules that ran. */
    val weightedSum: Float,
    /** True when two or more modules flagged the same target_key. */
    val agreement: Boolean,
    val agreedTargets: Set<String>,
    /** Set when a single strong signal lifted the level (see ScanConfig). */
    val floorApplied: RiskLevel?,
)

/**
 * Risk = Σ wᵢ·sᵢ + 0.2·agreement, capped at 1 (PRD "Risk scoring logic").
 * Skipped, failed and idle modules add nothing. On top of the PRD formula, a
 * confirmed video stream on the Wi-Fi lifts the result to at least High, and a
 * strong lens glint to at least Medium.
 */
object RiskEngine {

    fun assess(results: Collection<ModuleResult>): RiskAssessment {
        val counted = results.filter { it.isActive }
        val weightedSum = counted.sumOf {
            (ScanConfig.weightOf(it.moduleId) * it.score.coerceIn(0f, 1f)).toDouble()
        }.toFloat()

        val agreedTargets = agreedTargets(counted)
        val agreement = agreedTargets.isNotEmpty()
        var score = (weightedSum + if (agreement) ScanConfig.AGREEMENT_BONUS else 0f).coerceAtMost(1f)

        var floor: RiskLevel? = null
        val network = counted.firstOrNull { it.moduleId == ModuleId.NETWORK }
        val glint = counted.firstOrNull { it.moduleId == ModuleId.GLINT }
        if (network != null && network.score >= ScanConfig.NETWORK_STREAM_SCORE &&
            score < ScanConfig.NETWORK_STREAM_FLOOR
        ) {
            score = ScanConfig.NETWORK_STREAM_FLOOR
            floor = RiskLevel.HIGH
        } else if (glint != null && glint.score >= ScanConfig.GLINT_STRONG_SCORE &&
            score < ScanConfig.GLINT_STRONG_FLOOR
        ) {
            score = ScanConfig.GLINT_STRONG_FLOOR
            floor = RiskLevel.MEDIUM
        }

        return RiskAssessment(
            score = score,
            level = levelFor(score),
            weightedSum = weightedSum,
            agreement = agreement,
            agreedTargets = agreedTargets,
            floorApplied = floor,
        )
    }

    fun levelFor(score: Float): RiskLevel = when {
        score < ScanConfig.LOW_MAX -> RiskLevel.LOW
        score <= ScanConfig.MEDIUM_MAX -> RiskLevel.MEDIUM
        else -> RiskLevel.HIGH
    }

    /** target_keys that findings from two or more different modules share. */
    fun agreedTargets(results: Collection<ModuleResult>): Set<String> =
        results.flatMap { r -> r.findings.mapNotNull { f -> f.targetKey?.let { it to r.moduleId } } }
            .groupBy({ it.first }, { it.second })
            .filterValues { modules -> modules.toSet().size >= 2 }
            .keys

    /** Live risk while a scan runs: recomputed whenever any module publishes. */
    fun combine(flows: List<StateFlow<ModuleResult>>): Flow<RiskAssessment> =
        combine(flows) { assess(it.toList()) }
}
