package com.uader.ptah.ui.chat

/**
 * Registra las métricas de latencia (en milisegundos) del flujo conversacional completo.
 */
data class ConversationMetrics(
    val sttLatencyMs: Long? = null,
    val apiLatencyMs: Long? = null,
    val ttsLatencyMs: Long? = null,
    val totalLatencyMs: Long? = null
)
