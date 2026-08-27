package com.uader.ptah.ui.chat

/**
 * Estado sellado que representa el ciclo de vida completo del reconocimiento de voz (STT).
 *
 * Flujo normal:
 *   Idle → Listening → Processing → Result
 *
 * Flujos alternativos:
 *   Idle → PermissionDenied  (usuario negó el micrófono)
 *   Listening → Error         (error de audio, red, etc.)
 *   Listening → Idle          (usuario canceló)
 */
sealed interface SttState {

    /** Sin actividad de reconocimiento. Estado inicial y de reposo. */
    data object Idle : SttState

    /** El reconocedor está capturando audio activamente. */
    data object Listening : SttState

    /** El audio fue capturado y está siendo procesado por el motor de reconocimiento. */
    data object Processing : SttState

    /** El reconocimiento finalizó con éxito. [text] contiene el texto reconocido. */
    data class Result(val text: String) : SttState

    /** Ocurrió un error durante el reconocimiento. [message] describe el problema en español. */
    data class Error(val message: String) : SttState

    /** El usuario negó el permiso de micrófono necesario para operar. */
    data object PermissionDenied : SttState
}
