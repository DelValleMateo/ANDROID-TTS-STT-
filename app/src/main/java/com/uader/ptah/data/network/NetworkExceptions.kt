package com.uader.ptah.data.network

import java.io.IOException

internal class HttpStatusException(
    val statusCode: Int
) : IOException("HTTP $statusCode")

internal class NetworkConfigurationException(
    message: String
) : IOException(message)

internal class InvalidNetworkResponseException(
    message: String,
    cause: Throwable? = null
) : IOException(message, cause)
