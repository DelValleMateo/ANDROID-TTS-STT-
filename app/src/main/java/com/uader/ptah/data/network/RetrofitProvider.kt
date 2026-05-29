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

    private const val BASE_URL = "https://api.groq.com/"

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
        Log.d(TAG, "Longitud de GROQ_API_KEY: ${apiKey.length} caracteres")

        if (apiKey.isEmpty()) {
            throw ApiException("La API key de Groq no esta configurada.", 0)
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
            throw IOException("No se pudo conectar con el servidor. La solicitud supero el tiempo de espera.", e)
        } catch (e: IOException) {
            throw IOException("No se pudo conectar con el servidor. Verifica tu conexion.", e)
        }

        if (!response.isSuccessful) {
            response.body?.close()

            val errorMsg = when (response.code) {
                400 -> "El servicio respondio con una solicitud invalida (400)."
                401 -> "No autorizado por Groq. Revisa la API key (401)."
                403 -> "Acceso prohibido por Groq. Revisa permisos o restricciones de la API key (403)."
                408 -> "La solicitud supero el tiempo de espera (408)."
                429 -> "Se alcanzo el limite de uso de Groq. Intenta nuevamente mas tarde (429)."
                in 500..599 -> "Groq respondio con un error del servidor (${response.code}). Intenta nuevamente."
                else -> "El servicio respondio con un error (${response.code}). Intenta nuevamente."
            }
            throw ApiException(errorMsg, response.code)
        }

        return response
    }
}

class ApiException(message: String, val code: Int) : IOException(message)
