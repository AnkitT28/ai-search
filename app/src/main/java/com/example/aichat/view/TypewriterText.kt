package com.example.aichat.view

import androidx.compose.runtime.*
import androidx.compose.material3.Text
import androidx.compose.ui.Modifier
import androidx.compose.foundation.clickable
import kotlinx.coroutines.delay
import androidx.compose.ui.text.TextStyle

@Composable
fun TypewriterText(
    fullText: String,
    modifier: Modifier = Modifier,
    onClick: (() -> Unit)? = null,
    style: TextStyle = androidx.compose.material3.MaterialTheme.typography.bodyMedium
) {
    var displayedText by remember { mutableStateOf("") }

    // Animate text appearance (character by character)
    LaunchedEffect(fullText) {
        displayedText = ""
        for (char in fullText) {
            displayedText += char
            delay(40) // Adjust speed as needed
        }
    }

    // If onClick is provided, wrap Text in clickable
    if (onClick != null) {
        Text(
            text = displayedText,
            modifier = modifier.clickable { onClick() },
            style = style
        )
    } else {
        Text(text = displayedText, modifier = modifier, style = style)
    }
}
