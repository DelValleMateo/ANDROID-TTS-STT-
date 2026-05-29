package com.uader.ptah.data

import retrofit2.Response
import retrofit2.http.Body
import retrofit2.http.POST

interface GroqApiService {
    @POST("openai/v1/chat/completions")
    suspend fun createChatCompletion(
        @Body request: GroqChatCompletionRequest
    ): Response<GroqChatCompletionResponse>
}
