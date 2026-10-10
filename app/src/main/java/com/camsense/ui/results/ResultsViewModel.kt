package com.camsense.ui.results

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.camsense.AppContainer
import com.camsense.core.ModuleId
import com.camsense.core.ScanMode
import com.camsense.core.sensors.PointingMath
import com.camsense.data.SafeDevices
import com.camsense.data.ScanDiff
import com.camsense.data.db.RunStatus
import com.camsense.data.db.ScanWithDetails
import com.camsense.data.db.UserVerdict
import com.camsense.scoring.RiskLevel
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.io.File

data class FindingUi(
    val id: String,
    val module: ModuleId,
    val reason: String,
    val confidence: Float,
    val verdict: UserVerdict,
    val snapshot: File?,
    /** Technical values for the detail sheet only (IP, ports, banner). */
    val technical: String?,
    /** Every finding in this row (identical findings are grouped). */
    val ids: List<String> = listOf(id),
    val count: Int = 1,
    /** Direction the spot was seen in (camera findings), for the room map. */
    val yaw: Float? = null,
    val pitch: Float? = null,
    /** Devices behind this row, remembered as safe on "Checked, it's fine" (see SafeDevices). */
    val signatures: List<String> = emptyList(),
)

/** One numbered spot on the room map, at [angle] degrees from "ahead" (clockwise). */
data class MapPoint(val number: Int, val module: ModuleId, val reason: String, val angle: Float)

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
    val mode: ScanMode = ScanMode.FULL,
    /** Share of the room the camera pointed at, 0–1; null when not measured. */
    val coverage: Float? = null,
    val mapPoints: List<MapPoint> = emptyList(),
    /** Compared with the last scan of the same place, if there is one. */
    val previousScanAt: Long? = null,
    val sinceLast: ScanDiff.Result? = null,
)

class ResultsViewModel(private val container: AppContainer, private val scanId: String) : ViewModel() {

    val state: StateFlow<ResultsUi?> = container.repository.observeScan(scanId)
        .map { details -> details?.let { it.toUi(container.repository.previousScan(it.scan)) } }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), null)

    /**
     * Marks every finding in a grouped row. "Checked, it's fine" also remembers the
     * devices behind it so later scans don't flag them; "Looks like a camera" forgets them.
     */
    fun setVerdict(row: FindingUi, verdict: UserVerdict) {
        viewModelScope.launch {
            row.ids.forEach { container.repository.setVerdict(it, verdict) }
            when (verdict) {
                UserVerdict.CHECKED_OK -> container.settings.rememberSafe(row.signatures)
                UserVerdict.CONFIRMED -> container.settings.forgetSafe(row.signatures)
                UserVerdict.UNREVIEWED -> Unit
            }
        }
    }

    /** The PDF report for sharing; saves evidence first if that hasn't happened yet. */
    fun reportForSharing(onReady: (File) -> Unit, onError: () -> Unit) {
        val pdf = container.evidenceStore.pdfFile(scanId)
        if (pdf.exists()) return onReady(pdf)
        saveEvidence(onSaved = { if (pdf.exists()) onReady(pdf) else onError() }, onError = onError)
    }

    /** Writes report.json and hashes every file. Storage full -> onError, Results stays open. */
    fun saveEvidence(onSaved: () -> Unit, onError: () -> Unit) {
        viewModelScope.launch {
            try {
                val details = container.repository.getScan(scanId) ?: return@launch onError()
                container.evidenceStore.saveEvidence(details, container.appVersion)
                onSaved()
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                // Storage full shows up as IOException or SQLiteFullException.
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

    private fun ScanWithDetails.toUi(previous: ScanWithDetails?): ResultsUi {
        val runById = moduleRuns.associateBy { it.id }
        val deviceById = networkDevices.associateBy { it.id }
        val findingsUi = findings.mapNotNull { f ->
            val module = runById[f.moduleRunId]?.module ?: return@mapNotNull null
            val snap = container.evidenceStore.snapshotFile(scan.id, f.id).takeIf { it.exists() }
            val technical = f.networkDeviceId?.let(deviceById::get)?.let { d ->
                buildString {
                    d.hostname?.let { append(it).append("\n") }
                    append(d.ip)
                    if (d.openPorts.isNotEmpty()) append("  ports ").append(d.openPorts.joinToString(", "))
                    d.mdnsName?.let { append("\n").append(it) }
                    d.ssdpServer?.let { append("\n").append(it) }
                    d.banner?.let { append("\n").append(it) }
                }
            }
            val signature = when (module) {
                ModuleId.NETWORK -> f.networkDeviceId?.let(deviceById::get)?.let { d ->
                    if (SafeDevices.isHotspotAddress(d.ip)) SafeDevices.hotspot(d.ip)
                    else SafeDevices.lan(d.ip, d.mdnsName, d.ssdpServer, d.banner)
                }
                ModuleId.BLE -> f.deviceAddress?.let(SafeDevices::ble)
                else -> null
            }
            FindingUi(f.id, module, f.reason, f.confidence, f.userVerdict, snap, technical,
                yaw = f.yaw, pitch = f.pitch, signatures = listOfNotNull(signature))
        }.let(FindingGroups::group)
        val startYaw = scan.startYaw
        val mapPoints = if (startYaw == null) emptyList() else findingsUi.filter { it.yaw != null }
            .take(MAX_MAP_POINTS)
            .mapIndexed { i, f -> MapPoint(i + 1, f.module, f.reason, PointingMath.angleDiff(f.yaw!!, startYaw)) }

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
            mode = scan.mode ?: ScanMode.FULL,
            coverage = scan.coverage,
            mapPoints = mapPoints,
            previousScanAt = previous?.scan?.startedAt,
            sinceLast = previous?.let {
                ScanDiff.diff(it.networkDevices, it.bleDevices, networkDevices, bleDevices)
            },
        )
    }

    private companion object { const val MAX_MAP_POINTS = 9 }
}
