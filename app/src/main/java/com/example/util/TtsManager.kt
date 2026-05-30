package com.example.util

import android.content.Context
import android.speech.tts.TextToSpeech
import android.util.Log
import java.util.Locale

class TtsManager(context: Context) : TextToSpeech.OnInitListener {

    private var tts: TextToSpeech? = null
    private var isInitialized = false

    init {
        tts = TextToSpeech(context.applicationContext, this)
    }

    override fun onInit(status: Int) {
        if (status == TextToSpeech.SUCCESS) {
            isInitialized = true
            Log.d("TtsManager", "TTS Initialized successfully")
        } else {
            Log.e("TtsManager", "TTS Initialization failed")
        }
    }

    fun speak(text: String, languageCode: String) {
        if (!isInitialized) {
            Log.w("TtsManager", "TTS is not ready yet")
            return
        }

        val locale = when (languageCode.lowercase()) {
            "ja" -> Locale.JAPANESE
            "zh" -> Locale.CHINESE
            "ko" -> Locale.KOREAN
            else -> Locale.ENGLISH
        }

        tts?.let {
            val result = it.setLanguage(locale)
            if (result == TextToSpeech.LANG_MISSING_DATA || result == TextToSpeech.LANG_NOT_SUPPORTED) {
                Log.e("TtsManager", "Language $languageCode is not supported on this device.")
                it.language = Locale.ENGLISH // Fallback
            }
            it.speak(text, TextToSpeech.QUEUE_FLUSH, null, "LanguageAppTts")
        }
    }

    fun shutdown() {
        try {
            tts?.stop()
            tts?.shutdown()
        } catch (e: Exception) {
            Log.e("TtsManager", "Error shutting down TTS", e)
        }
    }
}
