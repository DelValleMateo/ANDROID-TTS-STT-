package com.uader.ptah.data.tts

sealed interface TtsState {
    data object Initializing : TtsState
    data object Idle : TtsState
    data class Speaking(val messageId: Long) : TtsState
    data class Error(val message: String) : TtsState
}
