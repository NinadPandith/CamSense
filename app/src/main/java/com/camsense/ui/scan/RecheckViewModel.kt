package com.camsense.ui.scan

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.camsense.AppContainer
import com.camsense.core.Spot
import com.camsense.core.Spots
import com.camsense.data.toModuleResults
import com.camsense.modules.magnetometer.MagnetSweepModule
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

/**
 * "Check flagged spots" from a Medium result: a magnetometer-only pass over the
 * saved scan's flagged spots. The scan is then re-scored and Results reopens.
 */
class RecheckViewModel(private val container: AppContainer, private val scanId: String) : ViewModel() {

    val magnet = MagnetSweepModule(container.app)

    private val _spots = MutableStateFlow<List<Spot>>(emptyList())
    val spots: StateFlow<List<Spot>> = _spots.asStateFlow()

    private var done = false

    init {
        viewModelScope.launch {
            val details = container.repository.getScan(scanId) ?: return@launch
            _spots.value = Spots.from(details.toModuleResults().flatMap { r -> r.findings.map { r.moduleId to it } })
            magnet.start()
            if (_spots.value.isEmpty()) magnet.selectTarget(null)
        }
    }

    fun selectSpot(key: String?) = magnet.selectTarget(key)

    fun finish(onSaved: () -> Unit) {
        if (done) return onSaved()
        done = true
        magnet.stop()
        viewModelScope.launch {
            container.repository.replaceMagnetResult(scanId, magnet.results.value)
            onSaved()
        }
    }

    override fun onCleared() {
        magnet.stop()
        super.onCleared()
    }
}
