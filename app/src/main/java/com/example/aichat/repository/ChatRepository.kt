package com.example.aichat.repository

import android.util.Log
import com.example.aichat.model.ChatMessage
import com.example.aichat.network.RetrofitInstance
import com.example.aichat.network.models.ChatRequest
import com.example.aichat.network.models.Profile
import com.google.gson.Gson

class ChatRepository {

    private val api = RetrofitInstance.api
    private val gson = Gson() // Initialize Gson for JSON conversion

    suspend fun getBotResponse(userMessage: String): ChatMessage {
        return try {
            // Create a static profile
            val profile = Profile(
                name = "Ankit Tiwari",
                gender = "Male",
                birth_date = 28,
                birth_month = 2,
                birth_year = 1999,
                birth_hour = 11,
                birth_min = 23,
                birth_lat = "25.7464",
                birth_lon = "82.6837",
                birth_place = "Jaunpur, Uttar Pradesh, India",
                is_unknown_time = false,
                birth_timezone = "5.5",
                is_primary = true
            )


            // Prepare the request
            val request = ChatRequest(
                query = userMessage,
                profiles = listOf(profile),
                user_id = 1
            )

            // Convert the request to JSON for logging
            val jsonRequest = gson.toJson(request)
            Log.d("API_Request", "Request JSON: $jsonRequest")

            // API call
            val response = api.getAiResponse(request)

            // Log the full response
            val jsonResponse = gson.toJson(response)
            Log.d("API_Response", "Response: $jsonResponse")

            // Check if response data is available
            val searchResult = response.data?.search_result ?: "No search result available"
            val navigations = response.data?.navigations

            // Return the chat message with navigations
            ChatMessage(
                isUser = false,
                botResponse = searchResult, // Use search_result from the response
                navigations = navigations // Pass navigations to the UI
            )
        } catch (e: Exception) {
            // Log the error with details
            Log.e("API_Error", "Error during API call: ${e.message}", e)

            // Return an error message as a chat message
            ChatMessage(
                isUser = false,
                botResponse = "Sorry, there was an error processing your request."
            )
        }
    }
}
