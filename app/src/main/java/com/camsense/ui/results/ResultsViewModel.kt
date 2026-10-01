package com.camsense.ui.results

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.camsense.AppContainer
import com.camsense.core.ModuleId
import com.camsense.data.db.RunStatus
import com.camsense.data.db.ScanWithDetails
import com.camsense.data.db.UserVerdict
import com.camsense.scoring.RiskLevel
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.io.File
import java.io.IOException

data class FindingUi(
    val id: String,
    val module: ModuleId,
    val reason: String,
    val confidence: Float,
    val verdict: UserVerdict,
    val snapshot: File?,
    /** Technical values for the detail sheet only (IP, ports, banner). */
    val technical: String?,
)

data class SkippedUi(val module: ModuleId, val skipReason: String?)

data class ResultsUi(
    val scanId: String,
    val placeName: String?,
    val startedAt: Long,
    val level: RiskLevel,
    val score: Float,
    val findings: List<FindingUi>,
    val skipped: List<SkippedUi>,
    val modulesRan: Int,
    val checkedCount: Int,
    val evidenceSaved: Boolean,
)

class ResultsViewModel(private val container: AppContainer, private val scanId: String) : ViewModel() {

    val state: StateFlow<ResultsUi?> = container.repository.observeScan(scanId)
        .map { it?.toUi() }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), null)

    fun setVerdict(findingId: String, verdict: UserVerdict) {
        viewModelScope.launch { container.repository.setVerdict(findingId, verdict) }
    }

    /** Writes report.json and hashes every file. Storage full -> onError, Results stays open. */
    fun saveEvidence(onSaved: () -> Unit, onError: () -> Unit) {
        viewModelScope.launch {
            try {
                val details = container.repository.getScan(scanId) ?: return@launch onError()
                container.evidenceStore.saveEvidence(details, container.appVersion)
                onSaved()
            } catch (e: IOException) {
                Log.w("CamSense/Evidence", "Save failed", e)
                onError()
            }
        }
    }

    fun delete(onDone: () -> Unit) {
        viewModelScope.launch {
            container.repository.deleteScan(scanId)
            onDone()
        }
    }

    private fun ScanWithDetails.toUi(): ResultsUi {
        val runById = moduleRuns.associateBy { it.id }
        val deviceById = networkDevices.associateBy { it.id }
        val findingsUi = findings.mapNotNull { f ->
            val module = runById[f.moduleRunId]?.module ?: return@mapNotNull null
            val snap = container.evidenceStore.snapshotFile(scan.id, f.id).takeIf { it.exists() }
            val technical = f.networkDeviceId?.let(deviceById::get)?.let { d ->
                buildString {
                    append(d.ip)
                    if (d.openPorts.isNotEmpty()) append("  ports ").append(d.openPorts.joinToString(", "))
                    d.mdnsName?.let { append("\n").append(it) }
                    d.ssdpServer?.let { append("\n").append(it) }
                    d.banner?.let { append("\n").append(it) }
                }
            }
            FindingUi(f.id, module, f.reason, f.confidence, f.userVerdict, snap, technical)
        }.sortedByDescending { it.confidence }

        return ResultsUi(
            scanId = scan.id,
            placeName = scan.placeName,
            startedAt = scan.startedAt,
            level = scan.riskLevel ?: RiskLevel.LOW,
            score = scan.riskScore ?: 0f,
            findings = findingsUi,
            skipped = moduleRuns.filter { it.status != RunStatus.DONE }
                .sortedBy { it.module.ordinal }
                .map { SkippedUi(it.module, it.skipReason) },
            modulesRan = moduleRuns.count { it.status == RunStatus.DONE },
            checkedCount = findingsUi.count { it.verdict != UserVerdict.UNREVIEWED },
            evidenceSaved = evidenceItems.isNotEmpty(),
        )
    }
}
