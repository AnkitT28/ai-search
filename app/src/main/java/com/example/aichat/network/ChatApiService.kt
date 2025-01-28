package com.example.aichat.network

import com.example.aichat.network.models.ChatRequest
import com.example.aichat.network.models.RecentQueriesResponse
import com.example.aichat.network.models.TrendingQueriesResponse
import okhttp3.ResponseBody
import retrofit2.http.Body
import retrofit2.http.GET
import retrofit2.http.POST
import retrofit2.http.Streaming


interface ChatApiService {
    @Streaming
    @POST("mock") // Replace with your actual endpoint
    suspend fun getAiResponse(@Body request: ChatRequest): ResponseBody


    @GET("queries/trending")
    suspend fun getTrendingQueries(): TrendingQueriesResponse

    @GET("queries/user/recent")
    suspend fun getRecentQueries(): RecentQueriesResponse
}
