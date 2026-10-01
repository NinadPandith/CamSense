package com.camsense.modules.magnetometer

import android.content.Context
import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorEventListener
import android.hardware.SensorManager
import com.camsense.core.BaseModule
import com.camsense.core.Finding
import com.camsense.core.ModuleId
import com.camsense.core.Reasons
import com.camsense.core.SkipReason
import com.camsense.scoring.ScanConfig
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch

/**
 * M5: magnetometer sweep (PRD FR-5.1–5.3). Records a 3-second baseline, then beeps
 * and vibrates faster as the field rises. Readings are tied to the flagged spot the
 * user has selected, so a spike near the tissue box shares its target_key.
 * Confirmation only: noisy and works within a few centimetres.
 */
class MagnetSweepModule(private val context: Context) : BaseModule(ModuleId.MAGNET) {

    data class Live(
        val calibrating: Boolean = true,
        val delta: Float = 0f,
        val level: Float = 0f,
        val target: String? = null,
    )

    private val sensorManager = context.getSystemService(SensorManager::class.java)
    private val sensor: Sensor? = sensorManager?.getDefaultSensor(Sensor.TYPE_MAGNETIC_FIELD)

    private val _live = MutableStateFlow(Live())
    val live: StateFlow<Live> = _live.asStateFlow()

    private var scope: CoroutineScope? = null
    private var beeper: BeepPlayer? = null
    private var listener: SensorEventListener? = null

    private val baselineSamples = mutableListOf<Float>()
    private var baselineEndsAt = 0L
    private var baseline = 0f
    private var target: String? = null
    private var freeSweep = false
    /** Biggest rise seen per spot; key "" is the free sweep. */
    private val peaks = linkedMapOf<String, Float>()

    override fun isSupported(): Boolean = sensor != null

    override suspend fun start() {
        val s = sensor ?: return skip(SkipReason.NO_MAGNETOMETER)
        if (scope != null) return
        markRunning()
        val sc = CoroutineScope(SupervisorJob() + Dispatchers.Default).also { scope = it }
        beeper = BeepPlayer(context)
        recalibrate()
        val l = object : SensorEventListener {
            override fun onSensorChanged(event: SensorEvent) = onReading(event.values[0], event.values[1], event.values[2])
            override fun onAccuracyChanged(sensor: Sensor?, accuracy: Int) {}
        }
        listener = l
        sensorManager!!.registerListener(l, s, SensorManager.SENSOR_DELAY_GAME)
        sc.launch { beepLoop() }
    }

    /** Points readings at one flagged spot; null means a free sweep. */
    fun selectTarget(key: String?) {
        target = key
        freeSweep = key == null
        _live.value = _live.value.copy(target = key)
    }

    /** Hold the phone away from objects, then call this to take a fresh baseline. */
    fun recalibrate() {
        synchronized(baselineSamples) { baselineSamples.clear() }
        baselineEndsAt = System.currentTimeMillis() + ScanConfig.MAG_BASELINE_MS
        _live.value = _live.value.copy(calibrating = true, delta = 0f, level = 0f)
    }

    private fun onReading(x: Float, y: Float, z: Float) {
        val m = MagnetMath.magnitude(x, y, z)
        if (System.currentTimeMillis() < baselineEndsAt) {
            synchronized(baselineSamples) { baselineSamples += m }
            return
        }
        if (_live.value.calibrating) {
            baseline = synchronized(baselineSamples) { if (baselineSamples.isEmpty()) m else baselineSamples.average().toFloat() }
            _live.value = _live.value.copy(calibrating = false)
        }
        val d = MagnetMath.delta(m, baseline)
        _live.value = _live.value.copy(delta = d, level = MagnetMath.level(d))
        if (target != null || freeSweep) {
            val key = target ?: ""
            if (d > (peaks[key] ?: 0f)) {
                peaks[key] = d
                publishFindings(done = false)
            }
        }
    }

    private suspend fun CoroutineScope.beepLoop() {
        while (isActive) {
            val live = _live.value
            val interval = if (live.calibrating) null else MagnetMath.beepIntervalMs(live.delta)
            if (interval == null) delay(100) else { beeper?.beep(); delay(interval) }
        }
    }

    private fun publishFindings(done: Boolean) {
        val findings = peaks.filterValues { it >= ScanConfig.MAG_FINDING_UT }.map { (key, peak) ->
            Finding(reason = Reasons.MAGNET, confidence = MagnetMath.score(peak), targetKey = key.ifEmpty { null })
        }
        publish(findings.maxOfOrNull { it.confidence } ?: 0f, findings, done)
    }

    override fun stop() {
        listener?.let { sensorManager?.unregisterListener(it) }
        listener = null
        scope?.cancel()
        scope = null
        beeper?.release()
        beeper = null
        if (results.value.status == com.camsense.core.ModuleResult.Status.RUNNING) publishFindings(done = true)
    }
}
