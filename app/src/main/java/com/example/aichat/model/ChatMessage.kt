package com.example.aichat.model

data class ChatMessage(
    val userMessage: String = "",
    val botResponse: String = "",
    val isUser: Boolean,
    val actionLabel: String = "" // Optional: for clickable actions
)
