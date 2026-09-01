package com.uader.ptah.data.network

import com.uader.ptah.BuildConfig

/** Configuración de red centralizada para el proveedor activo. */
internal object NetworkConfig {
    const val CONNECT_TIMEOUT_SECONDS = 15L
    const val READ_TIMEOUT_SECONDS = 30L
    const val WRITE_TIMEOUT_SECONDS = 15L

    val baseUrl: String
        get() = BuildConfig.PTAH_API_BASE_URL

    val groqApiKey: String
        get() = BuildConfig.GROQ_API_KEY

    val groqModel: String
        get() = BuildConfig.GROQ_MODEL
}
