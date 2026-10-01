package com.camsense.data

import com.camsense.core.Finding
import com.camsense.core.ModuleId
import com.camsense.core.ModuleResult
import com.camsense.core.NormRect
import com.camsense.core.SkipReason
import com.camsense.data.db.RunStatus
import com.camsense.data.db.ScanEntity
import com.camsense.data.db.ScanWithDetails
import com.camsense.scoring.RiskEngine
import com.camsense.scoring.RiskLevel
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class MappersTest {

    @Test
    fun moduleThatNeverStartedIsSavedAsNotReached() {
        val run = ModuleResult.idle(ModuleId.INFRARED).toRunEntity("scan-1")
        assertEquals(RunStatus.SKIPPED, run.status)
        assertEquals("NOT_REACHED", run.skipReason)
        assertNull(run.score)
        assertEquals(0.15f, run.weight)
    }

    @Test
    fun runningModuleIsSavedAsDone() {
        val run = ModuleResult(ModuleId.NETWORK, ModuleResult.Status.RUNNING, 0.9f, emptyList()).toRunEntity("s")
        assertEquals(RunStatus.DONE, run.status)
        assertEquals(0.9f, run.score)
    }

    @Test
    fun skipReasonIsKept() {
        val run = ModuleResult.skipped(ModuleId.NETWORK, SkipReason.NO_WIFI).toRunEntity("s")
        assertEquals("NO_WIFI", run.skipReason)
    }

    @Test
    fun savedScanRescoresTheSame() {
        val results = listOf(
            ModuleResult(ModuleId.GLINT, ModuleResult.Status.DONE, 0.9f,
                listOf(Finding("glint", 0.9f, "object-1", NormRect(0.1f, 0.1f, 0.2f, 0.2f)))),
            ModuleResult(ModuleId.OBJECTS, ModuleResult.Status.DONE, 0.6f, listOf(Finding("box", 0.6f, "object-1"))),
            ModuleResult.skipped(ModuleId.INFRARED, SkipReason.IR_NOT_SUPPORTED),
        )
        val runs = results.map { it.toRunEntity("s") }
        val findings = results.flatMap { r ->
            val runId = runs.first { it.module == r.moduleId }.id
            r.findings.map { it.toEntity("s", runId, null) }
        }
        val details = ScanWithDetails(
            ScanEntity(id = "s", placeName = null, startedAt = 0, deviceModel = "x", appVersion = "1"),
            runs, findings, emptyList(), emptyList(), emptyList(),
        )
        val before = RiskEngine.assess(results)
        val after = RiskEngine.assess(details.toModuleResults())
        assertEquals(before.score, after.score, 1e-6f)
        assertEquals(RiskLevel.MEDIUM, after.level) // 0.27 + 0.06 + 0.2 bonus = 0.53
        assertEquals(NormRect(0.1f, 0.1f, 0.2f, 0.2f), details.toModuleResults().first().findings.first().screenBox)
    }
}
