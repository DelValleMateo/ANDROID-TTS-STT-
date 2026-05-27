package com.uader.ptah.data

import android.util.Log
import com.uader.ptah.BuildConfig

interface PtahRepository {
    suspend fun ask(query: String): Result<QueryResponse>
}

class PtahRepositoryImpl(
    private val apiService: GoogleAiApiService
) : PtahRepository {

    override suspend fun ask(query: String): Result<QueryResponse> {
        return runCatching {
            Log.d(TAG, "Enviando consulta textual a Google IA: $query")

            val request = GoogleAiRequest(
                systemInstruction = GoogleAiContent(
                    parts = listOf(GoogleAiPart(PTAH_SYSTEM_PROMPT))
                ),
                contents = listOf(
                    GoogleAiContent(
                        role = "user",
                        parts = listOf(GoogleAiPart(query))
                    )
                )
            )

            val response = apiService.generateContent(
                model = BuildConfig.GOOGLE_AI_MODEL,
                request = request
            )

            if (!response.isSuccessful) {
                throw IllegalStateException("El servicio respondio con un error (${response.code()}). Intenta nuevamente.")
            }

            val body = response.body()
                ?: throw IllegalStateException("No se obtuvo una respuesta valida.")

            val answer = body.candidates
                ?.firstOrNull()
                ?.content
                ?.parts
                ?.joinToString(separator = "\n") { it.text }
                ?.trim()
                .orEmpty()

            if (answer.isBlank()) {
                throw IllegalStateException("No se obtuvo una respuesta valida.")
            }

            Log.d(TAG, "Respuesta textual recibida de Google IA.")
            QueryResponse(answer = answer)
        }
    }

    private companion object {
        const val TAG = "PtahRepository"

        const val PTAH_SYSTEM_PROMPT =
            "Sos un asistente del Proyecto PTAH, un sistema de busqueda semantica de reglamentacion institucional.\n" +
                "Tu tarea es responder consultas en lenguaje natural de forma clara, breve y util.\n" +
                "Responde como asistente academico/tecnico orientado a reglamentaciones.\n" +
                "Si la consulta no tiene suficiente contexto o no se puede responder con precision, indica que no se encontro informacion suficiente.\n" +
                "No inventes articulos, normas ni datos especificos si no fueron proporcionados por el sistema.\n" +
                "Prioriza claridad, precision y utilidad para el usuario."
    }
}
