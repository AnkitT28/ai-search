package com.example.aichat.view

import android.content.Intent
import android.speech.tts.TextToSpeech
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.*
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.aichat.viewmodel.ChatViewModel

@Composable
fun ChatPopup(
    viewModel: ChatViewModel,
    onClose: () -> Unit,
    tts: TextToSpeech?,
    startSpeechToText: () -> Unit
) {
    val chatMessages by viewModel.chatMessages.collectAsState()
    var userInput by remember { mutableStateOf("") }
    val context = LocalContext.current

    // Speak the bot's latest response (optional)
    LaunchedEffect(chatMessages.size) {
        chatMessages.lastOrNull()?.botResponse?.let { response ->
            if (response.isNotBlank()) {
                tts?.speak(response, TextToSpeech.QUEUE_FLUSH, null, null)
            }
        }
    }

    // The “Card” anchored to the top
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .wrapContentHeight()
            .shadow(8.dp)
            .clip(RoundedCornerShape(bottomStart = 16.dp, bottomEnd = 16.dp)),
        colors = CardDefaults.cardColors(containerColor = Color.White)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
        ) {
            // Top Row: Star icon, “AI Chat”, close button
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Left side: Star icon + "AI Chat" title
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = Icons.Default.Star,
                        contentDescription = "star",
                        tint = Color(0xFF3E77FF),
                        modifier = Modifier.size(24.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "AI Chat",
                        style = MaterialTheme.typography.titleMedium.copy(
                            fontWeight = FontWeight.SemiBold,
                            color = Color(0xFF3E77FF)
                        )
                    )
                }
                // Right side: close button
                IconButton(onClick = onClose) {
                    Icon(
                        imageVector = Icons.Default.Close,
                        contentDescription = "Close"
                    )
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Display each message with typewriter effect
            chatMessages.forEach { message ->
                if (message.isUser) {
                    // The user’s question
                    // "You: "
                    TypewriterText(
                        fullText = "You: ${message.userMessage}",
                        modifier = Modifier.padding(bottom = 8.dp)
                    )
                } else {
                    // The bot’s response
                    // "Bot: "
                    // Check if there's an actionLabel
                    if (message.actionLabel.isNotBlank()) {
                        // Combine the response and actionLabel in any style you want
                        // Let’s say "Bot: <response> [actionLabel]"
                        TypewriterText(
                            fullText = "Bot: ${message.botResponse} [${message.actionLabel}]",
                            modifier = Modifier.padding(bottom = 8.dp),
                            onClick = {
                                // On click, open MyPredictionActivity
                                val intent = Intent(context, MyPredictionActivity::class.java)
                                // Optional: you can pass extra data via intent.putExtra(...)
                                context.startActivity(intent)
                            }
                        )
                    } else {
                        // No actionLabel, just show the response
                        TypewriterText(
                            fullText = "Bot: ${message.botResponse}",
                            modifier = Modifier.padding(bottom = 8.dp)
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(20.dp))

            // The input row: text field + mic + send
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(32.dp))
                    .background(Color(0xFFF1F3F4))
                    .padding(horizontal = 16.dp, vertical = 8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    imageVector = Icons.Default.Keyboard,
                    contentDescription = "Keyboard",
                    tint = Color.Gray,
                    modifier = Modifier.size(24.dp)
                )
                Spacer(modifier = Modifier.width(8.dp))

                BasicTextField(
                    value = userInput,
                    onValueChange = { userInput = it },
                    maxLines = 1,
                    textStyle = TextStyle.Default.copy(color = Color.Black),
                    decorationBox = { innerTextField ->
                        if (userInput.isEmpty()) {
                            Text(
                                text = "Ask Your Question",
                                style = TextStyle.Default.copy(color = Color.Gray)
                            )
                        }
                        innerTextField()
                    },
                    modifier = Modifier
                        .weight(1f)
                        .padding(4.dp)
                )

                Spacer(modifier = Modifier.width(8.dp))

                // Microphone icon
                IconButton(
                    onClick = { startSpeechToText() },
                    modifier = Modifier.size(36.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Mic,
                        contentDescription = "Voice",
                        tint = Color.Gray
                    )
                }

                // Send button
                IconButton(
                    onClick = {
                        if (userInput.isNotBlank()) {
                            viewModel.sendMessage(userInput)
                            userInput = ""
                        }
                    },
                    modifier = Modifier.size(36.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Send,
                        contentDescription = "Send",
                        tint = Color.Gray
                    )
                }
            }

            Spacer(modifier = Modifier.height(8.dp))
        }
    }
}
