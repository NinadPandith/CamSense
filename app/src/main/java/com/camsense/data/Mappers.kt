package com.camsense.data

import com.camsense.core.Finding
import com.camsense.core.ModuleId
import com.camsense.core.ModuleResult
import com.camsense.core.NormRect
import com.camsense.data.db.FindingEntity
import com.camsense.data.db.ModuleRunEntity
import com.camsense.data.db.RunStatus
import com.camsense.data.db.ScanWithDetails
import com.camsense.data.db.UserVerdict
import com.camsense.scoring.ScanConfig

/** Rebuilds module results from a saved scan so the RiskEngine can re-score it. */
fun ScanWithDetails.toModuleResults(): List<ModuleResult> = moduleRuns.map { run ->
    ModuleResult(
        moduleId = run.module,
        status = when (run.status) {
            RunStatus.DONE -> ModuleResult.Status.DONE
            RunStatus.SKIPPED -> ModuleResult.Status.SKIPPED
            RunStatus.FAILED -> ModuleResult.Status.FAILED
        },
        score = run.score ?: 0f,
        findings = findings.filter { it.moduleRunId == run.id }.map { it.toFinding() },
        durationMs = run.durationMs,
    )
}

fun FindingEntity.toFinding() = Finding(
    reason = reason,
    confidence = confidence,
    targetKey = targetKey,
    screenBox = if (boxLeft != null && boxTop != null && boxRight != null && boxBottom != null)
        NormRect(boxLeft, boxTop, boxRight, boxBottom) else null,
    deviceAddress = deviceAddress,
    id = id,
    createdAt = createdAt,
    yaw = yaw,
    pitch = pitch,
)

fun ModuleResult.toRunEntity(scanId: String) = ModuleRunEntity(
    scanId = scanId,
    module = moduleId,
    status = when (status) {
        ModuleResult.Status.SKIPPED -> RunStatus.SKIPPED
        ModuleResult.Status.FAILED -> RunStatus.FAILED
        // A module still RUNNING when the scan ends just finished early.
        ModuleResult.Status.DONE, ModuleResult.Status.RUNNING -> RunStatus.DONE
        // Never started: the user didn't reach its step.
        ModuleResult.Status.IDLE -> RunStatus.SKIPPED
    },
    score = if (status == ModuleResult.Status.SKIPPED || status == ModuleResult.Status.FAILED ||
        status == ModuleResult.Status.IDLE) null else score,
    weight = ScanConfig.weightOf(moduleId),
    skipReason = skipReason?.name ?: if (status == ModuleResult.Status.IDLE) "NOT_REACHED" else null,
    durationMs = durationMs,
)

fun Finding.toEntity(scanId: String, moduleRunId: String, networkDeviceId: String?) = FindingEntity(
    id = id,
    scanId = scanId,
    moduleRunId = moduleRunId,
    targetKey = targetKey,
    reason = reason,
    confidence = confidence,
    boxLeft = screenBox?.left,
    boxTop = screenBox?.top,
    boxRight = screenBox?.right,
    boxBottom = screenBox?.bottom,
    networkDeviceId = networkDeviceId,
    userVerdict = UserVerdict.UNREVIEWED,
    createdAt = createdAt,
    yaw = yaw,
    pitch = pitch,
    deviceAddress = deviceAddress,
)

/** Modules whose score counts as "fired" (for reports and the Phase 3 cloud mapping). */
fun ScanWithDetails.firedModules(): List<ModuleId> =
    moduleRuns.filter { (it.score ?: 0f) >= ScanConfig.MODULE_FIRED_SCORE }.map { it.module }
