package com.example.aichat.repository

import android.util.Log
import com.example.aichat.model.ChatMessage
import com.example.aichat.network.ChatApiService
import com.example.aichat.network.RetrofitInstance
import com.example.aichat.network.models.ChatRequest
import com.example.aichat.network.models.Profile
import com.example.aichat.network.models.RecentQueriesResponse
import com.example.aichat.network.models.StreamResponse
import com.example.aichat.network.models.TrendingQueriesResponse
import com.example.aichat.network.models.TrendingQueryCategory
import com.google.gson.Gson
import com.google.gson.JsonSyntaxException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOn
import okhttp3.ResponseBody
import okio.BufferedSource
import okio.Okio
import okio.buffer
import okio.source

class ChatRepository {
    private val api: ChatApiService = RetrofitInstance.api
    private val gson = Gson()

    suspend fun fetchTrendingQueries(): List<TrendingQueryCategory> {
        return try {
            val response: TrendingQueriesResponse = api.getTrendingQueries()
            response.data.queries
        } catch (e: Exception) {
            Log.e("ChatRepository", "Error fetching trending queries: ${e.message}", e)
            emptyList()
        }
    }

    suspend fun fetchRecentQueries(): List<String> {
        return try {
            val response: RecentQueriesResponse = api.getRecentQueries()
            response.data.queries
        } catch (e: Exception) {
            Log.e("ChatRepository", "Error fetching recent queries: ${e.message}", e)
            emptyList()
        }
    }

    /**
     * SSE/streaming method to get AI responses.
     */
    fun getBotResponseStream(userMessage: String, profile: Profile): Flow<ChatMessage> = flow {
        try {
            val request = ChatRequest(
                query = userMessage,
                profiles = listOf(profile),
                user_id = 1
            )
            Log.d("API_Request", "Request JSON: ${gson.toJson(request)}")

            // Make the network call
            val responseBody: ResponseBody = api.getAiResponse(request)

            // Wrap the response body source with a BufferedSource
            val source: BufferedSource = responseBody.byteStream().source().buffer()

            var line: String?
            var lineNumber = 0

            while (true) {
                line = source.readUtf8Line()  // Blocking call until line is available

                if (line == null) break  // EOF reached

                lineNumber++
                Log.d("ChatRepository", "Line $lineNumber: $line")

                if (line.startsWith("data: ")) {
                    val jsonData = line.removePrefix("data: ").trim()
                    if (jsonData.isNotEmpty()) {
                        try {
                            val streamResponse = gson.fromJson(jsonData, StreamResponse::class.java)
                            val isComplete = streamResponse.is_complete
                            val data = streamResponse.data

                            val partialMessage = data.message
                            val finalText = data.search_result ?: data.message
                            val navigations = data.navigations

                            if (!isComplete) {
                                // Emit partial response while streaming
                                emit(
                                    ChatMessage(
                                        isUser = false,
                                        botResponse = partialMessage ?: "...",
                                        navigations = null,
                                        isLoader = true
                                    )
                                )
                            } else {
                                // Emit final response once completed
                                emit(
                                    ChatMessage(
                                        isUser = false,
                                        botResponse = finalText ?: "No final text found.",
                                        navigations = navigations,
                                        isLoader = false
                                    )
                                )
                            }
                        } catch (e: JsonSyntaxException) {
                            Log.e("ChatRepository", "JSON Parsing Error: ${e.message}", e)
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

            Log.d("ChatRepository", "Completed reading the response stream.")
            source.close()

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
