package com.example.aichat.viewmodel

import android.util.Log
import androidx.compose.ui.text.TextRange
import androidx.compose.ui.text.input.TextFieldValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.aichat.model.ChatMessage
import com.example.aichat.network.models.Profile
import com.example.aichat.network.models.TrendingQueryCategory
import com.example.aichat.repository.ChatRepository
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

class ChatViewModel : ViewModel() {

    private val chatRepository = ChatRepository()

    // == StateFlows ==

    private val _chatMessages = MutableStateFlow<List<ChatMessage>>(emptyList())
    val chatMessages: StateFlow<List<ChatMessage>> = _chatMessages

    private val _liveSpeechInput = MutableStateFlow(TextFieldValue(""))
    val liveSpeechInput: StateFlow<TextFieldValue> = _liveSpeechInput.asStateFlow()

    private val _recentSearches = MutableStateFlow<List<String>>(emptyList())
    val recentSearches: StateFlow<List<String>> = _recentSearches.asStateFlow()

    private val _suggestions = MutableStateFlow<List<TrendingQueryCategory>>(emptyList())
    val suggestions: StateFlow<List<TrendingQueryCategory>> = _suggestions.asStateFlow()


    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading

    private val _isRecording = MutableStateFlow(false)
    val isRecording: StateFlow<Boolean> = _isRecording.asStateFlow()

    // If suggestions are empty, we hide recent searches in the UI
    private val _showRecentSearches = MutableStateFlow(true)
    val showRecentSearches: StateFlow<Boolean> = _showRecentSearches.asStateFlow()

    // Track partial message index for streaming
    private var partialMessageIndex: Int? = null
    private var streamingJob: Job? = null

    init {
        // Immediately load suggestions (trending) and recent searches from the server
        loadSuggestionsAndRecentSearches()
    }

    /** Example profile data, adjust as needed. */
    private fun getCurrentUserProfile(): Profile {
        return Profile(
            name = "Alok Prasad",
            gender = "Male",
            birth_date = 24,
            birth_month = 3,
            birth_year = 1995,
            birth_hour = 14,
            birth_min = 47,
            birth_lat = "26.993314",
            birth_lon = "84.404072",
            birth_place = "Lauriya, Bihar",
            is_unknown_time = false,
            birth_timezone = "5.5",
            is_primary = true
        )
    }

    // =========================
    // Public / UI-facing methods
    // =========================

    /**
     * Loads trending suggestions and recent searches from the server.
     */
    private fun loadSuggestionsAndRecentSearches() {
        viewModelScope.launch {
            try {
                _isLoading.value = true

                // Fetch trending queries => suggestions
                val trendingList = chatRepository.fetchTrendingQueries()
                Log.d("ChatViewModel", "Trending Suggestions: $trendingList")

                // Set the fetched trending list directly to suggestions
                _suggestions.value = trendingList

                // Fetch recent queries => recentSearches
                val recentList = chatRepository.fetchRecentQueries()
                Log.d("ChatViewModel", "Recent Searches: $recentList")
                _recentSearches.value = recentList

                // If suggestions are empty, hide recent searches
                _showRecentSearches.value = trendingList.isNotEmpty()

            } catch (e: Exception) {
                Log.e("ChatViewModel", "Error loading suggestions: ${e.message}", e)
            } finally {
                _isLoading.value = false
            }
        }
    }



    fun setRecording(value: Boolean) {
        _isRecording.value = value
    }

    fun showRecentSearches() {
        _showRecentSearches.value = true
    }

    fun hideRecentSearches() {
        _showRecentSearches.value = false
    }

    fun setLiveSpeechInput(input: String) {
        _liveSpeechInput.value = TextFieldValue(
            text = input,
            selection = TextRange(input.length)
        )
    }

    /**
     * Send a message to the backend, collect the SSE streaming response.
     */
    fun sendMessage(userMessage: String) {
        if (userMessage.isBlank()) return

        // Cancel any previous streaming if it’s still running
        streamingJob?.cancel()

        // 1) Add the user’s message to our chat flow
        val userChat = ChatMessage(isUser = true, userMessage = userMessage, botResponse = "")
        _chatMessages.value += userChat

        // 2) Clear suggestions from UI while conversation is happening
        clearSuggestions()

        // 3) Update local "recentSearches" (top of the list)
        updateRecentSearches(userMessage)

        // 4) Insert a placeholder bubble for AI response
        val placeholder = ChatMessage(
            isUser = false,
            botResponse = "Waiting for AI response...",
            isLoader = true
        )
        partialMessageIndex = _chatMessages.value.size
        _chatMessages.value += placeholder

        val profile = getCurrentUserProfile()

        // 5) Start streaming from the repository
        streamingJob = viewModelScope.launch {
            chatRepository.getBotResponseStream(userMessage, profile)
                .collect { incoming ->
                    if (partialMessageIndex == null) {
                        // If partialMessageIndex got reset, just append
                        _chatMessages.value += incoming
                    } else {
                        // Check if the incoming data is partial (loader) or the final response
                        if (incoming.isLoader) {
                            // Update with loading message or partial message
                            updatePartialBubble(incoming.botResponse ?: "Loading...")
                            Log.d("PARTIAL_RESPONSE_LOG", "sendMessage: partialMessageIndex: $_isLoading")
                            Log.d("PARTIAL_RESPONSE_LOG", "sendMessage: partialMessageIndex: ${incoming.isLoader}")
                        } else {
                            // Once final message is complete, replace the placeholder
                            replacePartialBubbleWithFinal(incoming)
                        }
                    }
                }
        }
    }


    /**
     * Clears all messages in the chat. Show suggestions/recent again if needed.
     */
    fun clearMessages() {
        _chatMessages.value = emptyList()
        showRecentSearches()
        partialMessageIndex = null
    }

    /**
     * Clears the suggestions from the UI.
     */
    fun clearSuggestions() {
        _suggestions.value = emptyList()
        hideRecentSearches()
    }

    /**
     * If you need to clear the local recent searches list. (Does not call an API.)
     */
    fun clearRecentSearches() {
        _recentSearches.value = emptyList()
    }

    fun setIsLoading(value: Boolean) {
        _isLoading.value = value
    }

    /**
     * Cancel SSE streaming if needed.
     */
    fun cancelStreaming() {
        streamingJob?.cancel()
        streamingJob = null
        partialMessageIndex = null
    }

    // =========================
    // Private helpers
    // =========================

    /**
     * Update the partial bubble text while still streaming.
     */
    private fun updatePartialBubble(newText: String) {
        partialMessageIndex?.let { index ->
            val currentList = _chatMessages.value.toMutableList()
            val old = currentList[index]
            val updated = old.copy(botResponse = newText, isLoader = true)
            currentList[index] = updated
            _chatMessages.value = currentList
        }
    }

    /**
     * Once the final chunk arrives from SSE (isLoader = false), replace
     * the placeholder bubble with the final text and navigations.
     */
    private fun replacePartialBubbleWithFinal(finalMessage: ChatMessage) {
        partialMessageIndex?.let { index ->
            val currentList = _chatMessages.value.toMutableList()
            currentList[index] = ChatMessage(
                isUser = false,
                botResponse = finalMessage.botResponse,
                navigations = finalMessage.navigations,
                isLoader = false
            )
            _chatMessages.value = currentList
        }
        partialMessageIndex = null
    }

    /**
     * Adds a new search to the top of recent searches in local memory.
     * Removes duplicates and only keeps up to 4 in this example.
     */
    private fun updateRecentSearches(newSearch: String) {
        val list = _recentSearches.value.toMutableList()
        // Remove if it already exists
        list.remove(newSearch)
        // Insert at top
        list.add(0, newSearch)
        // Keep only last 4 (optional limit)
        if (list.size > 4) {
            list.removeAt(list.size - 1)
        }
        _recentSearches.value = list
    }
}
