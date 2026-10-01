package com.camsense.modules.magnetometer

import android.content.Context
import android.media.AudioManager
import android.media.ToneGenerator
import android.os.VibrationEffect
import android.os.Vibrator
import android.os.VibratorManager
import android.os.Build

/** Short beep plus a short buzz, so the sweep works without looking or hearing. */
class BeepPlayer(context: Context) {

    private val tone = try { ToneGenerator(AudioManager.STREAM_MUSIC, 80) } catch (_: RuntimeException) { null }
    private val vibrator: Vibrator? =
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S)
            context.getSystemService(VibratorManager::class.java)?.defaultVibrator
        else @Suppress("DEPRECATION") context.getSystemService(Vibrator::class.java)

    fun beep() {
        tone?.startTone(ToneGenerator.TONE_PROP_BEEP, 40)
        vibrator?.vibrate(VibrationEffect.createOneShot(30, VibrationEffect.DEFAULT_AMPLITUDE))
    }

    fun release() {
        tone?.release()
    }
}
