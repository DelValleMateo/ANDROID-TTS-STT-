package com.uader.ptah.data

import kotlinx.coroutines.delay

interface PtahRepository {
    suspend fun searchRegulations(query: String): Result<List<NormativeArticle>>
}

/**
 * Implementación del Repository.
 *
 * Recibe el cliente HTTP por constructor para poder reemplazarlo fácilmente
 * (tests, fakes, distintos entornos). Mientras el backend no esté listo,
 * usa [useFakeBackend] para devolver una respuesta simulada con una demora.
 */
class PtahRepositoryImpl(
    private val apiService: PtahApiService,
    private val useFakeBackend: Boolean = true
) : PtahRepository {

    override suspend fun searchRegulations(query: String): Result<List<NormativeArticle>> {
        if (useFakeBackend) {
            return runCatching {
                delay(1200)
                listOf(
                    NormativeArticle(
                        id = 1,
                        title = "Resultado simulado",
                        content = "Consulta recibida: \"$query\". " +
                            "La conexión con el servidor aún no está habilitada.",
                        relevance = 1.0
                    )
                )
            }
        }

        return runCatching {
            val response = apiService.searchRegulations(QueryRequest(query))
            if (response.isSuccessful) {
                response.body()?.results.orEmpty()
            } else {
                throw IllegalStateException("HTTP ${response.code()}")
            }
        }
    }
}
