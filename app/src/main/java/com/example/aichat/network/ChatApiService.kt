package com.example.aichat.network

import com.example.aichat.network.models.ChatRequest
import com.example.aichat.network.models.ChatResponse
import retrofit2.http.Body
import retrofit2.http.POST

interface ChatApiService {
    @POST("search") // Replace with the actual endpoint
    suspend fun getAiResponse(@Body request: ChatRequest): ChatResponse
}

