package com.uader.ptah.data

import com.google.gson.Gson
import com.uader.ptah.data.network.HttpStatusException
import kotlinx.coroutines.test.runTest
import okhttp3.mockwebserver.MockResponse
import okhttp3.mockwebserver.MockWebServer
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory

class GroqRemoteDataSourceTest {

    private lateinit var server: MockWebServer

    @Before
    fun setUp() {
        server = MockWebServer()
        server.start()
    }

    @After
    fun tearDown() {
        server.shutdown()
    }

    @Test
    fun `temporary adapter sends the validated Groq endpoint and request`() = runTest {
        server.enqueue(
            MockResponse()
                .setResponseCode(200)
                .setHeader("Content-Type", "application/json")
                .setBody(
                    """
                    {
                      "choices": [
                        {"message": {"content": "Respuesta temporal"}}
                      ]
                    }
                    """.trimIndent()
                )
        )

        val answer = dataSource().ask("¿Qué establece el reglamento?")
        val request = server.takeRequest()
        val sentRequest = Gson().fromJson(
            request.body.readUtf8(),
            GroqChatCompletionRequest::class.java
        )

        assertEquals("Respuesta temporal", answer)
        assertEquals("POST", request.method)
        assertEquals("/openai/v1/chat/completions", request.path)
        assertEquals(TEST_MODEL, sentRequest.model)
        assertEquals("system", sentRequest.messages[0].role)
        assertEquals("user", sentRequest.messages[1].role)
        assertEquals(
            "¿Qué establece el reglamento?",
            sentRequest.messages[1].content
        )
    }

    @Test
    fun `temporary adapter rejects HTTP errors without propagating the response body`() = runTest {
        val sensitiveBody = "detalle interno que no debe propagarse"
        server.enqueue(
            MockResponse()
                .setResponseCode(503)
                .setBody(sensitiveBody)
        )

        val exception = runCatching { dataSource().ask("consulta") }.exceptionOrNull()

        assertTrue(exception is HttpStatusException)
        assertEquals(503, (exception as HttpStatusException).statusCode)
        assertFalse(exception.message.orEmpty().contains(sensitiveBody))
    }

    @Test
    fun `temporary adapter tolerates a structurally empty successful payload`() = runTest {
        server.enqueue(
            MockResponse()
                .setResponseCode(200)
                .setHeader("Content-Type", "application/json")
                .setBody("{}")
        )

        assertEquals("", dataSource().ask("consulta"))
    }

    private fun dataSource(): GroqRemoteDataSource {
        val apiService = Retrofit.Builder()
            .baseUrl(server.url("/openai/v1/"))
            .addConverterFactory(GsonConverterFactory.create())
            .build()
            .create(GroqApiService::class.java)

        return GroqRemoteDataSource(apiService = apiService, model = TEST_MODEL)
    }

    private companion object {
        const val TEST_MODEL = "test-model"
    }
}
