package com.example.aichat.model

import com.example.aichat.network.models.Navigation

data class ChatMessage(
    val isUser: Boolean,
    val userMessage: String = "",
    val botResponse: String?,
    val navigations: List<Navigation>? = null,
    val isLoader: Boolean = false // Indicates if this is a loader message
)
