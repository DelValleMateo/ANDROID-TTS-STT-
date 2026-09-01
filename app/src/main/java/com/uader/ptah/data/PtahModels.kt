package com.uader.ptah.data

import com.google.gson.annotations.SerializedName

/** Modelo estable que consume la aplicación, independiente del proveedor. */
data class QueryResponse(
    val answer: String
)

enum class QueryError {
    INVALID_REQUEST,
    CONFIGURATION,
    TIMEOUT,
    CONNECTION,
    AUTHENTICATION,
    RATE_LIMIT,
    CLIENT_ERROR,
    SERVER_ERROR,
    INVALID_RESPONSE,
    UNKNOWN
}

/** Error estable de la capa de datos; no expone Retrofit ni DTOs a la UI. */
class QueryException(
    val error: QueryError,
    message: String,
    cause: Throwable? = null
) : Exception(message, cause)

// DTOs exclusivos del proveedor temporal Groq.
data class GroqChatCompletionRequest(
    @SerializedName("model")
    val model: String,

    @SerializedName("messages")
    val messages: List<GroqMessage>
)

data class GroqMessage(
    @SerializedName("role")
    val role: String,

    @SerializedName("content")
    val content: String
)

data class GroqChatCompletionResponse(
    @SerializedName("choices")
    val choices: List<GroqChoice>?
)

data class GroqChoice(
    @SerializedName("message")
    val message: GroqResponseMessage?
)

data class GroqResponseMessage(
    @SerializedName("content")
    val content: String?
)
