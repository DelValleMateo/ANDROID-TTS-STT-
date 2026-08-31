package com.uader.ptah.data.tts

import kotlinx.coroutines.flow.StateFlow

/** Contrato independiente de Android que consume el ViewModel. */
interface SpeechOutput {
    val state: StateFlow<TtsState>
    fun speak(messageId: Long, text: String)
    fun stop()
    fun shutdown()
}
