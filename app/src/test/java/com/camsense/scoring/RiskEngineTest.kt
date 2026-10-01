package com.camsense.scoring

import com.camsense.core.Finding
import com.camsense.core.ModuleId
import com.camsense.core.ModuleResult
import com.camsense.core.SkipReason
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class RiskEngineTest {

    private fun done(id: ModuleId, score: Float, vararg keys: String?) = ModuleResult(
        id, ModuleResult.Status.DONE, score,
        keys.map { Finding(reason = "r", confidence = score, targetKey = it) },
    )

    @Test
    fun weightsAddUpToOne() {
        assertEquals(1f, ScanConfig.WEIGHTS.values.sum(), 1e-6f)
    }

    @Test
    fun demoScanFromBackendSchemaScores082High() {
        // Backend Schema sample: 0.30×0.90 + 0.25×0.85 + 0.10×0.10 + 0.10×0.70 + 0.10×0.60 = 0.6225, + 0.2 bonus.
        val results = listOf(
            done(ModuleId.GLINT, 0.90f, "object-1"),
            ModuleResult.skipped(ModuleId.INFRARED, SkipReason.IR_NOT_SUPPORTED),
            done(ModuleId.NETWORK, 0.85f, null),
            done(ModuleId.BLE, 0.10f),
            done(ModuleId.MAGNET, 0.70f, "object-1"),
            done(ModuleId.OBJECTS, 0.60f, "object-1"),
        )
        val a = RiskEngine.assess(results)
        assertEquals(0.6225f, a.weightedSum, 1e-4f)
        assertTrue(a.agreement)
        assertEquals(setOf("object-1"), a.agreedTargets)
        assertEquals(0.8225f, a.score, 1e-4f)
        assertEquals(RiskLevel.HIGH, a.level)
        assertNull(a.floorApplied)
    }

    @Test
    fun skippedAndFailedModulesAddNothing() {
        val results = listOf(
            ModuleResult(ModuleId.GLINT, ModuleResult.Status.SKIPPED, 1f, emptyList()),
            ModuleResult(ModuleId.NETWORK, ModuleResult.Status.FAILED, 1f, emptyList()),
            done(ModuleId.BLE, 0.5f),
        )
        assertEquals(0.05f, RiskEngine.assess(results).score, 1e-6f)
    }

    @Test
    fun cleanRoomIsLow() {
        val a = RiskEngine.assess(ModuleId.entries.map { done(it, 0f) })
        assertEquals(0f, a.score, 0f)
        assertEquals(RiskLevel.LOW, a.level)
        assertFalse(a.agreement)
    }

    @Test
    fun agreementNeedsTwoDifferentModules() {
        val sameModuleTwice = ModuleResult(
            ModuleId.OBJECTS, ModuleResult.Status.DONE, 0.5f,
            listOf(Finding("a", 0.5f, "object-1"), Finding("b", 0.5f, "object-1")),
        )
        assertFalse(RiskEngine.assess(listOf(sameModuleTwice)).agreement)

        val twoModules = listOf(done(ModuleId.OBJECTS, 0.5f, "object-1"), done(ModuleId.MAGNET, 0.5f, "object-1"))
        assertTrue(RiskEngine.assess(twoModules).agreement)
    }

    @Test
    fun scoreIsCappedAtOne() {
        val a = RiskEngine.assess(ModuleId.entries.map { done(it, 1f, "object-1") })
        assertEquals(1f, a.score, 0f)
    }

    @Test
    fun confirmedStreamAloneIsHigh() {
        val a = RiskEngine.assess(listOf(done(ModuleId.NETWORK, 0.9f, null)))
        assertEquals(RiskLevel.HIGH, a.level)
        assertEquals(RiskLevel.HIGH, a.floorApplied)
    }

    @Test
    fun strongGlintAloneIsAtLeastMedium() {
        val a = RiskEngine.assess(listOf(done(ModuleId.GLINT, 0.85f, "glint-1")))
        assertEquals(RiskLevel.MEDIUM, a.level)
        assertEquals(RiskLevel.MEDIUM, a.floorApplied)
    }

    @Test
    fun weakSignalsGetNoFloor() {
        val a = RiskEngine.assess(listOf(done(ModuleId.NETWORK, 0.6f, null), done(ModuleId.GLINT, 0.5f)))
        assertNull(a.floorApplied)
        assertEquals(0.30f, a.score, 1e-6f)
        assertEquals(RiskLevel.MEDIUM, a.level)
    }

    @Test
    fun levelBoundariesMatchPrd() {
        assertEquals(RiskLevel.LOW, RiskEngine.levelFor(0.29f))
        assertEquals(RiskLevel.MEDIUM, RiskEngine.levelFor(0.3f))
        assertEquals(RiskLevel.MEDIUM, RiskEngine.levelFor(0.6f))
        assertEquals(RiskLevel.HIGH, RiskEngine.levelFor(0.61f))
    }

    @Test
    fun combineRecomputesWhenAModulePublishes() = runTest {
        val glint = MutableStateFlow(ModuleResult.idle(ModuleId.GLINT))
        val net = MutableStateFlow(ModuleResult.idle(ModuleId.NETWORK))
        val live = RiskEngine.combine(listOf(glint, net))
        assertEquals(0f, live.first().score, 0f)
        net.value = done(ModuleId.NETWORK, 0.4f)
        assertEquals(0.1f, live.first().score, 1e-6f)
    }
}
