//package com.example.aichat.network
//
//import android.util.Log
//import okhttp3.Interceptor
//import okhttp3.OkHttpClient
//import okhttp3.Response
//import okhttp3.logging.HttpLoggingInterceptor
//import java.util.Base64
//
//class NetworkInterceptor {
//
//    companion object {
//        private const val USERNAME = "your-username" // Replace with your username
//        private const val PASSWORD = "your-password" // Replace with your password
//
//        fun getClient(): OkHttpClient {
//            val authToken = "Basic " + ("$USERNAME:$PASSWORD".toByteArray())
//
//            val authInterceptor = Interceptor { chain ->
//                val request = chain.request().newBuilder()
//                    .addHeader("Authorization", authToken)
//                    .addHeader("Content-Type", "application/json")
//                    .build()
//                chain.proceed(request)
//            }
//
//            val loggingInterceptor = HttpLoggingInterceptor { message ->
//                Log.d("API_LOG", message)
//            }.apply {
//                level = HttpLoggingInterceptor.Level.BODY
//            }
//
//            return OkHttpClient.Builder()
//                .addInterceptor(authInterceptor)
//                .addInterceptor(loggingInterceptor)
//                .build()
//        }
//    }
//}
