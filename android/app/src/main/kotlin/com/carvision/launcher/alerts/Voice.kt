package com.carvision.launcher.alerts

import android.content.Context
import android.speech.tts.TextToSpeech
import java.util.Locale

/**
 * Lectura por voz de avisos con el motor de síntesis instalado en la radio (español de España si está disponible).
 * Si la radio no tiene motor o voz en español, no dice nada (nunca bloquea ni falla).
 */
class Voice(context: Context) {
    private var ready = false
    private val tts: TextToSpeech = TextToSpeech(context.applicationContext) { status ->
        if (status == TextToSpeech.SUCCESS) {
            val r = tts.setLanguage(Locale("es", "ES"))
            ready = r != TextToSpeech.LANG_MISSING_DATA && r != TextToSpeech.LANG_NOT_SUPPORTED
        }
    }

    val isAvailable: Boolean get() = ready

    fun say(text: String) {
        if (ready) tts.speak(text, TextToSpeech.QUEUE_FLUSH, null, "carvision-alert")
    }
}
