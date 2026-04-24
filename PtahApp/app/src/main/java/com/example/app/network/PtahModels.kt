package com.example.app.network

import com.google.gson.annotations.SerializedName

data class BusquedaRequest(val consulta: String)

data class BusquedaResponse(
    val estado: String,
    val mensaje: String,
    val resultados: List<ArticuloNormativo>
)

data class ArticuloNormativo(
    @SerializedName("id_articulo") val idArticulo: String,
    val titulo: String,
    val contenido: String,
    @SerializedName("score_relevancia") val scoreRelevancia: Double
)