package com.uader.ptah.ui.chat

sealed interface ConversationState {
    data object Idle : ConversationState
    data object Listening : ConversationState
    data object ProcessingVoice : ConversationState
    data class Consulting(val query: String) : ConversationState
    data class Speaking(val messageId: Long) : ConversationState
    data class Error(val message: String, val canRetry: Boolean) : ConversationState
}

enum class InputOrigin {
    KEYBOARD,
    VOICE
}

data class ChatMessage(
    val id: Long,
    val author: Author,
    val text: String,
    val origin: InputOrigin = InputOrigin.KEYBOARD
) {
    enum class Author { USER, SYSTEM }
}
