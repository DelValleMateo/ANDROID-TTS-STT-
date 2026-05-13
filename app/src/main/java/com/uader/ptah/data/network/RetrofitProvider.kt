package com.uader.ptah.data.network

import com.uader.ptah.data.PtahApiService
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory

object RetrofitProvider {

    // 10.0.2.2 permite que el emulador Android llegue al localhost de la computadora.
    private const val BASE_URL = "http://10.0.2.2:3000/"

    val apiService: PtahApiService by lazy {
        Retrofit.Builder()
            .baseUrl(BASE_URL)
            .addConverterFactory(GsonConverterFactory.create())
            .build()
            .create(PtahApiService::class.java)
    }
}
