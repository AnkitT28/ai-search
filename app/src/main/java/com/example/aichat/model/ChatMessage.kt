package com.example.aichat.model

import com.example.aichat.network.models.Navigation

data class ChatMessage(
    val userMessage: String = "",
    val botResponse: String = "",
    val isUser: Boolean,
    val navigations: List<Navigation>? = null
)


