package com.example.app.network

import retrofit2.Response
import retrofit2.http.Body
import retrofit2.http.POST

interface PtahApiService {
    @POST("busqueda/semantica")
    suspend fun buscarReglamentacion(@Body request: BusquedaRequest): Response<BusquedaResponse>
}