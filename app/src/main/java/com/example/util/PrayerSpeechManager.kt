package com.example.util

import android.content.Context
import android.speech.tts.TextToSpeech
import android.speech.tts.UtteranceProgressListener
import android.util.Log
import com.example.data.AppLanguage
import java.util.Locale

class PrayerSpeechManager(context: Context, private val onInitCallback: (Boolean) -> Unit) : TextToSpeech.OnInitListener {

    private var tts: TextToSpeech? = TextToSpeech(context, this)
    private var isInitialized = false
    private var onSpeakComplete: (() -> Unit)? = null

    override fun onInit(status: Int) {
        if (status == TextToSpeech.SUCCESS) {
            isInitialized = true
            // Configure progress listener
            tts?.setOnUtteranceProgressListener(object : UtteranceProgressListener() {
                override fun onStart(utteranceId: String?) {
                    Log.d("SpeechManager", "Speech started: $utteranceId")
                }
                override fun onDone(utteranceId: String?) {
                    onSpeakComplete?.invoke()
                }
                override fun onError(utteranceId: String?) {
                    Log.e("SpeechManager", "Speech error!")
                }
            })
            onInitCallback(true)
        } else {
            Log.e("SpeechManager", "TTS Initialization failed.")
            onInitCallback(false)
        }
    }

    fun setOnDoneListener(listener: () -> Unit) {
        onSpeakComplete = listener
    }

    fun speak(text: String, language: AppLanguage, speed: Float, activeSentenceCallback: (String?, Int) -> Unit) {
        if (!isInitialized || tts == null) return

        tts?.apply {
            stop()
            // Set language
            val locale = when (language) {
                AppLanguage.ENGLISH -> Locale.US
                AppLanguage.LATIN -> Locale.ITALY // Italy is the standard region containing Latin pronunciation support
                AppLanguage.TAGALOG -> Locale("fil", "PH") // Filipino/Tagalog
            }
            setLanguage(locale)
            setSpeechRate(speed)

            // Split into sentences
            val sentences = text.split(Regex("(?<=[.!?])\\s+"))
                .map { it.trim() }
                .filter { it.isNotEmpty() }

            if (sentences.isEmpty()) return

            setOnUtteranceProgressListener(object : UtteranceProgressListener() {
                override fun onStart(utteranceId: String?) {
                    if (utteranceId != null && utteranceId.startsWith("sentence_")) {
                        val index = utteranceId.substringAfter("sentence_").toIntOrNull() ?: -1
                        if (index in sentences.indices) {
                            activeSentenceCallback(sentences[index], index)
                        }
                    }
                }

                override fun onDone(utteranceId: String?) {
                    if (utteranceId != null && utteranceId.startsWith("sentence_")) {
                        val index = utteranceId.substringAfter("sentence_").toIntOrNull() ?: -1
                        if (index == sentences.size - 1) {
                            activeSentenceCallback(null, -1)
                            onSpeakComplete?.invoke()
                        }
                    }
                }

                override fun onError(utteranceId: String?) {
                    Log.e("SpeechManager", "Error speaking sentence $utteranceId")
                }
            })

            sentences.forEachIndexed { index, sentence ->
                val queueMode = if (index == 0) TextToSpeech.QUEUE_FLUSH else TextToSpeech.QUEUE_ADD
                speak(sentence, queueMode, null, "sentence_$index")
            }
        }
    }

    fun stop() {
        if (isInitialized) {
            tts?.stop()
        }
    }

    fun shutdown() {
        tts?.shutdown()
        tts = null
    }
}
