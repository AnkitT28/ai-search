package com.example.aichat.view

import android.Manifest
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
import androidx.compose.animation.*
import androidx.compose.animation.core.tween
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import com.example.aichat.viewmodel.ChatViewModel
import java.util.Locale

class AiChatActivity : AppCompatActivity() {

    private val chatViewModel: ChatViewModel by viewModels()

    // TTS instance
    private var tts: TextToSpeech? = null

    // For streaming STT
    private var speechRecognizer: SpeechRecognizer? = null
    private lateinit var recognizerIntent: Intent

    companion object {
        private const val PERMISSION_REQUEST_RECORD_AUDIO = 123
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // 1) Check or request RECORD_AUDIO permission
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.RECORD_AUDIO)
            != PackageManager.PERMISSION_GRANTED
        ) {
            Log.d("LOG_FOR_AUDIO", "RECORD_AUDIO permission not granted, requesting...")
            ActivityCompat.requestPermissions(
                this, arrayOf(Manifest.permission.RECORD_AUDIO), PERMISSION_REQUEST_RECORD_AUDIO
            )
        } else {
            Log.d("LOG_FOR_AUDIO", "RECORD_AUDIO permission already granted")
            // Initialize SpeechRecognizer
            initSpeechRecognizer()
        }

        // 2) Initialize TTS
        tts = TextToSpeech(this) { status ->
            if (status == TextToSpeech.SUCCESS) {
                val result = tts?.setLanguage(Locale.US)
                if (result == TextToSpeech.LANG_MISSING_DATA || result == TextToSpeech.LANG_NOT_SUPPORTED) {
                    Log.e("TTS", "Language not supported")
                } else {
                    Log.d("TTS", "TTS initialized successfully")
                }
            } else {
                Log.e("TTS", "Initialization failed")
            }
        }

        // 3) Set Compose UI
        setContent {
            var showPopup by remember { mutableStateOf(true) }

            Box(modifier = Modifier.fillMaxSize()) {
                AnimatedVisibility(
                    visible = showPopup,
                    enter = slideInVertically(
                        initialOffsetY = { -it },
                        animationSpec = tween(durationMillis = 400)
                    ),
                    exit = slideOutVertically(
                        targetOffsetY = { -it },
                        animationSpec = tween(durationMillis = 400)
                    )
                ) {
                    ChatPopup(
                        viewModel = chatViewModel,
                        tts = tts,
                        onClose = {
                            // Stop recognition, finish activity
                            stopSpeechRecognition()
                            showPopup = false
                            finish()
                        },
                        startSpeechToText = {
                            // Toggle STT on/off
                            if (!chatViewModel.isRecording.value) {
                                startSpeechRecognition()
                            } else {
                                stopSpeechRecognition()
                            }
                        }
                    )
                }
            }
        }
    }

    /** Initialize SpeechRecognizer and set RecognitionListener */
    private fun initSpeechRecognizer() {
        Log.d("LOG_FOR_AUDIO", "Initializing SpeechRecognizer...")

        // Check if speech recognition is available
        if (!SpeechRecognizer.isRecognitionAvailable(this)) {
            Log.e("LOG_FOR_AUDIO", "Speech Recognition not available on this device.")
            handleUnsupportedSpeechRecognition()
            return
        }

        // Initialize the SpeechRecognizer
        speechRecognizer = SpeechRecognizer.createSpeechRecognizer(this)
        Log.d("LOG_FOR_AUDIO", "SpeechRecognizer initialized successfully")

        // Set the RecognitionListener
        speechRecognizer?.setRecognitionListener(object : RecognitionListener {
            override fun onReadyForSpeech(params: Bundle?) {
                chatViewModel.setRecording(true)
                Log.d("LOG_FOR_AUDIO", "Ready for speech")
            }

            override fun onBeginningOfSpeech() {
                Log.d("LOG_FOR_AUDIO", "User started speaking")
            }

            override fun onRmsChanged(rmsdB: Float) {
                // You can use this to animate UI based on sound levels
            }

            override fun onBufferReceived(buffer: ByteArray?) {
                Log.d("LOG_FOR_AUDIO", "Buffer received")
            }

            override fun onEndOfSpeech() {
                chatViewModel.setRecording(false)
                Log.d("LOG_FOR_AUDIO", "Speech ended")
            }

            override fun onError(error: Int) {
                chatViewModel.setRecording(false)
                Log.e("LOG_FOR_AUDIO", "Error occurred: $error")
            }

            override fun onResults(results: Bundle?) {
                val data = results?.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION)
                if (!data.isNullOrEmpty()) {
                    val userSpeech = data[0]
                    Log.d("LOG_FOR_AUDIO", "Recognized speech: $userSpeech")

                    // Pass the recognized speech to ChatViewModel
                    chatViewModel.sendMessage(userSpeech)

                    // Optionally, speak the recognized input
                    tts?.speak(userSpeech, TextToSpeech.QUEUE_FLUSH, null, null)
                } else {
                    Log.d("LOG_FOR_AUDIO", "No speech recognized")
                }
            }

            override fun onPartialResults(partialResults: Bundle?) {
                val data = partialResults?.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION)
                if (!data.isNullOrEmpty()) {
                    val partialSpeech = data[0]
                    Log.d("LOG_FOR_AUDIO", "Partial speech: $partialSpeech")
                }
            }

            override fun onEvent(eventType: Int, params: Bundle?) {
                Log.d("LOG_FOR_AUDIO", "Event occurred: $eventType")
            }
        })

        // Initialize the recognizer intent
        recognizerIntent = Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH).apply {
            putExtra(
                RecognizerIntent.EXTRA_LANGUAGE_MODEL,
                RecognizerIntent.LANGUAGE_MODEL_FREE_FORM
            )
            putExtra(RecognizerIntent.EXTRA_LANGUAGE, Locale.getDefault())
            putExtra(RecognizerIntent.EXTRA_PARTIAL_RESULTS, true)
            putExtra(RecognizerIntent.EXTRA_MAX_RESULTS, 1)
        }
    }

    private fun startSpeechRecognition() {
        if (speechRecognizer == null) {
            Log.e("LOG_FOR_AUDIO", "SpeechRecognizer is not initialized")
            handleUnsupportedSpeechRecognition()
            return
        }

        if (!::recognizerIntent.isInitialized) {
            Log.e("LOG_FOR_AUDIO", "RecognizerIntent is not initialized")
            handleUnsupportedSpeechRecognition()
            return
        }

        if (ContextCompat.checkSelfPermission(this, Manifest.permission.RECORD_AUDIO)
            != PackageManager.PERMISSION_GRANTED
        ) {
            Log.e("LOG_FOR_AUDIO", "Permission not granted for RECORD_AUDIO")
            return
        }

        chatViewModel.setRecording(true)
        Log.d("LOG_FOR_AUDIO", "Starting SpeechRecognizer...")
        speechRecognizer?.startListening(recognizerIntent)
        Log.d("LOG_FOR_AUDIO", "SpeechRecognizer started")
    }




    /** Stop speech recognition */
    private fun stopSpeechRecognition() {
        if (speechRecognizer == null) {
            Log.e("LOG_FOR_AUDIO", "SpeechRecognizer is not initialized")
            return
        }

        chatViewModel.setRecording(false)
        speechRecognizer?.stopListening()
        Log.d("LOG_FOR_AUDIO", "SpeechRecognizer stopped")
    }

    override fun onDestroy() {
        super.onDestroy()
        tts?.stop()
        tts?.shutdown()
        tts = null

        speechRecognizer?.destroy()
        speechRecognizer = null
    }

    // Handle runtime permission result
    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<out String>,
        grantResults: IntArray
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        if (requestCode == PERMISSION_REQUEST_RECORD_AUDIO) {
            if (grantResults.isNotEmpty() && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                Log.d("LOG_FOR_AUDIO", "RECORD_AUDIO permission granted")
                initSpeechRecognizer()
            } else {
                Log.e("LOG_FOR_AUDIO", "RECORD_AUDIO permission denied")
            }
        }
    }

    private fun handleUnsupportedSpeechRecognition() {
        runOnUiThread {
            Toast.makeText(
                this,
                "Speech recognition is not supported on your device. Install Google App for support.",
                Toast.LENGTH_LONG
            ).show()
        }
    }


}
