package com.uader.ptah.ui.chat

sealed interface ChatUiState {
    data object Idle : ChatUiState
    data object Loading : ChatUiState
    data class Success(
        val latencyMs: Long
    ) : ChatUiState

    data class Error(
        val message: String,
        val latencyMs: Long? = null
    ) : ChatUiState
}

data class ChatMessage(
    val author: Author,
    val text: String
) {
    enum class Author { USER, SYSTEM }
}
