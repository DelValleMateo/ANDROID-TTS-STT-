package com.uader.ptah.data.network

import android.util.Log
import com.uader.ptah.BuildConfig
import com.uader.ptah.data.PtahApiService
import okhttp3.Interceptor
import okhttp3.OkHttpClient
import okhttp3.Response
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import java.io.IOException
import java.util.concurrent.TimeUnit

object RetrofitProvider {

    private const val BASE_URL = "http://10.0.2.2:3000/"

    private val okHttpClient = OkHttpClient.Builder()
        .connectTimeout(15, TimeUnit.SECONDS)
        .readTimeout(15, TimeUnit.SECONDS)
        .writeTimeout(15, TimeUnit.SECONDS)
        // LatencyInterceptor va PRIMERO para medir el tiempo total
        // incluyendo el procesamiento del ErrorInterceptor.
        .addInterceptor(LatencyInterceptor())
        // HttpLoggingInterceptor: activo SOLO en builds de debug.
        // La dependencia es 'debugImplementation', así que R8 la elimina
        // completamente del APK de producción. BuildConfig.DEBUG es la red
        // de seguridad adicional en caso de mezcla accidental de variantes.
        .apply {
            if (BuildConfig.DEBUG) {
                addInterceptor(
                    HttpLoggingInterceptor().apply {
                        level = HttpLoggingInterceptor.Level.BODY
                    }
                )
            }
        }
        .addInterceptor(ErrorInterceptor())
        .build()

    val apiService: PtahApiService by lazy {
        Retrofit.Builder()
            .baseUrl(BASE_URL)
            .client(okHttpClient)
            .addConverterFactory(GsonConverterFactory.create())
            .build()
            .create(PtahApiService::class.java)
    }
}

/**
 * Interceptor de latencia pura de red.
 * Mide el tiempo entre que OkHttp envía la petición y recibe los headers
 * de respuesta (no incluye el parsing de Gson ni la lógica del ViewModel).
 * Buscar en Logcat con el tag "PtahLatency" para ver los valores.
 */
class LatencyInterceptor : Interceptor {
    override fun intercept(chain: Interceptor.Chain): Response {
        val startNs = System.nanoTime()
        val response = chain.proceed(chain.request())
        val latencyMs = (System.nanoTime() - startNs) / 1_000_000
        Log.d(TAG, "[Red] ${chain.request().method()} ${chain.request().url()} → ${response.code} | Latencia: ${latencyMs}ms")
        return response
    }

    private companion object {
        const val TAG = "PtahLatency"
    }
}

/**
 * Interceptor de errores HTTP.
 * Si el servidor responde con un código no-2xx, cierra el body para evitar
 * resource leaks de OkHttp y lanza una [ApiException] con mensaje claro.
 */
class ErrorInterceptor : Interceptor {
    override fun intercept(chain: Interceptor.Chain): Response {
        val response = try {
            chain.proceed(chain.request())
        } catch (e: IOException) {
            // Sin conectividad o timeout: relanzamos con mensaje amigable
            throw IOException("Error de conexión: Revisa tu internet.", e)
        }

        if (!response.isSuccessful) {
            // FIX: cerrar el body ANTES de lanzar para evitar resource leaks
            response.body?.close()

            // FIX: usar propiedad Kotlin `response.code` en lugar del método Java `response.code()`
            val errorMsg = when (response.code) {
                400 -> "Petición incorrecta (400)"
                401 -> "No autorizado (401)"
                403 -> "Acceso prohibido (403)"
                404 -> "Recurso no encontrado (404)"
                408 -> "Tiempo de espera agotado (408)"
                500 -> "Error en el servidor (500)"
                502 -> "Gateway inválido (502)"
                503 -> "Servicio no disponible (503)"
                504 -> "Gateway timeout (504)"
                else -> "Error de red inesperado (${response.code})"
            }
            throw ApiException(errorMsg, response.code)
        }
        return response
    }
}

/** Excepción personalizada para errores HTTP con código de estado disponible. */
class ApiException(message: String, val code: Int) : IOException(message)
