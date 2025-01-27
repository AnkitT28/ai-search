package com.example.aichat.view

import android.Manifest
import android.annotation.SuppressLint
import android.content.ClipData
import android.content.ClipboardManager
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
import androidx.activity.compose.BackHandler
import androidx.appcompat.app.AppCompatActivity
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.automirrored.filled.VolumeOff
import androidx.compose.material.icons.automirrored.filled.VolumeUp
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.FocusRequester
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
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import com.example.aichat.R
import com.example.aichat.model.ChatMessage
import com.example.aichat.network.models.Navigation
import com.example.aichat.viewmodel.ChatViewModel
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import java.util.Locale

class MainActivity : AppCompatActivity() {

    private val chatViewModel: ChatViewModel by viewModels()

    private var tts: TextToSpeech? = null
    private var speechRecognizer: SpeechRecognizer? = null
    private lateinit var recognizerIntent: Intent
    private var isListening = mutableStateOf(false)

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

        // Set the status bar and navigation bar color
        window.statusBarColor = Color(0xFF1F002A).toArgb()
        window.navigationBarColor = Color(0xFF1F002A).toArgb()

        setContent {
            MainScreen(chatViewModel)
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
                        val data =
                            partialResults?.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION)
                        if (!data.isNullOrEmpty()) {
                            val partialSpeech = data[0]
                            chatViewModel.setLiveSpeechInput(partialSpeech) // Update via ViewModel
                            Log.d("SpeechRecognizer", "Partial: $partialSpeech")
                        }
                    }

                    override fun onResults(results: Bundle?) {
                        val data = results?.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION)
                        if (!data.isNullOrEmpty()) {
                            val finalSpeech = data[0]
                            chatViewModel.sendMessage(finalSpeech)
                            chatViewModel.clearRecentSearches() // Clear recent searches when sending a message
                            chatViewModel.setLiveSpeechInput(finalSpeech) // Set the search bar text to the recognized speech
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
                        chatViewModel.setLiveSpeechInput("") // Clear input via ViewModel
                        chatViewModel.clearRecentSearches() // Clear recent searches when sending a message
                        Log.e("SpeechRecognizer", "Error code: $error")
                        Toast.makeText(
                            this@MainActivity,
                            "Speech recognition error: $error",
                            Toast.LENGTH_SHORT
                        ).show()
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
            // Inform the user that speech recognition is unavailable
            Log.d("SpeechRecognizer", "Speech recognition not available on this device")
            Toast.makeText(
                this,
                "Speech Recognition is not available on this device",
                Toast.LENGTH_LONG
            ).show()
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

    private fun handleNavigationClick(link: String, context: Context) {
        when (link) {
            "/kundli-analysis" -> {
                val intent = Intent(context, MyPredictionActivity::class.java)
                context.startActivity(intent)
            }

            "/manglik-dosha" -> {
                val intent = Intent(context, MyPredictionActivity::class.java)
                context.startActivity(intent)
            }

            "/vimshottari-dasha" -> {
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

    @SuppressLint("InvalidColorHexValue")
    @Composable
    fun MainScreen(chatViewModel: ChatViewModel) {
        val isExpanded = remember { mutableStateOf(false) }
        val chatMessages by chatViewModel.chatMessages.collectAsState()
        val suggestions by chatViewModel.suggestions.collectAsState()
        val recentSearches by chatViewModel.recentSearches.collectAsState()
        val liveSpeechInput by chatViewModel.liveSpeechInput.collectAsState()

        val focusRequester = remember { FocusRequester() }
        val focusManager = androidx.compose.ui.platform.LocalFocusManager.current

        val coroutineScope = rememberCoroutineScope()

        // Function to reset suggestions via ViewModel
        fun resetSuggestions() {
            chatViewModel.resetSuggestions()
        }

        // Reset suggestions when the composable is first launched
        LaunchedEffect(Unit) {
            resetSuggestions()
        }

        // Reset suggestions every time the chat is expanded
        LaunchedEffect(isExpanded.value) {
            if (isExpanded.value) {
                resetSuggestions()
            }
        }

        // Handle device back press to collapse expanded mode and clear suggestions
        BackHandler(enabled = isExpanded.value) {
            isExpanded.value = false
            chatViewModel.clearMessages()
            chatViewModel.clearSuggestions()
            chatViewModel.clearRecentSearches()
            chatViewModel.setLiveSpeechInput("") // Clear live speech input
            focusManager.clearFocus()
            tts?.stop() // Stop the TTS when back is pressed
        }

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
                    // Greeting Section (only visible when not in expanded mode)
                    AnimatedVisibility(visible = !isExpanded.value) {
                        GreetingSection()
                    }

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
                        // Back Arrow (Visible only in expanded mode)
                        if (isExpanded.value) {
                            IconButton(
                                onClick = {
                                    isExpanded.value = false // Collapse on arrow click
                                    chatViewModel.clearMessages() // Clear chat messages
                                    chatViewModel.clearSuggestions() // Clear suggestions
                                    chatViewModel.clearRecentSearches() // Clear recent searches
                                    chatViewModel.setLiveSpeechInput("") // Clear live speech input
                                    focusManager.clearFocus() // Clear the focus of the text field
                                    tts?.stop() // Stop TTS
                                },
                                modifier = Modifier.size(24.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                                    contentDescription = "Back",
                                    tint = Color.White
                                )
                            }
                        } else {
                            Image(
                                painter = painterResource(id = R.drawable.search_star_icon),
                                contentDescription = "Star icon",
                                modifier = Modifier.size(28.dp)
                            )
                        }

                        Spacer(modifier = Modifier.width(12.dp))

                        // Input Field with white cursor using TextFieldValue
                        BasicTextField(
                            value = liveSpeechInput,
                            onValueChange = {
                                chatViewModel.setLiveSpeechInput(it.text) // Update via ViewModel
                            },
                            textStyle = TextStyle(color = Color.White, fontSize = 16.sp),
                            keyboardOptions = KeyboardOptions(
                                imeAction = ImeAction.Search
                            ),
                            keyboardActions = KeyboardActions(
                                onSearch = {
                                    if (liveSpeechInput.text.isNotBlank()) {
                                        chatViewModel.sendMessage(liveSpeechInput.text)
                                        // Do not clear the input to retain it
                                        isExpanded.value = true // Expand to show messages
                                    }
                                }
                            ),
                            cursorBrush = SolidColor(Color.White),
                            modifier = Modifier
                                .weight(1f)
                                .padding(4.dp)
                                .focusRequester(focusRequester)
                                .onFocusChanged { focusState ->
                                    isExpanded.value = focusState.isFocused // Update focus state
                                    if (focusState.isFocused && suggestions.isEmpty()) {
                                        chatViewModel.resetSuggestions() // Reset suggestions when focusing if they are empty
                                    }
                                },
                            decorationBox = { innerTextField ->
                                if (liveSpeechInput.text.isEmpty()) {
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
                        if (liveSpeechInput.text.isNotEmpty()) {
                            IconButton(
                                onClick = {
                                    chatViewModel.setLiveSpeechInput("") // Clear input via ViewModel
                                    chatViewModel.clearSuggestions() // Clear suggestions when cancelling input
                                    chatViewModel.clearRecentSearches() // Clear recent searches when cancelling input
                                },
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
                                    isExpanded.value = true
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

                    // Content Section: Show messages only when expanded
                    if (isExpanded.value) {
                        ExpandedContent(
                            chatMessages = chatMessages,
                            suggestions = suggestions,
                            recentSearches = recentSearches,
                            onSuggestionClick = { suggestion ->
                                chatViewModel.setLiveSpeechInput(suggestion) // Update search bar
                                chatViewModel.sendMessage(suggestion) // Send the message
                                isExpanded.value = true // Ensure UI is expanded
                            },
                            onNavigationClick = { navigation ->
                                handleNavigationClick(navigation.link, this@MainActivity)
                            }
                        )
                    }
                }
            }
        }
    }

    @Composable
    fun GreetingSection() {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = 40.dp),
            horizontalAlignment = Alignment.Start
        ) {
            Text(
                text = "Hi, Good Morning",
                color = Color.White,
                fontSize = 12.sp,
                style = MaterialTheme.typography.bodySmall
            )
            Text(
                text = "Ankit Tiwari",
                color = Color.White,
                fontSize = 20.sp,
                style = MaterialTheme.typography.bodyLarge.copy(fontWeight = FontWeight.Bold)
            )
        }
    }

    @Composable
    fun ExpandedContent(
        chatMessages: List<ChatMessage>,
        suggestions: List<String>,
        recentSearches: List<String>,
        onSuggestionClick: (String) -> Unit,
        onNavigationClick: (Navigation) -> Unit
    ) {
        LazyColumn(
            modifier = Modifier
                .fillMaxWidth()
                .fillMaxHeight() // Ensure it covers the available height
                .padding(8.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            // Conditionally add SuggestionsHeader and separator only if suggestions are not empty
            if (suggestions.isNotEmpty()) {
                // Add the suggestions as a header
                item {
                    SuggestionsHeader(
                        suggestions = suggestions,
                        onSuggestionClick = onSuggestionClick
                    )
                }

                // Add a separator between the suggestions and recent searches
                item {
                    Spacer(modifier = Modifier.height(16.dp))
                    HorizontalDivider(thickness = 1.dp, color = Color.Gray.copy(alpha = 0.5f))
                    Spacer(modifier = Modifier.height(16.dp))
                }
            }

            // Conditionally add Recent Searches Header and list
            if (recentSearches.isNotEmpty()) {
                // Recent Searches Header
                item {
                    Text(
                        text = "Recent Searches",
                        style = TextStyle(
                            color = Color.White,
                            fontSize = 16.sp,
                            fontWeight = FontWeight.SemiBold
                        ),
                        modifier = Modifier.padding(bottom = 8.dp, start = 6.dp)
                    )
                }

                // Recent Searches List
                items(recentSearches) { recentSearch ->
                    RecentSearchItem(recentSearch = recentSearch)
                }

                // Add a separator between recent searches and chat messages
                item {
                    Spacer(modifier = Modifier.height(16.dp))
                    HorizontalDivider(thickness = 1.dp, color = Color.Gray.copy(alpha = 0.5f))
                    Spacer(modifier = Modifier.height(16.dp))
                }
            }

            // Add the chat messages
            items(chatMessages) { message ->
                if (message.isUser) {
                    UserMessageBox(message.userMessage)
                } else {
                    AIResponseBox(
                        message = message.botResponse,
                        navigations = message.navigations,
                        onNavigationClick = onNavigationClick
                    )
                }
            }
        }
    }

    @Composable
    fun SuggestionsHeader(
        suggestions: List<String>,
        onSuggestionClick: (String) -> Unit
    ) {
        Column(modifier = Modifier.fillMaxWidth()) {
            // Header Text: "Suggestions" with font size 14sp
            Text(
                text = "Suggestions",
                style = TextStyle(
                    color = Color.White,
                    fontSize = 14.sp
                ),
                modifier = Modifier.padding(bottom = 8.dp, start = 6.dp)
            )

            // Iterate over the suggestions list
            suggestions.forEach { suggestion ->
                // Space between suggestion items
                Spacer(modifier = Modifier.height(8.dp))

                // Suggestion Item with background and clickable behavior
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(8.dp))
                        .background(Color(0xFF20FFFFFF)) // Semi-transparent background
                        .clickable {
                            onSuggestionClick(suggestion) // Handle suggestion click
                        }
                        .padding(horizontal = 16.dp, vertical = 8.dp) // Padding inside the box
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        // Suggestion Text with font size 14sp
                        Text(
                            text = suggestion,
                            style = TextStyle(
                                color = Color.White,
                                fontSize = 14.sp
                            )
                        )

                        Spacer(modifier = Modifier.width(8.dp)) // Space between text and arrow

                        // Arrow Icon rotated to 225 degrees
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowForward,
                            contentDescription = "Arrow",
                            tint = Color.White,
                            modifier = Modifier
                                .size(20.dp)
                                .graphicsLayer {
                                    rotationZ = 225f // Rotates the arrow icon
                                }
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(8.dp)) // Space after the last suggestion
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
        )
        {
            Text(
                text = "Search: $message",
                color = Color.Black,
                style = MaterialTheme.typography.bodyMedium
            )
        }
    }

    @SuppressLint("InvalidColorHexValue")
    @Composable
    fun AIResponseBox(
        message: String?,
        navigations: List<Navigation>?,
        onNavigationClick: (Navigation) -> Unit
    ) {
        val context = LocalContext.current
        var isSpeakerOn by remember { mutableStateOf(false) }
        var isMessageComplete by remember { mutableStateOf(false) } // State to track message completion

        val fallbackMessage = "I can't understand your request."

        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(start = 2.dp, bottom = 4.dp)
        ) {
            if (message.isNullOrBlank()) {
                // Display fallback message when the response is null or empty
                TypewriterText(
                    fullText = fallbackMessage,
                    modifier = Modifier.padding(bottom = 4.dp),
                    color = Color.White,
                    onComplete = { isMessageComplete = true } // Mark as complete
                )
            } else {
                // Row for the speaker toggle and copy button
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 14.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.End // Align buttons to the end (right)
                ) {
                    // Speaker Button with Icon, Background, and Text
                    Row(
                        modifier = Modifier
                            .background(
                                Color(0xFF20FFFFFF),
                                shape = RoundedCornerShape(8.dp)
                            ) // Background with rounded corners
                            .padding(horizontal = 16.dp, vertical = 8.dp) // Padding for the button
                            .clickable {
                                isSpeakerOn = !isSpeakerOn
                                if (isSpeakerOn) {
                                    // Start TTS with the response
                                    tts?.speak(message, TextToSpeech.QUEUE_FLUSH, null, null)
                                } else {
                                    // Stop TTS
                                    tts?.stop()
                                }
                            },
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = if (isSpeakerOn) Icons.AutoMirrored.Filled.VolumeUp else Icons.AutoMirrored.Filled.VolumeOff,
                            contentDescription = if (isSpeakerOn) "Speaker On" else "Speaker Off",
                            tint = Color.White,
                            modifier = Modifier.size(20.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "Listen",
                            style = MaterialTheme.typography.bodyMedium.copy(color = Color.White),
                            fontSize = 14.sp
                        )
                    }

                    Spacer(modifier = Modifier.width(16.dp)) // Space between buttons

                    // Copy Button with Icon, Background, and Text
                    Row(
                        modifier = Modifier
                            .background(
                                Color(0xFF20FFFFFF),
                                shape = RoundedCornerShape(8.dp)
                            ) // Background with rounded corners
                            .padding(horizontal = 16.dp, vertical = 8.dp) // Padding for the button
                            .clickable {
                                // Copy the message to clipboard
                                val clipboard =
                                    ContextCompat.getSystemService(
                                        context,
                                        ClipboardManager::class.java
                                    )
                                val clip = ClipData.newPlainText("AI Response", message)
                                clipboard?.setPrimaryClip(clip)

                                // Show a toast to inform the user
                                Toast
                                    .makeText(context, "Copied to clipboard!", Toast.LENGTH_SHORT)
                                    .show()
                            },
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            painter = painterResource(id = R.drawable.ic_copy_icon),
                            contentDescription = "Copy to Clipboard",
                            tint = Color.White,
                            modifier = Modifier.size(20.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "Copy",
                            style = MaterialTheme.typography.bodyMedium.copy(color = Color.White),
                            fontSize = 14.sp
                        )
                    }
                }

                Spacer(modifier = Modifier.height(8.dp))

                // Typewriter effect to display the AI Response
                TypewriterText(
                    fullText = message,
                    modifier = Modifier.padding(bottom = 4.dp),
                    color = Color.White,
                    onComplete = { isMessageComplete = true } // Mark as complete
                )
            }

            // Display navigation cards only if the message is complete
            if (isMessageComplete && !navigations.isNullOrEmpty()) {
                Spacer(modifier = Modifier.height(16.dp))
                LazyRow(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    items(navigations) { navigation ->
                        NavigationCard(navigation = navigation, onClick = {
                            onNavigationClick(navigation)
                        })
                    }
                }
            }
        }
    }

    @Composable
    fun NavigationCard(navigation: Navigation, onClick: () -> Unit) {
        Card(
            modifier = Modifier
                .width(220.dp)
                .fillMaxHeight() // Ensure the card height adapts to its content
                .clickable { onClick() },
            shape = RoundedCornerShape(16.dp),
            elevation = CardDefaults.cardElevation(defaultElevation = 0.dp),
            colors = CardDefaults.cardColors(containerColor = Color(0xFF20FFFFFF)) // Purple background color
        ) {
            Column(
                modifier = Modifier
                    .padding(16.dp),
                verticalArrangement = Arrangement.SpaceBetween
            ) {
                // Title
                Text(
                    text = navigation.title,
                    style = TextStyle(
                        color = Color.White,
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Bold
                    )
                )

                Spacer(modifier = Modifier.height(6.dp))

                // Description
                Text(
                    text = navigation.description,
                    style = TextStyle(
                        color = Color(0xFFCCCCCC), // Light gray text
                        fontSize = 14.sp,
                        lineHeight = 20.sp
                    ),
                    maxLines = 3,
                    overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis
                )

                Spacer(modifier = Modifier.height(6.dp))

                // Action Icon (Bottom Right)
                Box(
                    modifier = Modifier.fillMaxWidth(),
                    contentAlignment = Alignment.BottomEnd
                ) {
                    Icon(
                        painter = painterResource(id = R.drawable.ic_navigation_arrow_icon), // Replace with your arrow icon
                        contentDescription = "Navigate",
                        tint = Color(0xFFFFFFFF), // White color for the icon
                        modifier = Modifier.size(20.dp)
                    )
                }
            }
        }
    }

    @Composable
    fun getIconResource(iconName: String): Int {
        return when (iconName) {
            "moon_icon.png" -> R.drawable.search_star_icon
            "mars_icon.png" -> R.drawable.search_star_icon
            "jupiter_icon.png" -> R.drawable.search_star_icon
            else -> R.drawable.search_star_icon
        }
    }

    @Composable
    fun LoaderMessageBox(loaderMessage: String) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .padding(8.dp)
                .background(Color.LightGray, RoundedCornerShape(8.dp))
                .padding(12.dp)
        ) {
            Text(text = loaderMessage, color = Color.Black)
        }
    }

    @Composable
    fun RecentSearchItem(recentSearch: String) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier
                .fillMaxWidth()
                .clickable {
                    // Optional: Handle click on recent search (e.g., perform the search again)
                    // For example:
                    chatViewModel.sendMessage(recentSearch)
                    // isExpanded.value = true // Ensure UI is expanded
                }
                .padding(vertical = 4.dp)
        ) {
            // Recent Search Icon (Replace with your desired icon)
            Icon(
                imageVector = Icons.Default.Restore, // Using the Search icon as an example
                contentDescription = "Recent Search Icon",
                tint = Color.White,
                modifier = Modifier.size(20.dp)
            )

            Spacer(modifier = Modifier.width(8.dp))

            // Recent Search Text
            Text(
                text = recentSearch,
                style = TextStyle(
                    color = Color.White,
                    fontSize = 14.sp
                )
            )
        }
    }
}
