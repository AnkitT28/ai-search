package com.example.aichat.network

import android.util.Log
import okhttp3.Interceptor
import okhttp3.OkHttpClient
import okhttp3.Response
import okhttp3.logging.HttpLoggingInterceptor

class NetworkInterceptor {

    companion object {
        private const val APP_DB_HEADER = "X-APP-DB"
        private const val APP_DB_VALUE = "APP_UPASTROLOGY"

        fun getClient(): OkHttpClient {
            // Interceptor to add custom headers
            val headerInterceptor = Interceptor { chain ->
                val request = chain.request().newBuilder()
                    .addHeader(APP_DB_HEADER, APP_DB_VALUE)
                    .build()
                chain.proceed(request)
            }

            // Logging interceptor for debugging
            val loggingInterceptor = HttpLoggingInterceptor { message ->
                Log.d("API_LOG", message)
            }.apply {
                level = HttpLoggingInterceptor.Level.BODY
            }

            return OkHttpClient.Builder()
                .addInterceptor(headerInterceptor)
                .addInterceptor(loggingInterceptor)
                .connectTimeout(60, java.util.concurrent.TimeUnit.SECONDS)
                .readTimeout(60, java.util.concurrent.TimeUnit.SECONDS)
                .writeTimeout(60, java.util.concurrent.TimeUnit.SECONDS)
                .build()
        }
    }
}
