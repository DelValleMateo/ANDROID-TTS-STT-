package com.uader.ptah.data

import retrofit2.Response
import retrofit2.http.Body
import retrofit2.http.POST

interface PtahApiService {
    @POST("busqueda/semantica")
    suspend fun searchRegulations(
        @Body request: SemanticSearchRequest
    ): Response<SemanticSearchResponse>
}
