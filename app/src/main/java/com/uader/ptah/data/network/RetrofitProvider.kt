package com.uader.ptah.data.network

import android.util.Log
import com.uader.ptah.BuildConfig
import com.uader.ptah.data.GroqApiService
import okhttp3.Interceptor
import okhttp3.OkHttpClient
import okhttp3.Response
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import java.io.IOException
import java.net.SocketTimeoutException
import java.util.concurrent.TimeUnit

object RetrofitProvider {

    private const val BASE_URL = "https://api.groq.com/openai/v1/"

    private val okHttpClient = OkHttpClient.Builder()
        .connectTimeout(15, TimeUnit.SECONDS)
        .readTimeout(30, TimeUnit.SECONDS)
        .writeTimeout(15, TimeUnit.SECONDS)
        .addInterceptor(ApiKeyInterceptor())
        .addInterceptor(LatencyInterceptor())
        .apply {
            if (BuildConfig.DEBUG) {
                addInterceptor(
                    HttpLoggingInterceptor().apply {
                        redactHeader("Authorization")
                        level = HttpLoggingInterceptor.Level.BODY
                    }
                )
            }
        }
        .addInterceptor(ErrorInterceptor())
        .build()

    val groqApiService: GroqApiService by lazy {
        Retrofit.Builder()
            .baseUrl(BASE_URL)
            .client(okHttpClient)
            .addConverterFactory(GsonConverterFactory.create())
            .build()
            .create(GroqApiService::class.java)
    }
}

class ApiKeyInterceptor : Interceptor {
    override fun intercept(chain: Interceptor.Chain): Response {
        val apiKey = BuildConfig.GROQ_API_KEY.trim()
        Log.d(TAG, "GROQ_API_KEY configurada: ${apiKey.isNotBlank()}")

        if (apiKey.isEmpty() || apiKey == "TU_API_KEY_ACA") {
            throw ApiException("La API key de Groq no está configurada. Agrégala en local.properties como GROQ_API_KEY=gsk_...", 401)
        }

        val request = chain.request().newBuilder()
            .addHeader("Authorization", "Bearer $apiKey")
            .addHeader("Content-Type", "application/json")
            .build()

        return chain.proceed(request)
    }

    private companion object {
        const val TAG = "GroqApiKey"
    }
}

class LatencyInterceptor : Interceptor {
    override fun intercept(chain: Interceptor.Chain): Response {
        val startNs = System.nanoTime()
        val response = chain.proceed(chain.request())
        val latencyMs = (System.nanoTime() - startNs) / 1_000_000
        Log.d(TAG, "[Red] ${chain.request().method} ${chain.request().url} -> ${response.code} | Latencia: ${latencyMs}ms")
        return response
    }

    private companion object {
        const val TAG = "PtahLatency"
    }
}

class ErrorInterceptor : Interceptor {
    override fun intercept(chain: Interceptor.Chain): Response {
        val response = try {
            chain.proceed(chain.request())
        } catch (e: SocketTimeoutException) {
            throw IOException("No se pudo conectar con el servidor. La solicitud superó el tiempo de espera.", e)
        } catch (e: IOException) {
            throw IOException("No se pudo conectar con el servidor. Verifica tu conexión a internet.", e)
        }

        if (!response.isSuccessful) {
            val errorBody = try {
                response.body?.string().orEmpty()
            } catch (e: Exception) {
                ""
            }
            Log.e("ErrorInterceptor", "Error HTTP ${response.code}: $errorBody")

            val errorMsg = when (response.code) {
                400 -> "Solicitud inválida (400). Revisa los parámetros enviados a Groq."
                401 -> "No autorizado (401). Verifica tu GROQ_API_KEY en local.properties."
                403 -> "Acceso prohibido (403). Revisa los permisos de tu API key de Groq."
                404 -> "Recurso o modelo no encontrado (404). Verifica el modelo configurado en Groq."
                408 -> "La solicitud superó el tiempo de espera (408)."
                429 -> "Límite de cuota alcanzado en Groq (429). Intenta nuevamente en unos segundos."
                in 500..599 -> "Error interno del servidor Groq (${response.code}). Intenta nuevamente."
                else -> "El servicio respondió con error (${response.code}): $errorBody"
            }
            throw ApiException(errorMsg, response.code)
        }

        return response
    }
}

class ApiException(message: String, val code: Int) : IOException(message)
