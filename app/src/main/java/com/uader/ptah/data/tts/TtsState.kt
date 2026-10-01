package com.uader.ptah.data.tts

/**
 * Estados del ciclo de síntesis de voz natural.
 */
sealed interface TtsState {
    data object Initializing : TtsState
    data object Idle : TtsState
    data class Generating(val messageId: Long? = null) : TtsState
    data class Speaking(val messageId: Long) : TtsState
    data class Error(val message: String) : TtsState
}
