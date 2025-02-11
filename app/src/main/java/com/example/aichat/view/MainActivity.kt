//package com.example.aichat.view
//
//import android.Manifest
//import android.annotation.SuppressLint
//import android.content.ClipData
//import android.content.ClipboardManager
//import android.content.Context
//import android.content.Intent
//import android.content.pm.PackageManager
//import android.os.Bundle
//import android.speech.RecognitionListener
//import android.speech.RecognizerIntent
//import android.speech.SpeechRecognizer
//import android.speech.tts.TextToSpeech
//import android.util.Log
//import android.widget.Toast
//import androidx.activity.compose.BackHandler
//import androidx.activity.compose.setContent
//import androidx.activity.viewModels
//import androidx.appcompat.app.AppCompatActivity
//import androidx.compose.animation.AnimatedVisibility
//import androidx.compose.animation.core.*
//import androidx.compose.foundation.Canvas
//import androidx.compose.foundation.Image
//import androidx.compose.foundation.background
//import androidx.compose.foundation.clickable
//import androidx.compose.foundation.layout.*
//import androidx.compose.foundation.lazy.LazyColumn
//import androidx.compose.foundation.lazy.LazyRow
//import androidx.compose.foundation.lazy.items
//import androidx.compose.foundation.shape.CircleShape
//import androidx.compose.foundation.shape.RoundedCornerShape
//import androidx.compose.foundation.text.BasicTextField
//import androidx.compose.foundation.text.KeyboardActions
//import androidx.compose.foundation.text.KeyboardOptions
//import androidx.compose.material.*
//import androidx.compose.material.icons.Icons
//import androidx.compose.material.icons.filled.Check
//import androidx.compose.material.icons.filled.Mic
//import androidx.compose.material.icons.automirrored.filled.ArrowBack
//import androidx.compose.material.icons.automirrored.filled.ArrowForward
//import androidx.compose.material.icons.automirrored.filled.VolumeOff
//import androidx.compose.material.icons.automirrored.filled.VolumeUp
//import androidx.compose.material.icons.filled.Close
//import androidx.compose.material.icons.filled.Restore
//import androidx.compose.runtime.*
//import androidx.compose.ui.*
//import androidx.compose.ui.draw.clip
//import androidx.compose.ui.focus.*
//import androidx.compose.ui.graphics.*
//import androidx.compose.ui.graphics.graphicsLayer
//import androidx.compose.ui.graphics.toArgb
//import androidx.compose.ui.layout.ContentScale
//import androidx.compose.ui.platform.*
//import androidx.compose.ui.res.painterResource
//import androidx.compose.ui.text.*
//import androidx.compose.ui.text.font.FontWeight
//import androidx.compose.ui.text.input.ImeAction
//import androidx.compose.ui.unit.*
//import androidx.core.app.ActivityCompat
//import androidx.core.content.ContextCompat
//import com.example.aichat.R
//import com.example.aichat.model.ChatMessage
//import com.example.aichat.network.models.Navigation
//import com.example.aichat.viewmodel.ChatViewModel
//import kotlinx.coroutines.delay
//import kotlinx.coroutines.launch
//import java.util.Locale
//
//@OptIn(ExperimentalMaterialApi::class)
//class MainActivity : AppCompatActivity() {
//
//    // ViewModel
//    private val chatViewModel: ChatViewModel by viewModels()
//
//    // Speech
//    private var tts: TextToSpeech? = null
//    private var speechRecognizer: SpeechRecognizer? = null
//    private lateinit var recognizerIntent: Intent
//
//    // Mutable states for speech recognition
//    private var isListening = mutableStateOf(false)  // true if actively capturing audio
//    private var partialSpeech by mutableStateOf("")  // partial results
//    private var recognizedText by mutableStateOf("") // final text
//
//    companion object {
//        private const val PERMISSION_REQUEST_RECORD_AUDIO = 123
//    }
//
//    override fun onCreate(savedInstanceState: Bundle?) {
//        super.onCreate(savedInstanceState)
//
//        // Request Mic Permission
//        if (ContextCompat.checkSelfPermission(this, Manifest.permission.RECORD_AUDIO)
//            != PackageManager.PERMISSION_GRANTED
//        ) {
//            ActivityCompat.requestPermissions(
//                this, arrayOf(Manifest.permission.RECORD_AUDIO), PERMISSION_REQUEST_RECORD_AUDIO
//            )
//        } else {
//            initSpeechRecognizer()
//        }
//
//        // Initialize TTS
//        tts = TextToSpeech(this) { status ->
//            if (status == TextToSpeech.SUCCESS) {
//                tts?.language = Locale.US
//            }
//        }
//
//        // Set system bar colors
//        window.statusBarColor = Color(0xFF1F002A).toArgb()
//        window.navigationBarColor = Color(0xFF1F002A).toArgb()
//
//        setContent {
//            MainScreen(chatViewModel)
//        }
//    }
//
//    /**
//     * Prepare the SpeechRecognizer with a basic RecognitionListener.
//     */
//    private fun initSpeechRecognizer() {
//        if (SpeechRecognizer.isRecognitionAvailable(this)) {
//            speechRecognizer = SpeechRecognizer.createSpeechRecognizer(this).apply {
//                setRecognitionListener(object : RecognitionListener {
//                    override fun onReadyForSpeech(params: Bundle?) {
//                        Log.d("SpeechRecognizer", "Ready for speech")
//                    }
//
//                    override fun onBeginningOfSpeech() {
//                        isListening.value = true
//                        Log.d("SpeechRecognizer", "Beginning of speech")
//                    }
//
//                    override fun onPartialResults(partialResults: Bundle?) {
//                        val data =
//                            partialResults?.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION)
//                        if (!data.isNullOrEmpty()) {
//                            partialSpeech = data[0]
//                            Log.d("SpeechRecognizer", "Partial: $partialSpeech")
//                        }
//                    }
//
//                    override fun onResults(results: Bundle?) {
//                        val data = results?.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION)
//                        if (!data.isNullOrEmpty()) {
//                            recognizedText = data[0]
//                            // Send recognized text to ViewModel for API call
//
//                            // Clear old responses, then send the new question
//                            chatViewModel.clearMessages()
//                            chatViewModel.setLiveSpeechInput(recognizedText)
//                            chatViewModel.sendMessage(recognizedText)
//                            Log.d("SpeechRecognizer", "Final: $recognizedText")
//                        }
//                        isListening.value = false
//                        partialSpeech = ""
//                    }
//
//                    override fun onEndOfSpeech() {
//                        isListening.value = false
//                        Log.d("SpeechRecognizer", "End of speech")
//                    }
//
//                    override fun onError(error: Int) {
//                        Log.e("SpeechRecognizer", "Error code: $error")
//                        isListening.value = false
//                        partialSpeech = ""
//                    }
//
//                    override fun onBufferReceived(buffer: ByteArray?) {}
//                    override fun onRmsChanged(rmsdB: Float) {}
//                    override fun onEvent(eventType: Int, params: Bundle?) {}
//                })
//            }
//
//            recognizerIntent = Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH).apply {
//                putExtra(
//                    RecognizerIntent.EXTRA_LANGUAGE_MODEL,
//                    RecognizerIntent.LANGUAGE_MODEL_FREE_FORM
//                )
//                putExtra(RecognizerIntent.EXTRA_LANGUAGE, Locale.getDefault())
//                putExtra(RecognizerIntent.EXTRA_PARTIAL_RESULTS, true)
//                putExtra(RecognizerIntent.EXTRA_MAX_RESULTS, 3)
//            }
//        } else {
//            Toast.makeText(
//                this,
//                "Speech Recognition not available on this device",
//                Toast.LENGTH_LONG
//            ).show()
//        }
//    }
//
//    private fun startSpeechRecognition() {
//        speechRecognizer?.startListening(recognizerIntent)
//    }
//
//    private fun stopSpeechRecognition() {
//        speechRecognizer?.stopListening()
//        isListening.value = false
//    }
//
//    override fun onDestroy() {
//        super.onDestroy()
//        tts?.shutdown()
//        speechRecognizer?.destroy()
//    }
//
//    /**
//     * Cancel or Stop an in-flight streaming call if user navigates away
//     * You can call chatViewModel.cancelStreaming() in onPause or in back handler.
//     */
//    override fun onPause() {
//        super.onPause()
//        chatViewModel.cancelStreaming()
//    }
//
//    /**
//     * Main entry composable: sets up a bottom sheet for voice input + main content
//     */
//    @SuppressLint("InvalidColorHexValue")
//    @Composable
//    fun MainScreen(chatViewModel: ChatViewModel) {
//        val sheetState = rememberModalBottomSheetState(initialValue = ModalBottomSheetValue.Hidden)
//        val coroutineScope = rememberCoroutineScope()
//
//        ModalBottomSheetLayout(
//            sheetState = sheetState,
//            sheetShape = RoundedCornerShape(topStart = 24.dp, topEnd = 24.dp),
//            sheetBackgroundColor = Color(0xFF2B0142),
//            scrimColor = Color.Black.copy(alpha = 0.5f),
//            sheetContent = {
//                ListeningBottomSheetContent(
//                    partialText = partialSpeech,
//                    isListening = isListening.value,
//                    onStartListening = {
//                        // Called when the sheet wants to force listening mode
//                        startSpeechRecognition()
//                    },
//                    onCloseSheet = {
//                        coroutineScope.launch { sheetState.hide() }
//                    }
//                )
//            }
//        ) {
//            MainScreenContent(chatViewModel, sheetState)
//        }
//    }
//
//    /**
//     * The bottom sheet with auto-listening logic + 6s inactivity timer
//     */
//    @Composable
//    fun ListeningBottomSheetContent(
//        partialText: String,
//        isListening: Boolean,
//        onStartListening: () -> Unit,
//        onCloseSheet: () -> Unit
//    ) {
//        // "Idle", "Listening", "Completed"
//        val listeningState = remember { mutableStateOf("Idle") }
//
//        // If we open the sheet => automatically go to Listening
//        // as per your requirement
//        LaunchedEffect(Unit) {
//            listeningState.value = "Listening"
//            onStartListening()
//        }
//
//        // 6-second inactivity timer
//        // If we are in "Listening" state and partialText doesn't change => revert to "Idle"
//        var lastPartialUpdate by remember { mutableLongStateOf(System.currentTimeMillis()) }
//        val idleTimeoutMs = 6000L
//
//        // Watch partialText changes
//        LaunchedEffect(partialText) {
//            if (listeningState.value == "Listening") {
//                // user said something => update last partial time
//                lastPartialUpdate = System.currentTimeMillis()
//            }
//        }
//
//        // Check idle every 1 second
//        LaunchedEffect(isListening, partialText, listeningState.value) {
//            while (listeningState.value == "Listening") {
//                delay(1000)
//                val elapsed = System.currentTimeMillis() - lastPartialUpdate
//                if (elapsed > idleTimeoutMs) {
//                    // No speech for 6s => switch to idle
//                    listeningState.value = "Idle"
//                    stopSpeechRecognition() // stop listening
//                    break
//                }
//            }
//        }
//
//        // If final results have arrived => go "Completed" => then auto-close
//        // We'll do that in a side effect
//        LaunchedEffect(isListening) {
//            // If we were listening, but now the system has stopped => user done speaking
//            if (!isListening && listeningState.value == "Listening") {
//                listeningState.value = "Completed"
//                // show check mark for 1s, then close
//                delay(1500)
//                onCloseSheet()
//                // reset state to idle for next time
//                listeningState.value = "Idle"
//            }
//        }
//
//        // UI animation for "Listening"
//        val infiniteTransition = rememberInfiniteTransition()
//        val animatedAlpha = infiniteTransition.animateFloat(
//            initialValue = 0.5f,
//            targetValue = 1f,
//            animationSpec = infiniteRepeatable(
//                animation = tween(500, easing = LinearEasing),
//                repeatMode = RepeatMode.Reverse
//            )
//        )
//
//        Column(
//            modifier = Modifier
//                .fillMaxWidth()
//                .padding(24.dp),
//            horizontalAlignment = Alignment.CenterHorizontally
//        ) {
//            Text(
//                text = when (listeningState.value) {
//                    "Idle" -> "Tap Microphone to Speak"
//                    "Listening" -> "Listening...."
//                    "Completed" -> "Searching"
//                    else -> ""
//                },
//                color = Color.LightGray,
//                style = MaterialTheme.typography.body2.copy(fontSize = 14.sp)
//            )
//
//            Spacer(modifier = Modifier.height(16.dp))
//
//            Text(
//                text = when (listeningState.value) {
//                    "Listening" -> partialText
//                    "Completed" -> recognizedText
//                    else -> ""
//                },
//                color = Color.White,
//                style = MaterialTheme.typography.h5.copy(fontWeight = FontWeight.Bold),
//                maxLines = 2
//            )
//
//            Spacer(modifier = Modifier.height(24.dp))
//
//            Box(
//                modifier = Modifier
//                    .size(80.dp)
//                    .clip(CircleShape)
//                    .background(
//                        when (listeningState.value) {
//                            "Idle" -> Color(0xFFCE42F5)
//                            "Listening" -> Color(0xFF42A5F5)
//                            "Completed" -> Color(0xFF66BB6A)
//                            else -> Color.Gray
//                        }
//                    )
//                    .clickable {
//                        when (listeningState.value) {
//                            "Idle" -> {
//                                // user taps the mic => start listening
//                                listeningState.value = "Listening"
//                                partialSpeech = ""
//                                lastPartialUpdate = System.currentTimeMillis()
//                                onStartListening()
//                            }
//
//                            "Listening" -> {
//                                // optional: user taps => you can stop if desired
//
//                            }
//
//                            "Completed" -> {
//                                listeningState.value = "Idle"
//                                onCloseSheet()
//                            }
//                        }
//                    },
//                contentAlignment = Alignment.Center
//            ) {
//                when (listeningState.value) {
//                    "Idle" -> Icon(
//                        imageVector = Icons.Default.Mic,
//                        contentDescription = "Mic Icon",
//                        tint = Color.White,
//                        modifier = Modifier.size(32.dp)
//                    )
//
//                    "Listening" -> Canvas(
//                        modifier = Modifier
//                            .size(60.dp)
//                            .graphicsLayer { alpha = animatedAlpha.value }
//                    ) {
//                        drawCircle(color = Color.White, alpha = 0.3f)
//                        drawCircle(color = Color.White, radius = size.minDimension / 2.5f)
//                    }
//
//                    "Completed" -> Icon(
//                        imageVector = Icons.Default.Check,
//                        contentDescription = "Check Icon",
//                        tint = Color.White,
//                        modifier = Modifier.size(32.dp)
//                    )
//                }
//            }
//        }
//    }
//
//    /**
//     * Handle navigation clicks
//     */
//    private fun handleNavigationClick(link: String, context: Context) {
//        when (link) {
//            "/kundli-analysis" -> {
//                val intent = Intent(context, MyPredictionActivity::class.java)
//                context.startActivity(intent)
//            }
//
//            "/moon-sign-impact" -> {
//                val intent = Intent(context, MyPredictionActivity::class.java)
//                context.startActivity(intent)
//            }
//
//            "/moon-sign-career" -> {
//                val intent = Intent(context, MyPredictionActivity::class.java)
//                context.startActivity(intent)
//            }
//
//            "/daily-predictions" -> {
//                val intent = Intent(context, MyPredictionActivity::class.java)
//                context.startActivity(intent)
//            }
//
//            else -> {
//                Toast.makeText(context, "Unknown link: $link", Toast.LENGTH_SHORT).show()
//            }
//        }
//    }
//
//
//    /**
//     * Main content that shows the search bar, list of responses, etc.
//     */
//    @Composable
//    fun MainScreenContent(
//        chatViewModel: ChatViewModel,
//        sheetState: ModalBottomSheetState
//    ) {
//        val isExpanded = remember { mutableStateOf(false) }
//
//        val chatMessages by chatViewModel.chatMessages.collectAsState()
//        val suggestions by chatViewModel.suggestions.collectAsState()
//        val recentSearches by chatViewModel.recentSearches.collectAsState()
//        val liveSpeechInput by chatViewModel.liveSpeechInput.collectAsState()
//
//        val focusRequester = remember { FocusRequester() }
//        val focusManager = LocalFocusManager.current
//        val coroutineScope = rememberCoroutineScope()
//
//        // Reset suggestions when first launched
//        LaunchedEffect(Unit) {
//            chatViewModel.suggestions
//        }
//
//        // Reset suggestions each time we expand
//        LaunchedEffect(isExpanded.value) {
//            if (isExpanded.value) {
//                chatViewModel.clearSuggestions()
//            }
//        }
//
//        // Handle back press => close + cancel streaming
//        BackHandler(enabled = isExpanded.value) {
//            isExpanded.value = false
//            chatViewModel.clearMessages()
//            chatViewModel.clearSuggestions()
//            chatViewModel.setLiveSpeechInput("")
//            focusManager.clearFocus()
//            tts?.stop()
//            chatViewModel.cancelStreaming()
//        }
//
//        Box(Modifier.fillMaxSize()) {
//            // Background
//            Image(
//                painter = painterResource(id = R.drawable.main_background_img),
//                contentDescription = null,
//                modifier = Modifier.fillMaxSize(),
//                contentScale = ContentScale.Crop
//            )
//
//            // Foreground gradient + layout
//            Box(
//                modifier = Modifier
//                    .fillMaxSize()
//                    .background(
//                        Brush.verticalGradient(
//                            listOf(
//                                Color(0xFF1F002A),
//                                Color(0xFF1F002A)
//                            )
//                        )
//                    )
//                    .padding(8.dp)
//            ) {
//                Column(
//                    modifier = Modifier
//                        .fillMaxSize()
//                        .padding(horizontal = 8.dp),
//                    verticalArrangement = Arrangement.Top,
//                    horizontalAlignment = Alignment.Start
//                ) {
//                    // Greeting (visible only if not expanded)
//                    AnimatedVisibility(visible = !isExpanded.value) {
//                        GreetingSection()
//                    }
//
//                    Spacer(modifier = Modifier.height(8.dp))
//
//                    // Search Bar
//                    Row(
//                        modifier = Modifier
//                            .fillMaxWidth()
//                            .clip(RoundedCornerShape(8.dp))
//                            .background(Color(0xFF20FFFFFF))
//                            .padding(horizontal = 16.dp, vertical = 12.dp),
//                        verticalAlignment = Alignment.CenterVertically
//                    ) {
//                        if (isExpanded.value) {
//                            IconButton(
//                                onClick = {
//                                    isExpanded.value = false
//                                    chatViewModel.clearMessages()
//                                    chatViewModel.clearSuggestions()
//                                    chatViewModel.setLiveSpeechInput("")
//                                    focusManager.clearFocus()
//                                    tts?.stop()
//                                },
//                                modifier = Modifier.size(24.dp)
//                            ) {
//                                Icon(
//                                    imageVector = Icons.AutoMirrored.Filled.ArrowBack,
//                                    contentDescription = "Back",
//                                    tint = Color.White
//                                )
//                            }
//                        } else {
//                            Image(
//                                painter = painterResource(id = R.drawable.search_star_icon),
//                                contentDescription = "Star icon",
//                                modifier = Modifier.size(28.dp)
//                            )
//                        }
//
//                        Spacer(modifier = Modifier.width(12.dp))
//
//                        // TextField
//                        BasicTextField(
//                            value = liveSpeechInput,
//                            onValueChange = { chatViewModel.setLiveSpeechInput(it.text) },
//                            textStyle = TextStyle(color = Color.White, fontSize = 16.sp),
//                            keyboardOptions = KeyboardOptions(imeAction = ImeAction.Search),
//                            keyboardActions = KeyboardActions(
//                                onSearch = {
//                                    if (liveSpeechInput.text.isNotBlank()) {
//                                        //clear all previous messages when start new search
//                                        chatViewModel.clearMessages()
//                                        chatViewModel.sendMessage(liveSpeechInput.text)
//                                        isExpanded.value = true
//                                    }
//                                }
//                            ),
//                            cursorBrush = SolidColor(Color.White),
//                            modifier = Modifier
//                                .weight(1f)
//                                .padding(4.dp)
//                                .focusRequester(focusRequester)
//                                .onFocusChanged { focusState ->
//                                    isExpanded.value = focusState.isFocused
//                                    if (focusState.isFocused && suggestions.isEmpty()) {
//                                        chatViewModel.clearSuggestions()
//                                    }
//                                },
//                            decorationBox = { innerTextField ->
//                                if (liveSpeechInput.text.isEmpty()) {
//                                    Text(
//                                        text = "Ask the universe...",
//                                        style = TextStyle(
//                                            color = Color(0xFFAAAAAA),
//                                            fontSize = 16.sp
//                                        )
//                                    )
//                                }
//                                innerTextField()
//                            }
//                        )
//
//                        Spacer(modifier = Modifier.width(12.dp))
//
//                        // If text is typed => show X, else show mic
//                        if (liveSpeechInput.text.isNotEmpty()) {
//                            IconButton(
//                                onClick = {
//                                    chatViewModel.setLiveSpeechInput("")
//                                    chatViewModel.clearSuggestions()
//                                    chatViewModel.clearRecentSearches()
//                                },
//                                modifier = Modifier.size(24.dp)
//                            ) {
//                                Icon(
//                                    imageVector = Icons.Default.Close,
//                                    contentDescription = "Cancel Input",
//                                    tint = Color.White
//                                )
//                            }
//                        } else {
//                            // Mic => open bottom sheet
//                            IconButton(
//                                onClick = {
//                                    isExpanded.value = true
//                                    recognizedText = ""
//                                    partialSpeech = ""
//                                    // show bottom sheet
//                                    coroutineScope.launch { sheetState.show() }
//                                },
//                                modifier = Modifier.size(24.dp)
//                            ) {
//                                Icon(
//                                    imageVector = Icons.Default.Mic,
//                                    contentDescription = "Voice Input",
//                                    tint = Color.White
//                                )
//                            }
//                        }
//                    }
//
//                    Spacer(modifier = Modifier.height(24.dp))
//
//                    // If expanded => show chat content
//                    if (isExpanded.value) {
//                        ExpandedContent(
//                            chatMessages = chatViewModel.chatMessages.collectAsState().value,
//                            suggestions = suggestions,
//                            recentSearches = recentSearches,
//                            onSuggestionClick = { suggestion ->
//                                chatViewModel.setLiveSpeechInput(suggestion)
//                                chatViewModel.sendMessage(suggestion)
//                                isExpanded.value = true
//                            },
//                            onNavigationClick = { navigation ->
//                                handleNavigationClick(navigation.link, this@MainActivity)
//                            }
//                        )
//                    }
//                }
//            }
//        }
//    }
//
//
//    @Composable
//    fun GreetingSection() {
//        Column(
//            modifier = Modifier
//                .fillMaxWidth()
//                .padding(top = 40.dp, bottom = 16.dp),
//            horizontalAlignment = Alignment.Start
//        ) {
//            Text(
//                text = "Hi, Good Morning",
//                color = Color.White,
//                fontSize = 12.sp,
//                style = MaterialTheme.typography.body2
//            )
//            Text(
//                text = "Ankit Tiwari",
//                color = Color.White,
//                fontSize = 20.sp,
//                style = MaterialTheme.typography.body1.copy(fontWeight = FontWeight.Bold)
//            )
//        }
//    }
//
//    /**
//     * Expanded Content: we ONLY show AI responses
//     * (We skip user messages to hide user’s question text in the UI.)
//     */
//    @Composable
//    fun ExpandedContent(
//        chatMessages: List<ChatMessage>,
//        suggestions: List<String>,
//        recentSearches: List<String>,
//        onSuggestionClick: (String) -> Unit,
//        onNavigationClick: (Navigation) -> Unit
//    ) {
//        LazyColumn(
//            modifier = Modifier
//                .fillMaxSize()
//                .padding(8.dp),
//            verticalArrangement = Arrangement.spacedBy(8.dp)
//        ) {
//            // Suggestions
//            if (suggestions.isNotEmpty()) {
//                item {
//                    SuggestionsHeader(suggestions, onSuggestionClick)
//                }
//
//                item {
//                    Spacer(modifier = Modifier.height(16.dp))
//                    Divider(thickness = 1.dp, color = Color.Gray.copy(alpha = 0.5f))
//                    Spacer(modifier = Modifier.height(16.dp))
//                }
//
//                // Recent searches
//                if (recentSearches.isNotEmpty()) {
//                    item {
//                        Text(
//                            text = "Recent Searches",
//                            style = TextStyle(
//                                color = Color.White,
//                                fontSize = 16.sp,
//                                fontWeight = FontWeight.SemiBold
//                            ),
//                            modifier = Modifier.padding(bottom = 8.dp)
//                        )
//                    }
//
//                    items(recentSearches) { recentSearch ->
//                        Row(
//                            modifier = Modifier
//                                .fillMaxWidth()
//                                .clickable { onSuggestionClick(recentSearch) }
//                                .padding(vertical = 4.dp),
//                            verticalAlignment = Alignment.CenterVertically
//                        ) {
//                            Icon(
//                                imageVector = Icons.Default.Restore,
//                                contentDescription = "Recent Search",
//                                tint = Color.White,
//                                modifier = Modifier.size(20.dp)
//                            )
//                            Spacer(modifier = Modifier.width(8.dp))
//                            Text(
//                                text = recentSearch,
//                                style = TextStyle(color = Color.White, fontSize = 14.sp)
//                            )
//                        }
//                    }
//
//                    item {
//                        Spacer(modifier = Modifier.height(16.dp))
//                        Divider(thickness = 1.dp, color = Color.Gray.copy(alpha = 0.5f))
//                        Spacer(modifier = Modifier.height(16.dp))
//                    }
//                }
//            }
//
//            // Show AI responses only
//            items(chatMessages.filter { !it.isUser }) { message ->
//                AIResponseBox(
//                    message = message.botResponse,
//                    navigations = message.navigations,
//                    isLoader = message.isLoader,
//                    onNavigationClick = onNavigationClick
//                )
//            }
//        }
//    }
//
//    @Composable
//    fun SuggestionsHeader(
//        suggestions: List<String>,
//        onSuggestionClick: (String) -> Unit
//    ) {
//        Column(Modifier.fillMaxWidth()) {
//            Text(
//                text = "Suggestions",
//                style = TextStyle(color = Color.White, fontSize = 14.sp),
//                modifier = Modifier.padding(bottom = 8.dp)
//            )
//
//            suggestions.forEach { suggestion ->
//                Spacer(modifier = Modifier.height(8.dp))
//                Box(
//                    modifier = Modifier
//                        .clip(RoundedCornerShape(4.dp))
//                        .background(Color(0xFF20FFFFFF))
//                        .clickable { onSuggestionClick(suggestion) }
//                        .padding(horizontal = 16.dp, vertical = 8.dp)
//                ) {
//                    Row(verticalAlignment = Alignment.CenterVertically) {
//                        Text(suggestion, style = TextStyle(color = Color.White, fontSize = 14.sp))
//                        Spacer(modifier = Modifier.width(8.dp))
//                        Icon(
//                            imageVector = Icons.AutoMirrored.Filled.ArrowForward,
//                            contentDescription = "Arrow",
//                            tint = Color.White,
//                            modifier = Modifier
//                                .size(20.dp)
//                                .graphicsLayer { rotationZ = 225f }
//                        )
//                    }
//                }
//            }
//            Spacer(modifier = Modifier.height(8.dp))
//        }
//    }
//
//    /**
//     * Show final or partial AI responses, plus “Listen” and “Copy” options
//     */
//    @SuppressLint("InvalidColorHexValue")
//    @Composable
//    fun AIResponseBox(
//        message: String?,
//        navigations: List<Navigation>?,
//        isLoader: Boolean,
//        onNavigationClick: (Navigation) -> Unit
//    ) {
//        val context = LocalContext.current
//        val safeMessage = message ?: "No message available."
//        var isMessageComplete by remember { mutableStateOf(false) }
//        var isSpeakerOn by remember { mutableStateOf(false) }
//
//        // local TTS
//        var localTTS: TextToSpeech? = remember {
//            TextToSpeech(context) { status ->
//                if (status == TextToSpeech.SUCCESS) {
//                    tts?.language = Locale.US
//                }
//            }
//        }
//        DisposableEffect(Unit) {
//            onDispose { localTTS?.shutdown() }
//        }
//
//        Column(
//            Modifier
//                .fillMaxWidth()
//                .padding(8.dp)
//        ) {
//            if (isLoader) {
//                // partial text
//                Row(verticalAlignment = Alignment.CenterVertically) {
//                    CircularProgressIndicator(
//                        color = Color.White,
//                        modifier = Modifier.size(20.dp),
//                        strokeWidth = 2.dp
//                    )
//                    Spacer(modifier = Modifier.width(8.dp))
//                    Text(safeMessage, color = Color.White, style = MaterialTheme.typography.body1)
//                }
//            } else {
//                // final chunk
//                Row(
//                    modifier = Modifier
//                        .fillMaxWidth()
//                        .padding(vertical = 14.dp),
//                    horizontalArrangement = Arrangement.End,
//                    verticalAlignment = Alignment.CenterVertically
//                ) {
//                    Row(
//                        modifier = Modifier
//                            .background(Color(0xFF20FFFFFF), RoundedCornerShape(8.dp))
//                            .padding(horizontal = 16.dp, vertical = 8.dp)
//                            .clickable {
//                                isSpeakerOn = !isSpeakerOn
//                                if (isSpeakerOn) {
//                                    localTTS?.speak(
//                                        safeMessage,
//                                        TextToSpeech.QUEUE_FLUSH,
//                                        null,
//                                        null
//                                    )
//                                } else {
//                                    localTTS?.stop()
//                                }
//                            },
//                        verticalAlignment = Alignment.CenterVertically
//                    ) {
//                        Icon(
//                            imageVector = if (isSpeakerOn) Icons.AutoMirrored.Filled.VolumeUp
//                            else Icons.AutoMirrored.Filled.VolumeOff,
//                            contentDescription = null,
//                            tint = Color.White,
//                            modifier = Modifier.size(20.dp)
//                        )
//                        Spacer(modifier = Modifier.width(8.dp))
//                        Text(
//                            "Listen",
//                            style = MaterialTheme.typography.body2.copy(color = Color.White),
//                            fontSize = 14.sp
//                        )
//                    }
//
//                    Spacer(modifier = Modifier.width(16.dp))
//
//                    Row(
//                        modifier = Modifier
//                            .background(Color(0xFF20FFFFFF), RoundedCornerShape(8.dp))
//                            .padding(horizontal = 16.dp, vertical = 8.dp)
//                            .clickable {
//                                val clipboard = ContextCompat.getSystemService(
//                                    context,
//                                    ClipboardManager::class.java
//                                )
//                                val clip = ClipData.newPlainText("AI Response", safeMessage)
//                                clipboard?.setPrimaryClip(clip)
//                                Toast
//                                    .makeText(context, "Copied to clipboard!", Toast.LENGTH_SHORT)
//                                    .show()
//                            },
//                        verticalAlignment = Alignment.CenterVertically
//                    ) {
//                        Icon(
//                            painter = painterResource(id = R.drawable.ic_copy_icon),
//                            contentDescription = "Copy",
//                            tint = Color.White,
//                            modifier = Modifier.size(20.dp)
//                        )
//                        Spacer(modifier = Modifier.width(8.dp))
//                        Text(
//                            "Copy",
//                            style = MaterialTheme.typography.body2.copy(color = Color.White),
//                            fontSize = 14.sp
//                        )
//                    }
//                }
//
//                Spacer(modifier = Modifier.height(8.dp))
//
//                // Typewriter effect or just display text
//                TypewriterText(
//                    fullText = safeMessage,
//                    color = Color.White,
//                    onComplete = { isMessageComplete = true }
//                )
//
//                if (isMessageComplete && !navigations.isNullOrEmpty()) {
//                    Spacer(modifier = Modifier.height(16.dp))
//                    LazyRow(
//                        modifier = Modifier.fillMaxWidth(),
//                        horizontalArrangement = Arrangement.spacedBy(12.dp)
//                    ) {
//                        items(navigations) { nav ->
//                            NavigationCard(navigation = nav) {
//                                onNavigationClick(nav)
//                            }
//                        }
//                    }
//                }
//            }
//        }
//    }
//
//    @Composable
//    fun NavigationCard(navigation: Navigation, onClick: () -> Unit) {
//        Card(
//            modifier = Modifier
//                .width(220.dp)
//                .fillMaxHeight()
//                .clickable { onClick() },
//            shape = RoundedCornerShape(12.dp),
//            elevation = 0.dp,
//            backgroundColor = Color(0xFF20FFFFFF)
//        ) {
//            Column(
//                modifier = Modifier.padding(16.dp),
//                verticalArrangement = Arrangement.SpaceBetween
//            ) {
//                Text(
//                    text = navigation.title,
//                    style = TextStyle(
//                        color = Color.White,
//                        fontSize = 16.sp,
//                        fontWeight = FontWeight.Bold
//                    )
//                )
//                Spacer(modifier = Modifier.height(6.dp))
//                Text(
//                    text = navigation.description,
//                    style = TextStyle(
//                        color = Color(0xFFCCCCCC),
//                        fontSize = 14.sp,
//                        lineHeight = 20.sp
//                    ),
//                    maxLines = 3
//                )
//                Spacer(modifier = Modifier.height(6.dp))
//                Box(Modifier.fillMaxWidth(), contentAlignment = Alignment.BottomEnd) {
//                    Icon(
//                        painter = painterResource(id = R.drawable.ic_navigation_icon),
//                        contentDescription = "Navigate",
//                        tint = Color.White,
//                        modifier = Modifier.size(20.dp)
//                    )
//                }
//            }
//        }
//    }
//}
