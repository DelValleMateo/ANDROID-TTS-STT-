package com.uader.ptah.data

import android.util.Log
import retrofit2.HttpException
import java.io.IOException

interface PtahRepository {
    suspend fun ask(query: String): Result<QueryResponse>
}

class PtahRepositoryImpl(
    private val apiService: GroqApiService
) : PtahRepository {

    override suspend fun ask(query: String): Result<QueryResponse> {
        return try {
            Log.d(TAG, "Enviando consulta textual a Groq: $query")

            val request = GroqChatCompletionRequest(
                model = GROQ_MODEL,
                messages = listOf(
                    GroqMessage(
                        role = "system",
                        content = "Eres PTAH, un asistente normativo por voz. Tus respuestas deben ser claras, concisas y en tono conversacional. IMPORTANTE: NO uses NUNCA formato Markdown (asteriscos, negritas, cursivas, listas con símbolos o tablas), ya que tu respuesta será leída directamente por un sintetizador de voz. Responde siempre en texto plano simple."
                    ),
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
                
            val cleanAnswer = answer.replace("**", "")
                .replace("__", "")
                .replace("###", "")
                .replace("##", "")
                .replace("#", "")

            if (cleanAnswer.isBlank()) {
                throw IllegalStateException("No se obtuvo una respuesta valida.")
            }

            Log.d(TAG, "Respuesta textual recibida de Groq.")
            Result.success(QueryResponse(answer = cleanAnswer))
        } catch (e: HttpException) {
            Log.e(TAG, "Error HTTP en la consulta: ${e.code()}", e)
            Result.failure(e)
        } catch (e: IOException) {
            Log.e(TAG, "Error de red en la consulta: ${e.message}", e)
            Result.failure(e)
        } catch (e: Exception) {
            Log.e(TAG, "Error inesperado en la consulta: ${e.message}", e)
            Result.failure(e)
        }
    }

    private companion object {
        const val TAG = "PtahRepository"
        const val GROQ_MODEL = "openai/gpt-oss-120b"
    }
}
