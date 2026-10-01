package com.camsense.ui.scan

import android.util.Log
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.camsense.AppContainer
import com.camsense.core.ModuleId
import com.camsense.core.SkipReason
import com.camsense.core.Spot
import com.camsense.core.Spots
import com.camsense.scoring.RiskAssessment
import com.camsense.scoring.RiskEngine
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
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

    private val _scanId = MutableStateFlow<String?>(null)
    val scanId: StateFlow<String?> = _scanId.asStateFlow()

    val liveRisk: StateFlow<RiskAssessment> = RiskEngine.combine(modules.all.map { it.results })
        .stateIn(viewModelScope, SharingStarted.Eagerly, RiskEngine.assess(emptyList()))

    private val _spots = MutableStateFlow<List<Spot>>(emptyList())
    val spots: StateFlow<List<Spot>> = _spots.asStateFlow()

    private var networkJob: Job? = null
    private var bleJob: Job? = null
    private var finished = false

    /** Step 1: creates the scan row (status RUNNING). */
    fun startScan(placeName: String, onReady: () -> Unit) {
        if (_scanId.value != null) return onReady()
        viewModelScope.launch {
            val scan = container.repository.createScan(placeName)
            _scanId.value = scan.id
            modules.glint.scanId = scan.id
            onReady()
        }
    }

    /** Step 2: Wi-Fi and Bluetooth start, and keep running through step 3. */
    fun startNetworkChecks() {
        // Also re-runs a check that was skipped for a missing permission, once the user allows it.
        if (networkJob == null || modules.network.results.value.skipReason == SkipReason.NO_NEARBY_PERMISSION) {
            networkJob = viewModelScope.launch { modules.network.start() }
        }
        if (bleJob == null || modules.ble.results.value.skipReason == SkipReason.NO_NEARBY_PERMISSION) {
            bleJob = viewModelScope.launch { modules.ble.start() }
        }
    }

    /** Without camera access, steps 3, 4a and 4b are skipped. */
    fun skipCameraSteps() {
        listOf(modules.objects, modules.infrared, modules.glint).forEach { it.skip(SkipReason.NO_CAMERA_PERMISSION) }
    }

    /** Step 3. Returns false when the model can't load (the step is then skipped). */
    suspend fun startRoomSweep(): Boolean {
        modules.objects.start()
        return modules.objects.results.value.status != com.camsense.core.ModuleResult.Status.FAILED
    }

    fun endRoomSweep() = modules.objects.clearOverlay()

    /** Step 4a: false means the IR step is skipped and never shown. */
    suspend fun prepareIrStep(): Boolean {
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
            val results = modules.all.map { it.results.value }
            val assessment = RiskEngine.assess(results)
            Log.i(TAG, "Scan $id: score ${assessment.score} ${assessment.level} (sum ${assessment.weightedSum}, agreement ${assessment.agreedTargets}, floor ${assessment.floorApplied})")
            container.repository.completeScan(
                scanId = id,
                results = results,
                assessment = assessment,
                networkDevices = modules.network.devices.value,
                bleDevices = modules.ble.devices.value,
            )
            onSaved(id)
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

    fun moduleResult(id: ModuleId) = modules.all.first { it.id == id }.results

    private companion object { const val TAG = "CamSense/Scan" }
}
