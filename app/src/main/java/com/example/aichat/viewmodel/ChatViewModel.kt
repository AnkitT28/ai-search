package com.example.aichat.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.aichat.model.ChatMessage
import com.example.aichat.repository.ChatRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch

class ChatViewModel : ViewModel() {
    private val chatRepository = ChatRepository()

    private val _chatMessages = MutableStateFlow<List<ChatMessage>>(emptyList())
    val chatMessages: StateFlow<List<ChatMessage>> = _chatMessages

    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading

    private val _isRecording = MutableStateFlow(false)
    val isRecording: StateFlow<Boolean> = _isRecording

    fun setRecording(value: Boolean) {
        _isRecording.value = value
    }

    fun sendMessage(userMessage: String) {
        // Add user message to the chat
        val userChat = ChatMessage(
            userMessage = userMessage,
            botResponse = "",
            isUser = true
        )
        _chatMessages.value += userChat

        // Fetch bot response from the API
        viewModelScope.launch {
            _isLoading.value = true
            val botChat = chatRepository.getBotResponse(userMessage)
            _chatMessages.value += botChat
            _isLoading.value = false
        }
    }
}