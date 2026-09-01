package com.uader.ptah.data.network

import android.util.Log
import com.uader.ptah.BuildConfig
import com.uader.ptah.data.GroqApiService
import com.uader.ptah.data.GroqRemoteDataSource
import com.uader.ptah.data.QueryRemoteDataSource
import java.util.concurrent.TimeUnit
import okhttp3.Interceptor
import okhttp3.OkHttpClient
import okhttp3.Response
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory

/**
 * Punto único de creación del cliente HTTP y del proveedor remoto activo.
 *
 * Groq sigue siendo temporal. El contrato PTAH definitivo se conectará aquí
 * mediante otro [QueryRemoteDataSource] cuando se publique su especificación.
 */
object RetrofitProvider {

    private val okHttpClient: OkHttpClient by lazy {
        OkHttpClient.Builder()
            .connectTimeout(NetworkConfig.CONNECT_TIMEOUT_SECONDS, TimeUnit.SECONDS)
            .readTimeout(NetworkConfig.READ_TIMEOUT_SECONDS, TimeUnit.SECONDS)
            .writeTimeout(NetworkConfig.WRITE_TIMEOUT_SECONDS, TimeUnit.SECONDS)
            .addInterceptor(LatencyInterceptor())
            .addInterceptor(GroqAuthInterceptor(NetworkConfig.groqApiKey))
            .apply {
                if (BuildConfig.DEBUG) {
                    addInterceptor(
                        HttpLoggingInterceptor().apply {
                            redactHeader("Authorization")
                            // BASIC evita registrar consultas, respuestas o headers sensibles.
                            level = HttpLoggingInterceptor.Level.BASIC
                        }
                    )
                }
            }
            .build()
    }

    private val groqApiService: GroqApiService by lazy {
        Retrofit.Builder()
            .baseUrl(NetworkConfig.baseUrl)
            .client(okHttpClient)
            .addConverterFactory(GsonConverterFactory.create())
            .build()
            .create(GroqApiService::class.java)
    }

    val queryRemoteDataSource: QueryRemoteDataSource by lazy {
        GroqRemoteDataSource(
            apiService = groqApiService,
            model = NetworkConfig.groqModel
        )
    }
}

internal class GroqAuthInterceptor(
    private val apiKey: String
) : Interceptor {
    override fun intercept(chain: Interceptor.Chain): Response {
        val cleanApiKey = apiKey.trim()
        if (cleanApiKey.isEmpty() || cleanApiKey == "TU_API_KEY_ACA") {
            throw NetworkConfigurationException("Falta la credencial del proveedor temporal.")
        }

        val request = chain.request().newBuilder()
            .header("Authorization", "Bearer $cleanApiKey")
            .header("Accept", "application/json")
            .header("User-Agent", "PTAH-Android/1.0")
            .build()

        return chain.proceed(request)
    }
}

internal class LatencyInterceptor : Interceptor {
    override fun intercept(chain: Interceptor.Chain): Response {
        val request = chain.request()
        val startNs = System.nanoTime()
        var result = "NETWORK_ERROR"

        try {
            val response = chain.proceed(request)
            result = response.code.toString()
            return response
        } finally {
            val latencyMs = (System.nanoTime() - startNs) / NANOS_PER_MILLISECOND
            Log.d(
                TAG,
                "${request.method} ${request.url.encodedPath} -> $result | Latencia: ${latencyMs}ms"
            )
        }
    }

    private companion object {
        const val TAG = "PtahNetwork"
        const val NANOS_PER_MILLISECOND = 1_000_000L
    }
}
