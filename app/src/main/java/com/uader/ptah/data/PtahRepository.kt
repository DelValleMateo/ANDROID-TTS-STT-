package com.uader.ptah.data

import android.util.Log

interface PtahRepository {
    suspend fun searchRegulations(query: String): Result<List<NormativeArticle>>
}

/**
 * Implementación del Repository.
 *
 * Recibe el cliente HTTP por constructor para poder reemplazarlo fácilmente
 * (tests, fakes, distintos entornos). La llamada real queda aislada acá para
 * que el ViewModel no conozca detalles de Retrofit ni del JSON del Mock.
 */
class PtahRepositoryImpl(
    private val apiService: PtahApiService
) : PtahRepository {

    override suspend fun searchRegulations(query: String): Result<List<NormativeArticle>> {
        return runCatching {
            Log.d(TAG, "Enviando consulta al Mock: $query")
            val response = apiService.searchRegulations(SemanticSearchRequest(query))
            
            // Si llegamos acá es porque el Interceptor dejó pasar la respuesta (isSuccessful == true)
            val body = response.body()
                ?: throw IllegalStateException("Respuesta vacía del Mock")

            if (!body.status.equals("exito", ignoreCase = true)) {
                throw IllegalStateException(body.message ?: "El Mock devolvió estado inválido")
            }

            val articles = body.results.orEmpty().map { dto ->
                NormativeArticle(
                    id = dto.articleId.orEmpty(),
                    title = dto.title.orEmpty(),
                    content = dto.content.orEmpty(),
                    relevance = dto.relevanceScore
                )
            }

            Log.d(TAG, "Respuesta exitosa del Mock: ${articles.size} artículo(s)")
            articles
        }
    }

    private companion object {
        const val TAG = "PtahRepository"
    }
}
