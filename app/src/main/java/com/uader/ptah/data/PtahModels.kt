package com.uader.ptah.data

// Modelo para enviar la consulta
data class QueryRequest(
    val query: String
)

// Modelo para recibir cada artículo
data class NormativeArticle(
    val id: Int,
    val title: String,
    val content: String, // Este texto lo usará el TTS
    val relevance: Double? = null
)

// Respuesta completa del servidor
data class SearchResponse(
    val results: List<NormativeArticle>,
    val count: Int
)
