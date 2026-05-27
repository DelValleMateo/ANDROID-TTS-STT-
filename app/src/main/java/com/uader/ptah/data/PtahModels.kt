package com.uader.ptah.data

import com.google.gson.annotations.SerializedName

data class QueryRequest(
    val query: String
)

data class QueryResponse(
    val answer: String
)

data class GoogleAiRequest(
    @SerializedName("systemInstruction")
    val systemInstruction: GoogleAiContent,
    val contents: List<GoogleAiContent>,
    @SerializedName("generationConfig")
    val generationConfig: GoogleAiGenerationConfig = GoogleAiGenerationConfig()
)

data class GoogleAiGenerationConfig(
    val temperature: Double = 0.2,
    @SerializedName("maxOutputTokens")
    val maxOutputTokens: Int = 512
)

data class GoogleAiContent(
    val role: String? = null,
    val parts: List<GoogleAiPart>
)

data class GoogleAiPart(
    val text: String
)

data class GoogleAiResponse(
    val candidates: List<GoogleAiCandidate>?
)

data class GoogleAiCandidate(
    val content: GoogleAiContent?,
    @SerializedName("finishReason")
    val finishReason: String? = null
)
