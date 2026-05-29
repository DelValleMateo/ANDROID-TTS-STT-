package com.uader.ptah.data

import android.util.Log

interface PtahRepository {
    suspend fun ask(query: String): Result<QueryResponse>
}

class PtahRepositoryImpl(
    private val apiService: GroqApiService
) : PtahRepository {

    override suspend fun ask(query: String): Result<QueryResponse> {
        return runCatching {
            Log.d(TAG, "Enviando consulta textual a Groq: $query")

            val request = GroqChatCompletionRequest(
                model = GROQ_MODEL,
                messages = listOf(
                    GroqMessage(
                        role = "user",
                        content = query
                    )
                )
            )

            val response = apiService.createChatCompletion(request = request)

            if (!response.isSuccessful) {
                throw IllegalStateException("El servicio respondio con un error (${response.code()}). Intenta nuevamente.")
            }

            val body = response.body()
                ?: throw IllegalStateException("No se obtuvo una respuesta valida.")

            val answer = body.choices
                ?.firstOrNull()
                ?.message
                ?.content
                ?.trim()
                .orEmpty()

            if (answer.isBlank()) {
                throw IllegalStateException("No se obtuvo una respuesta valida.")
            }

            Log.d(TAG, "Respuesta textual recibida de Groq.")
            QueryResponse(answer = answer)
        }
    }

    private companion object {
        const val TAG = "PtahRepository"
        const val GROQ_MODEL = "llama-3.1-8b-instant"
    }
}
