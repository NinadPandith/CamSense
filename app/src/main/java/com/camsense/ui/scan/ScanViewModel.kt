package com.camsense.ui.scan

import android.util.Log
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.camsense.AppContainer
import com.camsense.core.Finding
import com.camsense.core.ModuleId
import com.camsense.core.ModuleResult
import com.camsense.core.Reasons
import com.camsense.core.ScanMode
import com.camsense.core.SkipReason
import com.camsense.core.Spot
import com.camsense.core.Spots
import com.camsense.core.coverage.CoverageGrid
import com.camsense.core.sensors.OrientationSource
import com.camsense.data.SafeDevices
import com.camsense.modules.magnetometer.HotCold
import com.camsense.modules.magnetometer.hotColdFlow
import com.camsense.modules.network.toNetworkDevice
import com.camsense.scoring.RiskAssessment
import com.camsense.scoring.RiskEngine
import com.camsense.scoring.ScanConfig
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

/**
 * Runs one guided scan. Shared by every step screen through the nested "scan"
 * navigation graph, so results from earlier steps survive moving between steps,
 * and it lives exactly as long as the scan (App Flow doc).
 */
class ScanViewModel(
    private val container: AppContainer,
    savedState: SavedStateHandle,
) : ViewModel() {

    val modules = container.newScanModules()
    val initialPlaceName: String = savedState.get<String>("place").orEmpty()
    val mode: ScanMode = savedState.get<String>("mode")
        ?.let { m -> ScanMode.entries.firstOrNull { it.name == m } } ?: ScanMode.FULL
    val steps: List<ScanStep> = ScanSteps.of(mode)
    val voice = container.voice

    private val _scanId = MutableStateFlow<String?>(null)
    val scanId: StateFlow<String?> = _scanId.asStateFlow()

    val liveRisk: StateFlow<RiskAssessment> = RiskEngine.combine(modules.all.map { it.results })
        .stateIn(viewModelScope, SharingStarted.Eagerly, RiskEngine.assess(emptyList()))

    private val _spots = MutableStateFlow<List<Spot>>(emptyList())
    val spots: StateFlow<List<Spot>> = _spots.asStateFlow()

    // ---- Room coverage (camera steps) ----
    data class Coverage(val fraction: Float = 0f, val ring: BooleanArray = BooleanArray(0), val gap: CoverageGrid.Gap = CoverageGrid.Gap.NONE)

    private val orientation = OrientationSource(container.app)
    private val grid = CoverageGrid(ScanConfig.COVERAGE_SECTORS, ScanConfig.COVERAGE_SAMPLES,
        ScanConfig.COVERAGE_LOW_PITCH, ScanConfig.COVERAGE_HIGH_PITCH)
    private val _coverage = MutableStateFlow(Coverage())
    val coverage: StateFlow<Coverage> = _coverage.asStateFlow()
    val coverageAvailable: Boolean get() = orientation.isAvailable
    @Volatile private var coverageActive = false
    private var lastCoveragePublish = 0L

    /** Close check: getting warmer / colder as the phone moves. */
    val hotCold: StateFlow<HotCold.State> = hotColdFlow(modules.magnet.live, viewModelScope)

    private var networkJob: Job? = null
    private var bleJob: Job? = null
    private var finished = false
    private var cameraSkipped = false

    init {
        orientation.onSample = { p ->
            if (coverageActive) {
                synchronized(grid) { grid.add(p.yaw, p.pitch) }
                if (p.timeMs - lastCoveragePublish > 200) {
                    lastCoveragePublish = p.timeMs
                    synchronized(grid) { _coverage.value = Coverage(grid.fraction(), grid.middleRing(), grid.biggestGap()) }
                }
            }
        }
    }

    /** Step 1: creates the scan row (status RUNNING). */
    fun startScan(placeName: String, onReady: () -> Unit) {
        if (_scanId.value != null) return onReady()
        viewModelScope.launch {
            val scan = container.repository.createScan(placeName, mode)
            _scanId.value = scan.id
            modules.glint.scanId = scan.id
            orientation.start()
            // Checks that aren't part of this scan type.
            if (mode == ScanMode.QUICK) {
                listOf(modules.objects, modules.infrared, modules.magnet).forEach { it.skip(SkipReason.NOT_IN_THIS_SCAN) }
            }
            onReady()
        }
    }

    /** The step after [from], running what has to happen in between. */
    suspend fun next(from: ScanStep): ScanStep {
        var i = steps.indexOf(from) + 1
        while (i < steps.size) {
            val step = steps[i]
            val runs = when (step) {
                ScanStep.ROOM, ScanStep.LENS -> !cameraSkipped
                ScanStep.IR -> !cameraSkipped && prepareIrStep()
                else -> true
            }
            if (runs) return step
            i++
        }
        return ScanStep.RESULTS
    }

    /** Step 2: Wi-Fi and Bluetooth start, and keep running through the next steps. */
    fun startNetworkChecks() {
        // Also re-runs a check that was skipped for a missing permission, once the user allows it.
        val networkNeedsRetry = modules.network.results.value.skipReason == SkipReason.NO_NEARBY_PERMISSION ||
            (networkJob?.isCompleted == true && modules.network.lanSkip.value == SkipReason.NO_NEARBY_PERMISSION)
        if (networkJob == null || networkNeedsRetry) {
            networkJob = viewModelScope.launch { modules.network.start() }
        }
        if (bleJob == null || modules.ble.results.value.skipReason == SkipReason.NO_NEARBY_PERMISSION) {
            bleJob = viewModelScope.launch { modules.ble.start() }
        }
    }

    /** Without camera access, the camera steps are skipped. */
    fun skipCameraSteps() {
        cameraSkipped = true
        listOf(modules.objects, modules.infrared, modules.glint)
            .filter { it.results.value.status == ModuleResult.Status.IDLE || it.results.value.status == ModuleResult.Status.RUNNING }
            .forEach { it.skip(SkipReason.NO_CAMERA_PERMISSION) }
    }

    /** Coverage counts while a camera step is on screen; the IR step uses the front camera. */
    fun startCoverage(frontCamera: Boolean) {
        orientation.frontCamera = frontCamera
        coverageActive = true
    }

    fun stopCoverage() { coverageActive = false }

    /** Step 3. Returns false when the model can't load (the step is then skipped). */
    suspend fun startRoomSweep(): Boolean {
        modules.objects.start()
        return modules.objects.results.value.status != ModuleResult.Status.FAILED
    }

    fun endRoomSweep() = modules.objects.clearOverlay()

    /** Bathroom / changing room: result of the mirror fingertip test. */
    enum class MirrorAnswer { NORMAL, NO_GAP, NO_MIRROR }

    fun recordMirror(answer: MirrorAnswer) {
        modules.objects.addManualFinding(
            if (answer == MirrorAnswer.NO_GAP) Finding(
                reason = Reasons.MIRROR_NO_GAP,
                confidence = ScanConfig.MIRROR_NO_GAP_CONFIDENCE,
                targetKey = "mirror",
            ) else null
        )
    }

    /** Step 4a: false means the IR step is skipped and never shown. */
    private suspend fun prepareIrStep(): Boolean {
        if (modules.infrared.results.value.status == ModuleResult.Status.SKIPPED) return false
        if (!modules.infrared.refreshSupport()) {
            modules.infrared.skip(SkipReason.IR_NOT_SUPPORTED)
            return false
        }
        modules.infrared.start()
        return true
    }

    fun endIrStep() {
        modules.infrared.stop()
        modules.infrared.clearOverlay()
    }

    /** Step 4b: torch on, glint tracking, objects every few frames. */
    fun startLensStep() = viewModelScope.launch { modules.glint.start() }

    fun endLensStep() {
        modules.glint.stop()
        modules.glint.clearOverlay()
        modules.objects.clearOverlay()
    }

    /** Step 5: builds the flagged-spot list and starts the magnetometer. */
    fun startCloseCheck(): Boolean {
        _spots.value = Spots.from(
            listOf(modules.objects, modules.glint, modules.infrared).flatMap { m ->
                m.results.value.findings.map { m.id to it }
            }
        )
        if (!modules.magnet.isSupported()) {
            modules.magnet.skip(SkipReason.NO_MAGNETOMETER)
            return false
        }
        viewModelScope.launch { modules.magnet.start() }
        if (_spots.value.isEmpty()) modules.magnet.selectTarget(null) // free sweep
        return true
    }

    fun selectSpot(key: String?) = modules.magnet.selectTarget(key)

    /** Step 6: stops everything, scores and saves. Calls back with the scan id for Results. */
    fun finish(onSaved: (String) -> Unit) {
        val id = _scanId.value ?: return
        if (finished) return onSaved(id)
        finished = true
        stopAll()
        viewModelScope.launch {
            val stamped = modules.all.map { it.results.value }.map(::stampDirections)
            // Devices the user marked safe in earlier scans aren't flagged again.
            val safe = container.settings.safeDevices.first()
            val (results, dropped) = SafeDevices.filter(stamped, safe, ::signatureOf)
            if (dropped > 0) Log.i(TAG, "Ignored $dropped finding(s) for devices marked safe before")
            val assessment = RiskEngine.assess(results)
            Log.i(TAG, "Scan $id ($mode): score ${assessment.score} ${assessment.level} (sum ${assessment.weightedSum}, agreement ${assessment.agreedTargets}, floor ${assessment.floorApplied})")
            val (coverageFraction, startYaw) = synchronized(grid) {
                (if (grid.startYaw != null) grid.fraction() else null) to grid.startYaw
            }
            container.repository.completeScan(
                scanId = id,
                results = results,
                assessment = assessment,
                networkDevices = modules.network.devices.value + modules.network.hotspots.value
                    .filter { it.suspicionScore > 0f }
                    .map { it.toNetworkDevice() },
                bleDevices = modules.ble.devices.value,
                coverage = coverageFraction,
                startYaw = startYaw,
            )
            onSaved(id)
        }
    }

    /**
     * Gives camera findings the direction they were seen in (for the room map): where
     * the phone pointed when the finding was made, nudged by where it sat in the frame.
     */
    private fun stampDirections(r: ModuleResult): ModuleResult {
        if (r.moduleId !in setOf(ModuleId.GLINT, ModuleId.OBJECTS, ModuleId.INFRARED)) return r
        return r.copy(findings = r.findings.map { f ->
            val p = orientation.at(f.createdAt) ?: return@map f
            val box = f.screenBox
            // The front camera (IR) isn't nudged: its picture is mirrored relative to the room.
            val nudge = box != null && r.moduleId != ModuleId.INFRARED
            val yaw = p.yaw + if (nudge) (box!!.centerX - 0.5f) * ScanConfig.CAMERA_HFOV_DEG else 0f
            val pitch = p.pitch - if (nudge) (box!!.centerY - 0.5f) * ScanConfig.CAMERA_VFOV_DEG else 0f
            f.copy(yaw = (yaw + 360f) % 360f, pitch = pitch.coerceIn(-90f, 90f))
        })
    }

    /** Signature of the device behind a Wi-Fi or Bluetooth finding (see SafeDevices). */
    private fun signatureOf(module: ModuleId, f: Finding): String? {
        val address = f.deviceAddress ?: return null
        return when (module) {
            ModuleId.BLE -> SafeDevices.ble(address)
            ModuleId.NETWORK -> if (SafeDevices.isHotspotAddress(address)) SafeDevices.hotspot(address)
                else modules.network.devices.value.firstOrNull { it.ip == address }
                    ?.let { SafeDevices.lan(it.ip, it.mdnsName, it.serverText, it.banner) }
            else -> null
        }
    }

    /** "Stop this scan?": saved as ABORTED and not shown in History. */
    fun abort() {
        if (finished) return
        finished = true
        stopAll()
        val id = _scanId.value ?: return
        container.appScope.launch { container.repository.abortScan(id) }
    }

    private fun stopAll() {
        coverageActive = false
        orientation.stop()
        voice.stop()
        networkJob?.cancel()
        bleJob?.cancel()
        modules.all.forEach {
            try { it.stop() } catch (e: Exception) { Log.w(TAG, "stop ${it.id}", e) }
        }
    }

    override fun onCleared() {
        if (!finished) abort()
        super.onCleared()
    }

    private companion object { const val TAG = "CamSense/Scan" }
}
