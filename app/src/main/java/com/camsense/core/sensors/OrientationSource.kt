package com.camsense.core.sensors

import android.content.Context
import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorEventListener
import android.hardware.SensorManager
import android.util.Log
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

/**
 * Where the phone's camera points, from the rotation-vector sensor (fused
 * accelerometer, gyroscope and compass). Used by the coverage meter, the room map
 * and the hot–cold finder. Keeps a short history so findings can be stamped with
 * the direction they were seen in.
 */
class OrientationSource(context: Context) {

    private val sensorManager = context.getSystemService(SensorManager::class.java)
    private val sensor: Sensor? = sensorManager?.getDefaultSensor(Sensor.TYPE_ROTATION_VECTOR)
        ?: sensorManager?.getDefaultSensor(Sensor.TYPE_GAME_ROTATION_VECTOR)

    private val _pointing = MutableStateFlow<Pointing?>(null)
    val pointing: StateFlow<Pointing?> = _pointing.asStateFlow()

    /** Front camera (IR step): the camera looks out of the screen side. */
    @Volatile var frontCamera = false

    private val history = ArrayDeque<Pointing>()
    private val matrix = FloatArray(9)
    private var listener: SensorEventListener? = null
    var onSample: ((Pointing) -> Unit)? = null

    val isAvailable: Boolean get() = sensor != null

    fun start() {
        val s = sensor
        if (s == null) {
            Log.i(TAG, "No rotation-vector sensor: coverage and room map are off")
            return
        }
        if (listener != null) return
        val l = object : SensorEventListener {
            override fun onSensorChanged(event: SensorEvent) {
                SensorManager.getRotationMatrixFromVector(matrix, event.values)
                val p = PointingMath.fromRotationMatrix(matrix, frontCamera).copy(timeMs = System.currentTimeMillis())
                _pointing.value = p
                synchronized(history) {
                    history.addLast(p)
                    while (history.size > MAX_HISTORY) history.removeFirst()
                }
                onSample?.invoke(p)
            }
            override fun onAccuracyChanged(sensor: Sensor?, accuracy: Int) {}
        }
        listener = l
        // ~50 Hz is plenty; SENSOR_DELAY_GAME
        sensorManager!!.registerListener(l, s, SensorManager.SENSOR_DELAY_GAME)
    }

    fun stop() {
        listener?.let { sensorManager?.unregisterListener(it) }
        listener = null
    }

    /** The pointing recorded closest to [timeMs], if any within the history. */
    fun at(timeMs: Long): Pointing? = synchronized(history) {
        history.minByOrNull { kotlin.math.abs(it.timeMs - timeMs) }
            ?.takeIf { kotlin.math.abs(it.timeMs - timeMs) < 2_000 }
    }

    private companion object {
        const val TAG = "CamSense/Orientation"
        /** ~10 minutes at 50 Hz would be 30k; keep 20k samples (~7 min). */
        const val MAX_HISTORY = 20_000
    }
}
