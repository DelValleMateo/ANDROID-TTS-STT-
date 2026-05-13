package com.uader.ptah.data.network

import com.uader.ptah.data.PtahApiService
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory

object RetrofitProvider {

    // URL base placeholder. Se reemplazará por el endpoint real cuando esté disponible.
    private const val BASE_URL = "https://ptah.invalid/"

    val apiService: PtahApiService by lazy {
        Retrofit.Builder()
            .baseUrl(BASE_URL)
            .addConverterFactory(GsonConverterFactory.create())
            .build()
            .create(PtahApiService::class.java)
    }
}
