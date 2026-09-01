package com.uader.ptah.data

import com.google.gson.JsonParseException
import com.google.gson.stream.MalformedJsonException
import com.uader.ptah.data.network.HttpStatusException
import com.uader.ptah.data.network.InvalidNetworkResponseException
import com.uader.ptah.data.network.NetworkConfigurationException
import java.io.IOException
import java.net.SocketTimeoutException
import kotlinx.coroutines.CancellationException

interface PtahRepository {
    suspend fun ask(query: String): Result<QueryResponse>
}

class PtahRepositoryImpl(
    private val remoteDataSource: QueryRemoteDataSource
) : PtahRepository {

    override suspend fun ask(query: String): Result<QueryResponse> {
        val cleanQuery = query.trim()
        if (cleanQuery.isEmpty()) {
            return failure(
                error = QueryError.INVALID_REQUEST,
                message = "La consulta no puede estar vacía."
            )
        }

        return try {
            val cleanAnswer = remoteDataSource.ask(cleanQuery)
                .trim()
                .replace("**", "")
                .replace("__", "")
                .replace("###", "")
                .replace("##", "")
                .replace("#", "")

            if (cleanAnswer.isBlank()) {
                throw QueryException(
                    error = QueryError.INVALID_RESPONSE,
                    message = "El servicio devolvió una respuesta vacía o inválida."
                )
            }

            Result.success(QueryResponse(answer = cleanAnswer))
        } catch (exception: CancellationException) {
            throw exception
        } catch (exception: QueryException) {
            Result.failure(exception)
        } catch (exception: NetworkConfigurationException) {
            failure(
                error = QueryError.CONFIGURATION,
                message = "La integración remota no está configurada.",
                cause = exception
            )
        } catch (exception: SocketTimeoutException) {
            failure(
                error = QueryError.TIMEOUT,
                message = "La solicitud superó el tiempo de espera. Intenta nuevamente.",
                cause = exception
            )
        } catch (exception: HttpStatusException) {
            mapHttpError(exception)
        } catch (exception: InvalidNetworkResponseException) {
            failure(
                error = QueryError.INVALID_RESPONSE,
                message = "El servicio devolvió una respuesta vacía o inválida.",
                cause = exception
            )
        } catch (exception: JsonParseException) {
            failure(
                error = QueryError.INVALID_RESPONSE,
                message = "El servicio devolvió una respuesta con formato inválido.",
                cause = exception
            )
        } catch (exception: MalformedJsonException) {
            failure(
                error = QueryError.INVALID_RESPONSE,
                message = "El servicio devolvió una respuesta con formato inválido.",
                cause = exception
            )
        } catch (e: IOException) {
            failure(
                error = QueryError.CONNECTION,
                message = "No se pudo conectar con el servidor. Verifica tu conexión.",
                cause = e
            )
        } catch (exception: Exception) {
            failure(
                error = QueryError.UNKNOWN,
                message = "Ocurrió un error inesperado al consultar el servicio.",
                cause = exception
            )
        }
    }

    private fun mapHttpError(exception: HttpStatusException): Result<QueryResponse> {
        val code = exception.statusCode
        val (error, message) = when (code) {
            401, 403 -> QueryError.AUTHENTICATION to
                "El servicio rechazó la autenticación ($code)."
            408 -> QueryError.TIMEOUT to
                "La solicitud superó el tiempo de espera (408). Intenta nuevamente."
            429 -> QueryError.RATE_LIMIT to
                "El servicio está temporalmente ocupado (429). Intenta nuevamente en unos segundos."
            in 400..499 -> QueryError.CLIENT_ERROR to
                "El servicio rechazó la solicitud ($code)."
            in 500..599 -> QueryError.SERVER_ERROR to
                "El servidor no pudo procesar la consulta ($code). Intenta nuevamente."
            else -> QueryError.UNKNOWN to
                "El servicio respondió con un estado inesperado ($code)."
        }

        return failure(error = error, message = message, cause = exception)
    }

    private fun failure(
        error: QueryError,
        message: String,
        cause: Throwable? = null
    ): Result<QueryResponse> {
        return Result.failure(QueryException(error, message, cause))
    }
}
