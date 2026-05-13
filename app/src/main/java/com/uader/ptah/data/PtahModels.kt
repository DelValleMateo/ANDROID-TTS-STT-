package com.uader.ptah.data

import com.google.gson.annotations.SerializedName

// Request esperado por el Mock: {"consulta": "..."}.
data class SemanticSearchRequest(
    @SerializedName("consulta")
    val query: String
)

// Modelo de dominio usado por el ViewModel y la UI.
data class NormativeArticle(
    val id: String,
    val title: String,
    val content: String,
    val relevance: Double? = null
)

// DTOs de red según CONTRATO_API.md y mock_server/app_mock.py.
data class SemanticSearchResponse(
    @SerializedName("estado")
    val status: String?,
    @SerializedName("mensaje")
    val message: String?,
    @SerializedName("resultados")
    val results: List<SemanticArticleDto>?
)

data class SemanticArticleDto(
    @SerializedName("id_articulo")
    val articleId: String?,
    @SerializedName("titulo")
    val title: String?,
    @SerializedName("contenido")
    val content: String?,
    @SerializedName("score_relevancia")
    val relevanceScore: Double?
)
