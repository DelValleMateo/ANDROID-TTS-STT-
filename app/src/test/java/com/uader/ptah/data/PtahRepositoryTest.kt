package com.uader.ptah.data

import com.uader.ptah.data.network.HttpStatusException
import com.uader.ptah.data.network.NetworkConfigurationException
import java.io.IOException
import java.net.SocketTimeoutException
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Assert.fail
import org.junit.Test

class PtahRepositoryTest {

    @Test
    fun `ask normalizes the query and returns a provider-neutral response`() = runTest {
        var receivedQuery = ""
        val repository = repositoryWith { query ->
            receivedQuery = query
            "  **Respuesta** válida  "
        }

        val result = repository.ask("  licencia por examen  ")

        assertEquals("licencia por examen", receivedQuery)
        assertEquals("Respuesta válida", result.getOrThrow().answer)
    }

    @Test
    fun `ask rejects an empty query without calling the network`() = runTest {
        var networkWasCalled = false
        val repository = repositoryWith {
            networkWasCalled = true
            "respuesta"
        }

        val exception = repository.ask("   ").exceptionOrNull() as QueryException

        assertEquals(QueryError.INVALID_REQUEST, exception.error)
        assertTrue(!networkWasCalled)
    }

    @Test
    fun `ask maps an empty provider response as invalid`() = runTest {
        val repository = repositoryWith { "  " }

        val exception = repository.ask("consulta").exceptionOrNull() as QueryException

        assertEquals(QueryError.INVALID_RESPONSE, exception.error)
    }

    @Test
    fun `ask maps socket timeouts`() = runTest {
        val repository = repositoryWith { throw SocketTimeoutException("timeout") }

        val exception = repository.ask("consulta").exceptionOrNull() as QueryException

        assertEquals(QueryError.TIMEOUT, exception.error)
    }

    @Test
    fun `ask maps connection failures`() = runTest {
        val repository = repositoryWith { throw IOException("offline") }

        val exception = repository.ask("consulta").exceptionOrNull() as QueryException

        assertEquals(QueryError.CONNECTION, exception.error)
    }

    @Test
    fun `ask maps HTTP rate limiting without exposing an error body`() = runTest {
        val repository = repositoryWith { throw HttpStatusException(429) }

        val exception = repository.ask("consulta").exceptionOrNull() as QueryException

        assertEquals(QueryError.RATE_LIMIT, exception.error)
        assertTrue(exception.message.orEmpty().contains("429"))
    }

    @Test
    fun `ask maps HTTP authentication failures`() = runTest {
        val repository = repositoryWith { throw HttpStatusException(401) }

        val exception = repository.ask("consulta").exceptionOrNull() as QueryException

        assertEquals(QueryError.AUTHENTICATION, exception.error)
    }

    @Test
    fun `ask maps HTTP server failures`() = runTest {
        val repository = repositoryWith { throw HttpStatusException(503) }

        val exception = repository.ask("consulta").exceptionOrNull() as QueryException

        assertEquals(QueryError.SERVER_ERROR, exception.error)
    }

    @Test
    fun `ask maps a missing temporary provider configuration`() = runTest {
        val repository = repositoryWith {
            throw NetworkConfigurationException("missing credential")
        }

        val exception = repository.ask("consulta").exceptionOrNull() as QueryException

        assertEquals(QueryError.CONFIGURATION, exception.error)
        assertTrue(!exception.message.orEmpty().contains("credential"))
    }

    @Test
    fun `ask preserves coroutine cancellation`() = runTest {
        val repository = repositoryWith { throw CancellationException("cancelled") }

        try {
            repository.ask("consulta")
            fail("La cancelación no debe convertirse en Result.failure")
        } catch (_: CancellationException) {
            // Expected: structured concurrency remains intact.
        }
    }

    private fun repositoryWith(
        response: suspend (String) -> String
    ): PtahRepository {
        return PtahRepositoryImpl(
            remoteDataSource = QueryRemoteDataSource { query -> response(query) }
        )
    }
}
