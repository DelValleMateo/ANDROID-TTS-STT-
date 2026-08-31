package com.uader.ptah.ui.chat

// ─── Estados de la pantalla ──────────────────────────────────────────────────

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

// ─── Origen de la consulta ───────────────────────────────────────────────────

/**
 * Indica de qué fuente provino el texto de una consulta.
 *
 * - [KEYBOARD]: el usuario escribió la consulta manualmente.
 * - [VOICE]: el texto fue dictado y reconocido por el motor STT.
 *
 * Este valor se propaga desde [ChatViewModel] hasta cada [ChatMessage] para
 * que la UI pueda mostrar un indicador visual diferenciado en la burbuja.
 */
enum class InputOrigin {
    KEYBOARD,
    VOICE
}

// ─── Modelo de mensaje ───────────────────────────────────────────────────────

/**
 * Representa un único mensaje en el historial de la conversación.
 *
 * @param author    Quién emitió el mensaje: el usuario o el sistema (IA).
 * @param text      Contenido textual del mensaje.
 * @param origin    Origen del texto del usuario. Solo relevante cuando [author]
 *                  es [Author.USER]; para mensajes del sistema se usa [InputOrigin.KEYBOARD]
 *                  como valor neutral por defecto.
 */
data class ChatMessage(
    val id: Long,
    val author: Author,
    val text: String,
    val origin: InputOrigin = InputOrigin.KEYBOARD
) {
    enum class Author { USER, SYSTEM }
}
