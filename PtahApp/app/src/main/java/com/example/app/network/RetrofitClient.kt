package com.example.app.network

import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory

object RetrofitClient {
    // La IP especial para que el emulador lea el localhost de tu compu
    private const val BASE_URL = "http://10.0.2.2:3000/"

    val apiService: PtahApiService by lazy {
        Retrofit.Builder()
            .baseUrl(BASE_URL)
            .addConverterFactory(GsonConverterFactory.create())
            .build()
            .create(PtahApiService::class.java)
    }
}