package com.example.aichat.view

import android.Manifest
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Bundle
import android.speech.RecognitionListener
import android.speech.RecognizerIntent
import android.speech.SpeechRecognizer
import android.speech.tts.TextToSpeech
import android.util.Log
import android.widget.Toast
import androidx.activity.compose.setContent
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.unit.dp
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import com.example.aichat.viewmodel.ChatViewModel
import java.util.Locale

class MainActivity : AppCompatActivity() {

    private val chatViewModel: ChatViewModel by viewModels()

    private var tts: TextToSpeech? = null
    private var speechRecognizer: SpeechRecognizer? = null
    private lateinit var recognizerIntent: Intent
    private var isListening = mutableStateOf(false)
    private var liveSpeechInput = mutableStateOf("") // Live speech input

    companion object {
        private const val PERMISSION_REQUEST_RECORD_AUDIO = 123
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // Check for RECORD_AUDIO permission
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.RECORD_AUDIO)
            != PackageManager.PERMISSION_GRANTED
        ) {
            ActivityCompat.requestPermissions(
                this, arrayOf(Manifest.permission.RECORD_AUDIO), PERMISSION_REQUEST_RECORD_AUDIO
            )
        } else {
            initSpeechRecognizer()
        }

        // Initialize TTS
        tts = TextToSpeech(this) { status ->
            if (status == TextToSpeech.SUCCESS) {
                tts?.language = Locale.US
            }
        }

        setContent {
            var showPopup by remember { mutableStateOf(true) }
            val chatMessages by chatViewModel.chatMessages.collectAsState()
            val listState = rememberLazyListState() // Manage LazyColumn scroll state

            Box(modifier = Modifier.fillMaxSize()) {
                AnimatedVisibility(
                    visible = showPopup,
                    enter = slideInVertically(initialOffsetY = { -it }, animationSpec = tween(400)),
                    exit = slideOutVertically(targetOffsetY = { -it }, animationSpec = tween(400))
                ) {
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
                            // Header
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(horizontal = 16.dp, vertical = 8.dp),
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Star,
                                    contentDescription = "Star",
                                    tint = Color(0xFF3E77FF),
                                    modifier = Modifier.size(24.dp)
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(
                                    text = "AI Search",
                                    style = MaterialTheme.typography.titleMedium,
                                    color = Color(0xFF3E77FF)
                                )
                            }

                            Spacer(modifier = Modifier.height(8.dp))

                            // Input Row with Mic and Live Speech
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clip(RoundedCornerShape(32.dp))
                                    .background(Color(0xFFF1F3F4))
                                    .padding(horizontal = 16.dp, vertical = 8.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Box(
                                    modifier = Modifier.size(56.dp),
                                    contentAlignment = Alignment.Center
                                ) {
                                    if (isListening.value) {
                                        CircularMicAnimation()
                                    }
                                    IconButton(onClick = { startSpeechRecognition() }) {
                                        Icon(
                                            imageVector = Icons.Default.Mic,
                                            contentDescription = "Voice",
                                            tint = Color.Gray,
                                            modifier = Modifier.size(24.dp)
                                        )
                                    }
                                }
                                Spacer(modifier = Modifier.width(8.dp))
                                // Input Field
                                BasicTextField(
                                    value = liveSpeechInput.value,
                                    onValueChange = { liveSpeechInput.value = it },
                                    textStyle = TextStyle(color = Color.Black),
                                    modifier = Modifier
                                        .weight(1f)
                                        .padding(4.dp),
                                    decorationBox = { innerTextField ->
                                        if (liveSpeechInput.value.isEmpty()) {
                                            Text(
                                                text = "Write your question here",
                                                style = TextStyle(color = Color.Gray)
                                            )
                                        }
                                        innerTextField()
                                    }
                                )

                                Spacer(modifier = Modifier.width(8.dp))
                                IconButton(onClick = {
                                    if (liveSpeechInput.value.isNotBlank()) {
                                        chatViewModel.sendMessage(liveSpeechInput.value)
                                        liveSpeechInput.value = ""
                                    }
                                }) {
                                    Icon(
                                        imageVector = Icons.Default.Send,
                                        contentDescription = "Send",
                                        tint = Color.Gray
                                    )
                                }
                            }

                            Spacer(modifier = Modifier.height(16.dp))

                            // Chat Messages
                            val chatMessages by chatViewModel.chatMessages.collectAsState() // Collect the list as state

                            LazyColumn(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .weight(1f)
                                    .padding(8.dp),
                                verticalArrangement = Arrangement.spacedBy(8.dp) // Adds spacing between items
                            ) {
                                items(chatMessages) { message -> // Correct usage of items with List<ChatMessage>
                                    if (message.isUser) {
                                        // User's message box
                                        Box(
                                            modifier = Modifier
                                                .fillMaxWidth()
                                                .background(
                                                    Color(0xFFE5D9F2),
                                                    RoundedCornerShape(8.dp)
                                                )
                                                .padding(8.dp)
                                        ) {
                                            Text(
                                                text = "You : ${message.userMessage}",
                                                color = Color.Black,
                                                style = MaterialTheme.typography.bodyMedium
                                            )
                                        }
                                    } else {
                                        // AI response box
                                        Column(
                                            modifier = Modifier
                                                .fillMaxWidth()
                                                .padding(start = 8.dp, bottom = 8.dp)
                                        ) {
                                            TypewriterText(
                                                fullText = "AI : ${message.botResponse}",
                                                modifier = Modifier.padding(bottom = 4.dp)
                                            )
                                            if (message.title.isNotBlank()) {
                                                Text(
                                                    text = "Know more",
                                                    color = Color.Blue,
                                                    modifier = Modifier.clickable {
                                                        handleLinkClick(
                                                            message.title,
                                                            applicationContext
                                                        )
                                                    }
                                                )
                                            }

                                            // Use TTS to read the bot response aloud
//                                            tts?.speak(
//                                                message.botResponse,
//                                                TextToSpeech.QUEUE_FLUSH,
//                                                null,
//                                                null
//
//
//                                            )
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    private fun initSpeechRecognizer() {
        speechRecognizer = SpeechRecognizer.createSpeechRecognizer(this).apply {
            setRecognitionListener(object : RecognitionListener {
                override fun onReadyForSpeech(params: Bundle?) {}
                override fun onBeginningOfSpeech() {
                    isListening.value = true
                }

                override fun onPartialResults(partialResults: Bundle?) {
                    val data =
                        partialResults?.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION)
                    if (!data.isNullOrEmpty()) {
                        val partialSpeech = data[0]
                        liveSpeechInput.value = partialSpeech
                    }
                }

                override fun onResults(results: Bundle?) {
                    val data = results?.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION)
                    if (!data.isNullOrEmpty()) {
                        val finalSpeech = data[0]
                        chatViewModel.sendMessage(finalSpeech)
                        liveSpeechInput.value = ""
                    }
                    isListening.value = false
                }

                override fun onEndOfSpeech() {
                    isListening.value = false
                }

                override fun onError(error: Int) {
                    isListening.value = false
                    liveSpeechInput.value = ""
                }

                override fun onBufferReceived(buffer: ByteArray?) {}
                override fun onRmsChanged(rmsdB: Float) {}
                override fun onEvent(eventType: Int, params: Bundle?) {}
            })
        }

        recognizerIntent = Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH).apply {
            putExtra(
                RecognizerIntent.EXTRA_LANGUAGE_MODEL,
                RecognizerIntent.LANGUAGE_MODEL_FREE_FORM
            )
            putExtra(RecognizerIntent.EXTRA_LANGUAGE, Locale.getDefault())
            putExtra(RecognizerIntent.EXTRA_PARTIAL_RESULTS, true)
        }
    }

    private fun startSpeechRecognition() {
        speechRecognizer?.startListening(recognizerIntent)
    }

    private fun stopSpeechRecognition() {
        speechRecognizer?.stopListening()
        isListening.value = false
    }

    override fun onDestroy() {
        super.onDestroy()
        tts?.shutdown()
        speechRecognizer?.destroy()
    }

    private fun handleLinkClick(link: String, context: Context) {
        when (link) {
            "Taro Prediction" -> {
                val intent = Intent(context, MyPredictionActivity::class.java)
                context.startActivity(intent)
            }

            else -> {
                Toast.makeText(context, "Unknown link: $link", Toast.LENGTH_SHORT).show()
            }
        }
    }

    @Composable
    fun CircularMicAnimation() {
        val infiniteTransition = rememberInfiniteTransition()
        val circleAlpha = infiniteTransition.animateFloat(
            initialValue = 0.3f,
            targetValue = 0f,
            animationSpec = infiniteRepeatable(
                animation = tween(durationMillis = 1500, easing = LinearEasing),
                repeatMode = RepeatMode.Restart
            ), label = ""
        )
        val circleRadius = infiniteTransition.animateFloat(
            initialValue = 0f,
            targetValue = 80f,
            animationSpec = infiniteRepeatable(
                animation = tween(durationMillis = 1500, easing = LinearEasing),
                repeatMode = RepeatMode.Restart
            ), label = ""
        )

        Box(
            modifier = Modifier.size(100.dp),
            contentAlignment = Alignment.Center
        ) {
            Canvas(modifier = Modifier.fillMaxSize()) {
                drawCircle(
                    color = Color.Blue.copy(alpha = circleAlpha.value),
                    radius = circleRadius.value
                )
            }
        }
    }
}
