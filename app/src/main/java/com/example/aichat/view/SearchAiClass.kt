package com.example.aichat.view

import android.Manifest
import android.annotation.SuppressLint
import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.pm.PackageManager
import android.graphics.Color
import android.os.Bundle
import android.speech.tts.TextToSpeech
import android.speech.tts.UtteranceProgressListener
import android.text.*
import android.view.*
import android.view.inputmethod.EditorInfo
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.lifecycle.Observer
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.asLiveData
import com.example.aichat.R
import com.example.aichat.databinding.ActivitySearchAiClassBinding
import com.example.aichat.databinding.ItemHeaderSuggestionBinding
import com.example.aichat.databinding.ItemRecentSearchBinding
import com.example.aichat.databinding.ItemRedirectedCardBinding
import com.example.aichat.databinding.ItemSuggestionBinding
import com.example.aichat.viewmodel.ChatViewModel
import java.util.Locale

class SearchAiClass : AppCompatActivity(), VoiceInputBottomSheetFragment.VoiceInputListener {

    private lateinit var binding: ActivitySearchAiClassBinding
    private lateinit var chatViewModel: ChatViewModel

    // TTS
    private var tts: TextToSpeech? = null
    private var isSpeakerOn = false

    companion object {
        private const val PERMISSION_REQUEST_RECORD_AUDIO = 123
        private const val UTTERANCE_ID = "utteranceId"
    }

    @SuppressLint("MissingInflatedId")
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivitySearchAiClassBinding.inflate(layoutInflater)
        setContentView(binding.root)

        // Initialize ViewModel
        chatViewModel = ViewModelProvider(this)[ChatViewModel::class.java]

        // Set status & navigation bar colors
        window.statusBarColor = Color.parseColor("#1f002b")
        window.navigationBarColor = Color.parseColor("#1f002b")

        // TTS with progress listener
        tts = TextToSpeech(this) { status ->
            if (status == TextToSpeech.SUCCESS) {
                tts?.language = Locale.US
                tts?.setOnUtteranceProgressListener(object : UtteranceProgressListener() {
                    override fun onStart(utteranceId: String?) { /* no-op */
                    }

                    override fun onDone(utteranceId: String?) {
                        runOnUiThread {
                            binding.listenButton.text = getString(R.string.listen)
                            isSpeakerOn = false
                        }
                    }

                    override fun onError(utteranceId: String?) {
                        runOnUiThread {
                            binding.listenButton.text = getString(R.string.listen)
                            isSpeakerOn = false
                        }
                    }
                })
            }
        }

        // Check RECORD_AUDIO permission
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.RECORD_AUDIO)
            != PackageManager.PERMISSION_GRANTED
        ) {
            ActivityCompat.requestPermissions(
                this,
                arrayOf(Manifest.permission.RECORD_AUDIO),
                PERMISSION_REQUEST_RECORD_AUDIO
            )
        }

        // === Haptic feedback for mic button & search EditText ===
        // 1) Search EditText
        binding.searchEditText.setOnClickListener {
            // Provide haptic feedback
            it.performHapticFeedback(HapticFeedbackConstants.VIRTUAL_KEY)
        }
        // 2) Mic/cancel button
        binding.micCancelButton.setOnClickListener {
            // Provide haptic feedback
            it.performHapticFeedback(HapticFeedbackConstants.VIRTUAL_KEY)

            val currentText = binding.searchEditText.text.toString().trim()
            if (currentText.isEmpty()) {
                // If search box is empty => open voice input bottom sheet
                val bottomSheet = VoiceInputBottomSheetFragment()
                bottomSheet.show(supportFragmentManager, "VoiceInputBottomSheet")
            } else {
                // If text exists => clear it
                binding.searchEditText.setText("")
            }
        }

        // Toggle mic/cancel icon based on search text
        binding.searchEditText.addTextChangedListener(object : TextWatcher {
            override fun afterTextChanged(s: Editable?) {
                if (!s.isNullOrEmpty()) {
                    binding.micCancelButton.setImageResource(R.drawable.ic_cancel)
                } else {
                    binding.micCancelButton.setImageResource(R.drawable.ic_mic)
                }
            }

            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {}
        })

        // Ensure cursor is at end when new text is set
        binding.searchEditText.addTextChangedListener(object : TextWatcher {
            override fun afterTextChanged(s: Editable?) {
                s?.let {
                    binding.searchEditText.setSelection(it.length)
                }
            }

            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {}
        })

        // Keyboard search action
        binding.searchEditText.setOnEditorActionListener { _, actionId, event ->
            if (actionId == EditorInfo.IME_ACTION_SEARCH ||
                (event != null
                        && event.keyCode == KeyEvent.KEYCODE_ENTER
                        && event.action == KeyEvent.ACTION_DOWN)
            ) {
                val query = binding.searchEditText.text.toString().trim()
                if (query.isNotEmpty()) {
                    performSearch(query)
                }
                true
            } else {
                false
            }
        }

        // Copy button => copy text + "Copied" + check icon
        binding.copyButton.setOnClickListener {
            it.performHapticFeedback(HapticFeedbackConstants.VIRTUAL_KEY)

            val response = binding.responseTextView.text.toString()
            if (response.isNotEmpty()) {
                val clipboard = getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                val clip = ClipData.newPlainText("AI Response", response)
                clipboard.setPrimaryClip(clip)
                // Change text & icon to "Copied"
                binding.copyButton.text = getString(R.string.copied)
                binding.copyButton.setCompoundDrawablesWithIntrinsicBounds(
                    R.drawable.ic_check_small_icon,
                    0,
                    0,
                    0
                )
                // Revert after 3 seconds
                binding.copyButton.postDelayed({
                    binding.copyButton.text = getString(R.string.copy)
                    binding.copyButton.setCompoundDrawablesWithIntrinsicBounds(
                        R.drawable.ic_copy_default,
                        0,
                        0,
                        0
                    )
                }, 3000L)
            }
        }

        // Listen button => TTS
        binding.listenButton.setOnClickListener {
            it.performHapticFeedback(HapticFeedbackConstants.VIRTUAL_KEY)

            val response = binding.responseTextView.text.toString()
            if (response.isEmpty()) return@setOnClickListener

            if (!isSpeakerOn) {
                val params = Bundle().apply {
                    putString(TextToSpeech.Engine.KEY_PARAM_UTTERANCE_ID, UTTERANCE_ID)
                }
                tts?.speak(response, TextToSpeech.QUEUE_FLUSH, params, UTTERANCE_ID)
                binding.listenButton.text = getString(R.string.stop)
                isSpeakerOn = true
            } else {
                tts?.stop()
                binding.listenButton.text = getString(R.string.listen)
                isSpeakerOn = false
            }
        }

        // Observe chat messages => animate response, show nav cards, etc.
        chatViewModel.chatMessages.asLiveData().observe(this, Observer { messages ->
            val aiMessage = messages.lastOrNull { !it.isUser }
            binding.navigationCardsLayout.removeAllViews()
            if (aiMessage != null) {
                if (aiMessage.isLoader) {
                    binding.responseTextView.text = getString(R.string.waiting_for_ai_response)
                    binding.loaderProgressBar.visibility = View.VISIBLE
                    binding.actionButtonsLayout.visibility = View.GONE
                } else {
                    binding.loaderProgressBar.visibility = View.GONE
                    binding.actionButtonsLayout.visibility = View.VISIBLE
                    animateResponseText(aiMessage.botResponse.toString()) {
                        // After animation => reset "Listen"
                        binding.listenButton.text = getString(R.string.listen)
                        // Show navigation cards if any
                        if (!aiMessage.navigations.isNullOrEmpty()) {
                            binding.navigationCardsLayout.visibility = View.VISIBLE
                            aiMessage.navigations.forEach { nav ->
                                val navItemBinding = ItemRedirectedCardBinding.inflate(
                                    LayoutInflater.from(this),
                                    binding.navigationCardsLayout,
                                    false
                                )
                                navItemBinding.cardTitle.text = nav.title
                                navItemBinding.cardDescription.text = nav.description
                                navItemBinding.root.setOnClickListener {
                                    Toast.makeText(
                                        this,
                                        "Navigating to: ${nav.title}",
                                        Toast.LENGTH_SHORT
                                    ).show()
                                }
                                binding.navigationCardsLayout.addView(navItemBinding.root)
                            }
                        } else {
                            binding.navigationCardsLayout.visibility = View.GONE
                        }
                    }
                }
            } else {
                binding.responseTextView.text = ""
                binding.loaderProgressBar.visibility = View.GONE
                binding.actionButtonsLayout.visibility = View.GONE
                binding.navigationCardsLayout.visibility = View.GONE
            }
        })

        // Observe suggestions => show
        chatViewModel.suggestions.asLiveData().observe(this) { suggestions ->
            binding.suggestionsLayout.removeAllViews()

            if (suggestions.isNotEmpty()) {
                binding.suggestionsLayout.visibility = View.VISIBLE

                // Iterate over each TrendingQueryCategory
                suggestions.forEach { category ->

                    val categoryHeaderBinding = ItemHeaderSuggestionBinding.inflate(
                        LayoutInflater.from(this),
                        binding.suggestionsLayout,
                        false
                    )
                    categoryHeaderBinding.suggestionHeader.text =
                        category.category  // Set the category name as header
                    binding.suggestionsLayout.addView(categoryHeaderBinding.root)

                    // Iterate over the queries in each category
                    category.queries.forEach { query ->
                        val itemBinding = ItemSuggestionBinding.inflate(
                            LayoutInflater.from(this),
                            binding.suggestionsLayout,
                            false
                        )
                        itemBinding.suggestionTextView.text = query  // Set the query text
                        itemBinding.root.setOnClickListener {
                            binding.searchEditText.setText(query)  // Set the clicked query in the search box
                            binding.searchEditText.setSelection(query.length)  // Place the cursor at the end
                            performSearch(query)  // Perform search with the clicked query
                        }
                        binding.suggestionsLayout.addView(itemBinding.root)  // Add the query item view
                    }
                }
            }
        }


        // Observe recent searches => show
        chatViewModel.recentSearches.asLiveData().observe(this) { recentSearches ->
            binding.recentSearchesLayout.removeAllViews()
            if (binding.suggestionsLayout.visibility == View.VISIBLE && recentSearches.isNotEmpty()) {
                binding.recentSearchesLayout.visibility = View.VISIBLE
                binding.recentSearchesHeader.visibility = View.VISIBLE
                recentSearches.forEach { recent ->
                    val itemBinding = ItemRecentSearchBinding.inflate(
                        LayoutInflater.from(this),
                        binding.recentSearchesLayout,
                        false
                    )
                    itemBinding.suggestionTextView.text = recent
                    itemBinding.root.setOnClickListener {
                        binding.searchEditText.setText(recent)
                        binding.searchEditText.setSelection(recent.length)
                        performSearch(recent)
                    }
                    binding.recentSearchesLayout.addView(itemBinding.root)
                }
            } else {
                binding.recentSearchesLayout.visibility = View.GONE
                binding.recentSearchesHeader.visibility = View.GONE
            }
        }
    }

    // VoiceInputBottomSheetFragment callback
    override fun onVoiceInputCompleted(recognizedText: String) {
        binding.searchEditText.setText(recognizedText)
        // Place cursor at end
        binding.searchEditText.setSelection(recognizedText.length)
        performSearch(recognizedText)
    }

    // Animate response text letter by letter
    private fun animateResponseText(fullText: String, onAnimationComplete: () -> Unit) {
        binding.responseTextView.text = ""
        val delayPerLetter = 50L
        var currentIndex = 0

        binding.responseTextView.post(object : Runnable {
            override fun run() {
                if (currentIndex < fullText.length) {
                    binding.responseTextView.append(fullText[currentIndex].toString())
                    currentIndex++
                    binding.responseTextView.postDelayed(this, delayPerLetter)
                } else {
                    onAnimationComplete()
                }
            }
        })
    }

    // Perform search
    private fun performSearch(query: String) {
        binding.suggestionsLayout.visibility = View.GONE
        binding.recentSearchesLayout.visibility = View.GONE
        binding.recentSearchesHeader.visibility = View.GONE
        binding.navigationCardsLayout.visibility = View.GONE
        binding.responseTextView.text = ""
        binding.loaderProgressBar.visibility = View.VISIBLE
        binding.actionButtonsLayout.visibility = View.GONE

        chatViewModel.clearMessages()
        chatViewModel.sendMessage(query)
    }

    override fun onDestroy() {
        super.onDestroy()
        tts?.shutdown()
    }
}
