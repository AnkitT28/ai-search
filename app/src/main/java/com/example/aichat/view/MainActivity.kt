package com.example.aichat.view

import android.content.Intent
import android.os.Bundle
import androidx.activity.compose.setContent
import androidx.appcompat.app.AppCompatActivity
import androidx.compose.foundation.layout.*
import androidx.compose.material3.Button
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp

class MainActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            MainScreen { navigateToAiChat() }
        }
    }

    private fun navigateToAiChat() {
        val intent = Intent(this, AiChatActivity::class.java)
        startActivity(intent)
    }
}

@Composable
fun MainScreen(onNavigateToAiChat: () -> Unit) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.Center
    ) {
        Button(onClick = onNavigateToAiChat, modifier = Modifier.fillMaxWidth()) {
            Text("Open AI Chat")
        }
    }
}
