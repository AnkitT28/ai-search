package com.example.aichat.repository

import android.util.Log
import com.example.aichat.model.ChatMessage
import com.example.aichat.network.ChatApiService
import com.example.aichat.network.RetrofitInstance
import com.example.aichat.network.models.ChatRequest
import com.example.aichat.network.models.Profile
import com.example.aichat.network.models.StreamResponse
import com.google.gson.Gson
import com.google.gson.JsonSyntaxException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOn
import okhttp3.ResponseBody
import java.io.BufferedReader
import java.io.InputStreamReader

class ChatRepository {
    private val api: ChatApiService = RetrofitInstance.api
    private val gson = Gson()

    fun getBotResponseStream(userMessage: String, profile: Profile): Flow<ChatMessage> = flow {
        try {
            val request = ChatRequest(
                query = userMessage,
                profiles = listOf(profile),
                user_id = 1
            )
            Log.d("API_Request", "Request JSON: ${gson.toJson(request)}")

            val responseBody: ResponseBody = api.getAiResponse(request)
            val reader = BufferedReader(InputStreamReader(responseBody.byteStream()))
            var line: String?
            var lineNumber = 0

            while (reader.readLine().also { line = it } != null) {
                lineNumber++
                Log.d("ChatRepository", "Line $lineNumber: $line")

                // Typical SSE lines start with "data:"
                if (line != null && line!!.startsWith("data: ")) {
                    val jsonData = line!!.removePrefix("data: ").trim()
                    Log.d("ChatRepository", "Raw JSON data: $jsonData")

                    if (jsonData.isNotEmpty()) {
                        try {
                            val streamResponse = gson.fromJson(jsonData, StreamResponse::class.java)
                            val isComplete = streamResponse.is_complete
                            val data = streamResponse.data

                            val partialMessage = data.message               // If is_complete=false
                            val finalText = data.search_result ?: data.message
                            val navigations = data.navigations

                            Log.d(
                                "ChatRepository",
                                "Parsed: message=$partialMessage, isComplete=$isComplete"
                            )

                            if (!isComplete) {
                                // Emit partial text (loader=true)
                                emit(
                                    ChatMessage(
                                        isUser = false,
                                        botResponse = partialMessage ?: "...",
                                        navigations = null,
                                        isLoader = true
                                    )
                                )
                            } else {
                                // Emit final text (loader=false)
                                emit(
                                    ChatMessage(
                                        isUser = false,
                                        botResponse = finalText ?: "No final text found.",
                                        navigations = navigations,
                                        isLoader = false
                                    )
                                )
                            }
                        } catch (parseException: JsonSyntaxException) {
                            Log.e("ChatRepository", "JSON Parsing Error: ${parseException.message}", parseException)
                            emit(
                                ChatMessage(
                                    isUser = false,
                                    botResponse = "Received malformed data from the server.",
                                    navigations = null,
                                    isLoader = false
                                )
                            )
                        }
                    }
                }
            }
            reader.close()
            Log.d("ChatRepository", "Completed reading the response stream.")
        } catch (e: Exception) {
            Log.e("ChatRepository", "Error processing stream: ${e.message}", e)
            emit(
                ChatMessage(
                    isUser = false,
                    botResponse = "Sorry, there was an error processing your request.",
                    navigations = null,
                    isLoader = false
                )
            )
        }
    }.flowOn(Dispatchers.IO)
}
