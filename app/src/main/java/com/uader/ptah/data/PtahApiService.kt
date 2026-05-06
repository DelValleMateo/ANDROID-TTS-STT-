package com.uader.ptah.data

import retrofit2.Response
import retrofit2.http.Body
import retrofit2.http.POST

interface  PtahApiService {
    @POST("search")
    suspend fun searchRegulations(
        @Body request: QueryRequest
    ): Response<SearchResponse>
}