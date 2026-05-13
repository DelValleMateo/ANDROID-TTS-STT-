package com.uader.ptah.ui.chat

import com.uader.ptah.data.NormativeArticle

sealed interface ChatUiState {
    data object Idle : ChatUiState
    data object Loading : ChatUiState
    data class Success(
        val results: List<NormativeArticle>,
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
