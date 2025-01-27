package com.example.aichat.viewmodel

import android.util.Log
import androidx.compose.ui.text.TextRange
import androidx.compose.ui.text.input.TextFieldValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.aichat.model.ChatMessage
import com.example.aichat.repository.ChatRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

class ChatViewModel : ViewModel() {
    private val chatRepository = ChatRepository()

    // Define the initial suggestions as a constant
    private val INITIAL_SUGGESTIONS = listOf(
        "View my Kundli-based predictions",
        "Get daily insights from my birth chart",
        "When is the next full moon?",
        "Explore zodiac compatibility",
        "Generate a new Kundli"
    )

    // StateFlow for chat messages
    private val _chatMessages = MutableStateFlow<List<ChatMessage>>(emptyList())
    val chatMessages: StateFlow<List<ChatMessage>> = _chatMessages

    private val _liveSpeechInput = MutableStateFlow(TextFieldValue(""))
    val liveSpeechInput: StateFlow<TextFieldValue> = _liveSpeechInput.asStateFlow()

    // StateFlow for recent searches
    private val _recentSearches = MutableStateFlow<List<String>>(emptyList())
    val recentSearches: StateFlow<List<String>> = _recentSearches.asStateFlow()

    // StateFlow for loader messages
    private val _loaderMessage = MutableStateFlow<String?>(null) // Single loader message at a time
    val loaderMessage: StateFlow<String?> = _loaderMessage

    // StateFlow for loading status
    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading

    // StateFlow for recording status (e.g., during voice input)
    private val _isRecording = MutableStateFlow(false)
    val isRecording: StateFlow<Boolean> = _isRecording

    // StateFlow for suggestions
    private val _suggestions = MutableStateFlow<List<String>>(INITIAL_SUGGESTIONS)
    val suggestions: StateFlow<List<String>> = _suggestions

    fun setRecording(value: Boolean) {
        _isRecording.value = value
    }


    // Function to update liveSpeechInput with cursor at the end
    fun setLiveSpeechInput(input: String) {
        _liveSpeechInput.value = TextFieldValue(
            text = input,
            selection = TextRange(input.length) // Cursor at the end
        )
    }


    fun sendMessage(userMessage: String) {
        // Add user message to the chat
        val userChat = ChatMessage(
            userMessage = userMessage,
            botResponse = "",
            isUser = true
        )
        _chatMessages.value += userChat

        // Clear suggestions after sending a message
        clearSuggestions()
        updateRecentSearches(userMessage)

        // Fetch bot response from the API
        viewModelScope.launch {
            _isLoading.value = true
            try {
                val botChat = chatRepository.getBotResponse(userMessage)
                _chatMessages.value += botChat
            } catch (e: Exception) {
                // Handle exceptions (e.g., network errors)
                Log.e("ChatViewModel", "Error fetching bot response", e)
                _chatMessages.value += ChatMessage(
                    userMessage = "",
                    botResponse = "Sorry, I couldn't process that. Please try again.",
                    isUser = false
                )
            } finally {
                _isLoading.value = false
            }
        }
    }

    // Function to update recent searches
    private fun updateRecentSearches(newSearch: String) {
        val updatedList = _recentSearches.value.toMutableList()

        // Remove the search if it already exists to avoid duplicates
        updatedList.remove(newSearch)

        // Add the new search at the top
        updatedList.add(0, newSearch)

        // Retain only the last 4 searches
        if (updatedList.size > 4) {
            updatedList.removeAt(updatedList.size - 1)
        }

        _recentSearches.value = updatedList
    }


    private suspend fun getIntermediateLoaderMessages(userMessage: String): List<String> {
        // Simulate intermediate loader messages (replace with API logic)
        return listOf(
            "Processing your request...",
            "Fetching astrological data...",
            "Analyzing your zodiac insights...",
            "Finalizing your prediction..."
        )
    }


    // Clears all chat messages.
    fun clearMessages() {
        _chatMessages.value = emptyList()
    }

    // Resets suggestions to the initial list.
    fun resetSuggestions() {
        _suggestions.value = INITIAL_SUGGESTIONS
    }

    // Clears all suggestions.
    fun clearSuggestions() {
        _suggestions.value = emptyList()
    }

    // Function to clear recent searches (optional)
    fun clearRecentSearches() {
        _recentSearches.value = emptyList()
    }
}
