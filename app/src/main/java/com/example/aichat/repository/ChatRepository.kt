package com.example.aichat.repository

import com.example.aichat.model.ChatMessage

class ChatRepository {

    fun getBotResponse(userMessage: String): ChatMessage {
        val lower = userMessage.lowercase()

        return when {
            "hello" in lower -> {
                ChatMessage(
                    isUser = false,
                    botResponse = "Hi there! How can I assist you today?"
                )
            }
            "today's prediction" in lower || "todays prediction" in lower -> {
                ChatMessage(
                    isUser = false,
                    botResponse = "Here is your prediction for today!",
                    actionLabel = "Know more about Today's Prediction (click here)"
                )
            }
            "weather" in lower -> {
                ChatMessage(
                    isUser = false,
                    botResponse = "I can help you find the weather. Which city?"
                )
            }
            "mumbai" in lower -> {
                ChatMessage(
                    isUser = false,
                    botResponse = "Mumbai is sunny & warm at 24°C right now."
                )
            }
            "name" in lower -> {
                ChatMessage(
                    isUser = false,
                    botResponse = "I'm AIChatBot, your virtual assistant."
                )
            }
            "joke" in lower -> {
                ChatMessage(
                    isUser = false,
                    botResponse = "Why don't scientists trust atoms? Because they make up everything!"
                )
            }
            "help" in lower -> {
                ChatMessage(
                    isUser = false,
                    botResponse = "Sure, I can help with:\n1. Predictions\n2. Weather updates\n3. General queries\nJust ask!"
                )
            }
            "time" in lower -> {
                ChatMessage(
                    isUser = false,
                    botResponse = "I'm a bot, and I never lose track of time! But your device should have the current time."
                )
            }
            "kya ratnesh bhai kaise ho?" in lower -> {
                ChatMessage(
                    isUser = false,
                    botResponse = "Main thik hu tum batao tum kaise ho ? aur kya chal raha hai ?"
                )
            }
            else -> {
                ChatMessage(
                    isUser = false,
                    botResponse = "I'm not sure how to respond to that. Can you ask me something else?"
                )
            }
        }
    }
}
