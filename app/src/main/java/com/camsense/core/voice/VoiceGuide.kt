package com.camsense.core.voice

import android.content.Context
import android.speech.tts.TextToSpeech
import android.util.Log
import com.camsense.data.SettingsStore
import com.camsense.scoring.ScanConfig
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import java.util.Locale

/**
 * Spoken, hands-free guidance with the phone's own offline text-to-speech, so the
 * dark sweep works without reading the screen. Off until turned on in Settings.
 * English (Indian English when the phone has it).
 */
class VoiceGuide(private val context: Context, settings: SettingsStore, scope: CoroutineScope) {

    @Volatile private var enabled = false
    private var tts: TextToSpeech? = null
    @Volatile private var ready = false
    private var pending: String? = null
    private val lastSpoken = mutableMapOf<String, Long>()

    init {
        scope.launch(Dispatchers.Main) {
            settings.voiceGuidance.collect { on ->
                enabled = on
                if (on) ensureEngine() else stop()
            }
        }
    }

    private fun ensureEngine() {
        if (tts != null) return
        tts = TextToSpeech(context.applicationContext) { status ->
            if (status != TextToSpeech.SUCCESS) {
                Log.w(TAG, "Text-to-speech not available ($status)")
                return@TextToSpeech
            }
            val engine = tts ?: return@TextToSpeech
            val india = Locale("en", "IN")
            engine.language = if (engine.isLanguageAvailable(india) >= TextToSpeech.LANG_AVAILABLE) india else Locale.ENGLISH
            engine.setSpeechRate(0.95f)
            ready = true
            pending?.let { speakNow(it, interrupt = true) }
            pending = null
        }
    }

    /**
     * Speaks [text] if voice guidance is on. The same sentence isn't repeated within a
     * few seconds. [interrupt] cuts off what is being said (new step); otherwise it queues.
     */
    fun say(text: String, interrupt: Boolean = false) {
        if (!enabled || text.isBlank()) return
        val now = System.currentTimeMillis()
        synchronized(lastSpoken) {
            if (now - (lastSpoken[text] ?: 0L) < ScanConfig.VOICE_REPEAT_MS) return
            lastSpoken[text] = now
        }
        if (!ready) { pending = text; ensureEngine(); return }
        speakNow(text, interrupt)
    }

    private fun speakNow(text: String, interrupt: Boolean) {
        tts?.speak(text, if (interrupt) TextToSpeech.QUEUE_FLUSH else TextToSpeech.QUEUE_ADD, null, text.hashCode().toString())
    }

    fun stop() {
        tts?.stop()
    }

    private companion object { const val TAG = "CamSense/Voice" }
}
