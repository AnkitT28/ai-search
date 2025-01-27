package com.example.aichat.view

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.speech.RecognitionListener
import android.speech.RecognizerIntent
import android.speech.SpeechRecognizer
import android.util.Log
import android.view.WindowManager
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
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.aichat.R
import com.example.aichat.model.ChatMessage
import com.example.aichat.viewmodel.ChatViewModel
import java.util.Locale

class AiChatActivity : AppCompatActivity() {

    private val chatViewModel: ChatViewModel by viewModels()
    private var speechRecognizer: SpeechRecognizer? = null
    private lateinit var recognizerIntent: Intent
    private var isListening = mutableStateOf(false)
    private var liveSpeechInput = mutableStateOf("")

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        initSpeechRecognizer()
        // Make the activity fullscreen
        window.setFlags(
            WindowManager.LayoutParams.FLAG_LAYOUT_NO_LIMITS,
            WindowManager.LayoutParams.FLAG_LAYOUT_NO_LIMITS
        )

        // Set the status bar and navigation bar color
        window.statusBarColor = Color(0xFF1F002A).toArgb()
        window.navigationBarColor = Color(0xFF1F002A).toArgb()

        setContent {
            ChatScreen(chatViewModel)
        }
    }
    private fun initSpeechRecognizer() {
        if (SpeechRecognizer.isRecognitionAvailable(this)) {
            speechRecognizer = SpeechRecognizer.createSpeechRecognizer(this).apply {
                setRecognitionListener(object : RecognitionListener {
                    override fun onReadyForSpeech(params: Bundle?) {
                        Log.d("SpeechRecognizer", "Ready for speech")
                    }

                    override fun onBeginningOfSpeech() {
                        isListening.value = true
                        Log.d("SpeechRecognizer", "Beginning of speech")
                    }

                    override fun onPartialResults(partialResults: Bundle?) {
                        val data = partialResults?.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION)
                        if (!data.isNullOrEmpty()) {
                            val partialSpeech = data[0]
                            liveSpeechInput.value = partialSpeech
                            Log.d("SpeechRecognizer", "Partial: $partialSpeech")
                        }
                    }

                    override fun onResults(results: Bundle?) {
                        val data = results?.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION)
                        if (!data.isNullOrEmpty()) {
                            val finalSpeech = data[0]
                            chatViewModel.sendMessage(finalSpeech)
                            liveSpeechInput.value = ""
                            Log.d("SpeechRecognizer", "Final: $finalSpeech")
                        }
                        isListening.value = false
                    }

                    override fun onEndOfSpeech() {
                        isListening.value = false
                        Log.d("SpeechRecognizer", "End of speech")
                    }

                    override fun onError(error: Int) {
                        isListening.value = false
                        liveSpeechInput.value = ""
                        Log.e("SpeechRecognizer", "Error code: $error")
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
                putExtra(RecognizerIntent.EXTRA_MAX_RESULTS, 3)
            }
        } else {
            Log.d("SpeechRecognizer", "Speech recognition not available on this device")
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

    @Composable
    fun ChatScreen(chatViewModel: ChatViewModel) {
        val liveSpeechInput = remember { mutableStateOf("") }
        val chatMessages by chatViewModel.chatMessages.collectAsState()

        val focusRequester = remember { androidx.compose.ui.focus.FocusRequester() }
        val focusManager = androidx.compose.ui.platform.LocalFocusManager.current

        Box(
            modifier = Modifier
                .fillMaxSize()
        ) {
            // Background Image
            Image(
                painter = painterResource(id = R.drawable.main_background_img),
                contentDescription = null,
                modifier = Modifier.fillMaxSize(),
                contentScale = ContentScale.Crop
            )

            // Foreground Content
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(
                        Brush.verticalGradient(
                            colors = listOf(Color(0xFF1F002A), Color(0xFF1F002A))
                        )
                    )
                    .padding(16.dp)
            ) {
                Column(
                    modifier = Modifier.fillMaxSize(),
                    verticalArrangement = Arrangement.Top,
                    horizontalAlignment = Alignment.Start
                ) {
                    // Always show GreetingSection

                    Spacer(modifier = Modifier.height(24.dp))

                    // Search Bar with star image, input field, and mic icon
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(16.dp))
                            .background(Color(0xFF20FFFFFF))
                            .padding(horizontal = 16.dp, vertical = 12.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Image( // Always show the star icon
                            painter = painterResource(id = R.drawable.search_star_icon),
                            contentDescription = "Star icon",
                            modifier = Modifier.size(28.dp)
                        )

                        Spacer(modifier = Modifier.width(12.dp))

                        // Input Field with white cursor
                        BasicTextField(
                            value = liveSpeechInput.value,
                            onValueChange = { liveSpeechInput.value = it },
                            textStyle = TextStyle(color = Color.White, fontSize = 16.sp),
                            keyboardOptions = KeyboardOptions(
                                imeAction = ImeAction.Search
                            ),
                            keyboardActions = KeyboardActions(
                                onSearch = {
                                    if (liveSpeechInput.value.isNotBlank()) {
                                        chatViewModel.sendMessage(liveSpeechInput.value)
                                        liveSpeechInput.value = ""
                                    }
                                    focusManager.clearFocus() // Clear focus after sending message
                                }
                            ),
                            cursorBrush = SolidColor(Color.White),
                            modifier = Modifier
                                .weight(1f)
                                .padding(4.dp)
                                .focusRequester(focusRequester)
                                .onFocusChanged { focusState ->
                                    // No need to change isExpanded here
                                },
                            decorationBox = { innerTextField ->
                                if (liveSpeechInput.value.isEmpty()) {
                                    Text(
                                        text = "Ask the universe...",
                                        style = TextStyle(
                                            color = Color(0xFFAAAAAA),
                                            fontSize = 16.sp
                                        )
                                    )
                                }
                                innerTextField()
                            }
                        )

                        Spacer(modifier = Modifier.width(12.dp))

                        // Mic or Cancel Icon
                        if (liveSpeechInput.value.isNotEmpty()) {
                            IconButton(
                                onClick = { liveSpeechInput.value = "" },
                                modifier = Modifier.size(24.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Close,
                                    contentDescription = "Cancel Input",
                                    tint = Color(0xFFFFFFFF)
                                )
                            }
                        } else {
                            IconButton(
                                onClick = {
                                    startSpeechRecognition()
                                },
                                modifier = Modifier.size(24.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Mic,
                                    contentDescription = "Voice Input",
                                    tint = Color(0xFFFFFFFF)
                                )
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(24.dp))

                    // Content Section (Always Expanded)
                    ExpandedContent(chatMessages)
                }
            }
        }
    }

    @Composable
    fun ExpandedContent(chatMessages: List<ChatMessage>) {
        LazyColumn(
            modifier = Modifier
                .fillMaxWidth()
                .padding(8.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            // Add the suggestions as a header
            item {
                SuggestionsHeader()
            }

            // Add the chat messages (no separator needed)
            items(chatMessages) { message ->
                if (message.isUser) {
                    UserMessageBox(message.userMessage)
                } else {
                    AIResponseBox(message.botResponse)
                }
            }
        }
    }
    @Composable
    fun SuggestionsHeader() {
        Column(modifier = Modifier.fillMaxWidth()) {
            Text(
                text = "Suggestions",
                style = MaterialTheme.typography.titleMedium.copy(color = Color.White),
                modifier = Modifier.padding(bottom = 8.dp)
            )

            // List of suggestions
            val suggestions = listOf(
                "View my Kundli-based predictions",
                "Get daily insights from my birth chart",
                "When is the next full moon?",
                "Explore zodiac compatibility",
                "Generate a new Kundli"
            )

            suggestions.forEach { suggestion ->
                // Add space between items
                Spacer(modifier = Modifier.height(8.dp))

                // Text with background and arrow
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(8.dp))
                        .background(Color(0xFF9575CD))
                        .clickable {
                            // Handle suggestion click (e.g., send as query)
                        }
                        .padding(horizontal = 16.dp, vertical = 8.dp)
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        // Suggestion Text
                        Text(
                            text = suggestion,
                            style = TextStyle(color = Color.White, fontSize = 16.sp)
                        )

                        Spacer(modifier = Modifier.width(8.dp))

                        // Arrow Icon
                        Icon(
                            imageVector = Icons.Default.ArrowForward,
                            contentDescription = "Arrow",
                            tint = Color.White,
                            modifier = Modifier
                                .size(20.dp)
                                .graphicsLayer {
                                    rotationZ = 225f
                                }
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(8.dp))
        }
    }


    @Composable
    fun DefaultContent(chatMessages: List<ChatMessage>) {
        LazyColumn(
            modifier = Modifier
                .fillMaxWidth()
                .padding(8.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            items(chatMessages) { message ->
                if (message.isUser) {
                    UserMessageBox(message.userMessage)
                } else {
                    AIResponseBox(message.botResponse)
                }
            }
        }
    }

    @Composable
    fun UserMessageBox(message: String) {
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
                text = "Search: $message",
                color = Color.Black,
                style = MaterialTheme.typography.bodyMedium
            )
        }
    }

    @Composable
    fun AIResponseBox(message: String) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(start = 8.dp, bottom = 8.dp)
        ) {
            TypewriterText(
                fullText = message,
                modifier = Modifier.padding(bottom = 4.dp),
                color = Color.White
            )
        }
    }

}