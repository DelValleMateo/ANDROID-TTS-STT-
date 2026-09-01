package com.uader.ptah.data

import com.uader.ptah.data.network.HttpStatusException
import com.uader.ptah.data.network.InvalidNetworkResponseException
import retrofit2.Response
import retrofit2.http.Body
import retrofit2.http.POST

/** Contrato HTTP vigente del proveedor temporal. No representa al backend PTAH definitivo. */
interface GroqApiService {
    @POST("chat/completions")
    suspend fun createChatCompletion(
        @Body request: GroqChatCompletionRequest
    ): Response<GroqChatCompletionResponse>
}

/** Adapta el contrato específico de Groq a la frontera neutral de consultas. */
internal class GroqRemoteDataSource(
    private val apiService: GroqApiService,
    private val model: String
) : QueryRemoteDataSource {

    override suspend fun ask(query: String): String {
        val request = GroqChatCompletionRequest(
            model = model,
            messages = listOf(
                GroqMessage(role = "system", content = SYSTEM_PROMPT),
                GroqMessage(role = "user", content = query)
            )
        )

        val response = apiService.createChatCompletion(request)
        if (!response.isSuccessful) {
            response.errorBody()?.close()
            throw HttpStatusException(response.code())
        }

        val body = response.body()
            ?: throw InvalidNetworkResponseException("El servidor devolvió una respuesta sin contenido.")

        return body.choices
            ?.firstOrNull()
            ?.message
            ?.content
            .orEmpty()
    }

    private companion object {
        const val SYSTEM_PROMPT =
            "Eres PTAH, un asistente normativo por voz. Tus respuestas deben ser claras, " +
                "concisas y en tono conversacional. IMPORTANTE: NO uses NUNCA formato " +
                "Markdown (asteriscos, negritas, cursivas, listas con símbolos o tablas), " +
                "ya que tu respuesta será leída directamente por un sintetizador de voz. " +
                "Responde siempre en texto plano simple."
    }
}
