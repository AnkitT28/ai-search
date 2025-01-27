package com.example.aichat.view

import androidx.compose.runtime.*
import androidx.compose.material3.Text
import androidx.compose.ui.Modifier
import androidx.compose.foundation.clickable
import androidx.compose.ui.graphics.Color
import kotlinx.coroutines.delay
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.unit.sp

@Composable
fun TypewriterText(
    fullText: String,
    modifier: Modifier = Modifier,
    onClick: (() -> Unit)? = null,
    onAnimationComplete: (() -> Unit)? = null, // Callback for animation completion
    onComplete: (() -> Unit)? = null, // Additional onComplete callback
    style: TextStyle = TextStyle(
        fontSize = 16.sp,
        lineHeight = 28.sp,
        color = Color.White
    ),
    color: Color = Color.White // Default color is white (overrides TextStyle if provided)
) {
    var displayedText by remember { mutableStateOf("") }

    // Animate text appearance (character by character)
    LaunchedEffect(fullText) {
        displayedText = ""
        for (char in fullText) {
            displayedText += char
            delay(40) // Adjust speed as needed
        }
        onAnimationComplete?.invoke() // Notify animation completion
        onComplete?.invoke() // Notify additional onComplete callback
    }

    // If onClick is provided, wrap Text in clickable
    if (onClick != null) {
        Text(
            text = displayedText,
            modifier = modifier.clickable { onClick() },
            style = style.copy(color = color), // Merge style and color
            color = color
        )
    } else {
        Text(
            text = displayedText,
            modifier = modifier,
            style = style.copy(color = color), // Merge style and color
            color = color
        )
    }
}



