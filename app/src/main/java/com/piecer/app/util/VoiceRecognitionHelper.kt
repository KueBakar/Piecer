package com.piecer.app.util

import android.content.Context
import android.content.Intent
import android.speech.RecognizerIntent

object VoiceRecognitionHelper {

    const val MAX_RESULTS = 3
    const val LANGUAGE_ID = "id-ID"

    fun createRecognizerIntent(prompt: String): Intent {
        return Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH).apply {
            putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM)
            putExtra(RecognizerIntent.EXTRA_LANGUAGE, LANGUAGE_ID)
            putExtra(RecognizerIntent.EXTRA_PREFER_OFFLINE, true)
            putExtra(RecognizerIntent.EXTRA_MAX_RESULTS, MAX_RESULTS)
            if (prompt.isNotBlank()) {
                putExtra(RecognizerIntent.EXTRA_PROMPT, prompt)
            }
        }
    }

    fun isSpeechRecognitionAvailable(context: Context, prompt: String = ""): Boolean {
        val intent = createRecognizerIntent(prompt)
        return intent.resolveActivity(context.packageManager) != null
    }

    fun extractCandidates(data: Intent?): List<String> {
        return data?.getStringArrayListExtra(RecognizerIntent.EXTRA_RESULTS)
            ?.take(MAX_RESULTS)
            .orEmpty()
    }
}
