package com.example.aichat.network

import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory


object RetrofitInstance {
    private const val BASE_URL =
        "https://app-services-dev.vedicrishi.in/ai-search-service/"

    // Use the OkHttpClient from NetworkInterceptor
    private val client = NetworkInterceptor.getClient()

    val api: ChatApiService by lazy {
        Retrofit.Builder()
            .baseUrl(BASE_URL)
            .client(client) // Attach the client with interceptors and timeouts
            .addConverterFactory(GsonConverterFactory.create())
            .build()
            .create(ChatApiService::class.java)
    }
}

