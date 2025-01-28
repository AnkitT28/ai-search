package com.example.aichat.viewmodel

import android.util.Log
import androidx.compose.ui.text.TextRange
import androidx.compose.ui.text.input.TextFieldValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.aichat.model.ChatMessage
import com.example.aichat.network.models.Profile
import com.example.aichat.repository.ChatRepository
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

class ChatViewModel : ViewModel() {

    private val chatRepository = ChatRepository()

    private val INITIAL_SUGGESTIONS = listOf(
        "View my Kundli-based predictions",
        "Get daily insights from my birth chart",
        "When is the next full moon?",
        "Explore zodiac compatibility",
        "Generate a new Kundli"
    )

    // == StateFlows ==

    private val _chatMessages = MutableStateFlow<List<ChatMessage>>(emptyList())
    val chatMessages: StateFlow<List<ChatMessage>> = _chatMessages

    private val _liveSpeechInput = MutableStateFlow(TextFieldValue(""))
    val liveSpeechInput: StateFlow<TextFieldValue> = _liveSpeechInput.asStateFlow()

    private val _recentSearches = MutableStateFlow<List<String>>(emptyList())
    val recentSearches: StateFlow<List<String>> = _recentSearches.asStateFlow()

    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading

    private val _isRecording = MutableStateFlow(false)
    val isRecording: StateFlow<Boolean> = _isRecording.asStateFlow()

    private val _suggestions = MutableStateFlow(INITIAL_SUGGESTIONS)
    val suggestions: StateFlow<List<String>> = _suggestions.asStateFlow()

    // If suggestions are empty, we hide recent searches.
    private val _showRecentSearches = MutableStateFlow(true)
    val showRecentSearches: StateFlow<Boolean> = _showRecentSearches.asStateFlow()

    // A special index to track the current “partial” message if any
    private var partialMessageIndex: Int? = null

    /** Example profile for demonstration. Replace with your real logic if needed. **/
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

    // == Public Methods ==

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

    private var streamingJob: Job? = null

    fun sendMessage(userMessage: String) {
        if (userMessage.isBlank()) return

        // Cancel any previous job if you only want one active at a time
        streamingJob?.cancel()

        val userChat = ChatMessage(isUser = true, userMessage = userMessage, botResponse = "")
        _chatMessages.value += userChat

        clearSuggestions()
        updateRecentSearches(userMessage)

        val currentProfile = getCurrentUserProfile()

        // Insert placeholder bubble
        val placeholder = ChatMessage(
            isUser = false,
            botResponse = "Waiting for AI response...",
            isLoader = true
        )
        partialMessageIndex = _chatMessages.value.size
        _chatMessages.value += placeholder

        // **Launch a new streamingJob**
        streamingJob = viewModelScope.launch {
            chatRepository.getBotResponseStream(userMessage, currentProfile)
                .collect { incoming ->
                    if (partialMessageIndex == null) {
                        _chatMessages.value += incoming
                    } else {
                        if (incoming.isLoader) {
                            updatePartialBubble(incoming.botResponse ?: "Loading...")
                        } else {
                            replacePartialBubbleWithFinal(incoming)
                        }
                    }
                }
        }
    }


    /** Replace partial bubble text (still isLoader=true). **/
    private fun updatePartialBubble(newText: String) {
        partialMessageIndex?.let { index ->
            val currentList = _chatMessages.value.toMutableList()
            val old = currentList[index]
            // Update the text, keep isLoader = true
            val updated = old.copy(botResponse = newText, isLoader = true)
            currentList[index] = updated
            _chatMessages.value = currentList
        }
    }

    /** Replace the partial bubble with the final chunk (isLoader=false). **/
    private fun replacePartialBubbleWithFinal(finalMessage: ChatMessage) {
        partialMessageIndex?.let { index ->
            val currentList = _chatMessages.value.toMutableList()
            // Overwrite the partial bubble with final data
            currentList[index] = ChatMessage(
                isUser = false,
                botResponse = finalMessage.botResponse,
                navigations = finalMessage.navigations,
                isLoader = false
            )
            _chatMessages.value = currentList
        }
        // Clear partial index once final arrived
        partialMessageIndex = null
    }

    /** Clears all messages. **/
    fun clearMessages() {
        _chatMessages.value = emptyList()
        showRecentSearches()
        partialMessageIndex = null
    }

    fun resetSuggestions() {
        _suggestions.value = INITIAL_SUGGESTIONS
        showRecentSearches()
    }

    fun clearSuggestions() {
        _suggestions.value = emptyList()
        hideRecentSearches()
    }

    fun clearRecentSearches() {
        _recentSearches.value = emptyList()
    }

    fun setIsLoading(value: Boolean) {
        _isLoading.value = value
    }

    // == Helpers ==

    private fun updateRecentSearches(newSearch: String) {
        val list = _recentSearches.value.toMutableList()
        // Remove if it already exists
        list.remove(newSearch)
        // Insert at top
        list.add(0, newSearch)
        // Keep only last 4
        if (list.size > 4) {
            list.removeAt(list.size - 1)
        }
        _recentSearches.value = list
    }

    fun cancelStreaming() {
        streamingJob?.cancel()
        streamingJob = null
        partialMessageIndex = null
    }
}
