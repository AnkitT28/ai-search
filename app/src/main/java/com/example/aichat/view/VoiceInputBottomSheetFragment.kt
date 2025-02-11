package com.example.aichat.view

import android.animation.AnimatorSet
import android.animation.ObjectAnimator
import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.speech.RecognitionListener
import android.speech.RecognizerIntent
import android.speech.SpeechRecognizer
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import com.example.aichat.R
import com.example.aichat.databinding.FragmentVoiceInputBottomSheetBinding
import com.google.android.material.bottomsheet.BottomSheetBehavior
import com.google.android.material.bottomsheet.BottomSheetDialogFragment
import java.util.Locale

class VoiceInputBottomSheetFragment : BottomSheetDialogFragment(), RecognitionListener {

    private var _binding: FragmentVoiceInputBottomSheetBinding? = null
    private val binding get() = _binding!!

    private var speechRecognizer: SpeechRecognizer? = null
    private lateinit var recognizerIntent: Intent
    private var isListening = false

    // The bars for the visualizer
    private val barAnimators = mutableListOf<ObjectAnimator>()

    // 4-second inactivity timer
    private val inactivityDelayMillis = 4000L
    private val handler = Handler(Looper.getMainLooper())
    private var inactivityRunnable: Runnable? = null

    // Callback to return final recognized text
    interface VoiceInputListener {
        fun onVoiceInputCompleted(recognizedText: String)
    }

    private var listener: VoiceInputListener? = null

    override fun onAttach(context: Context) {
        super.onAttach(context)
        if (context is VoiceInputListener) {
            listener = context
        } else {
            throw RuntimeException("$context must implement VoiceInputListener")
        }
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentVoiceInputBottomSheetBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onStart() {
        super.onStart()
        dialog?.let { dlg ->
            val bottomSheet =
                dlg.findViewById<View>(com.google.android.material.R.id.design_bottom_sheet)
            bottomSheet?.let { sheet ->
                // Force the sheet background to be your custom shape (so white corners won't appear)
                sheet.setBackgroundResource(R.drawable.rounded_top_backgroun_bottomsheet)

                val behavior = BottomSheetBehavior.from(sheet)
                behavior.peekHeight = (resources.displayMetrics.heightPixels * 0.5).toInt()
                behavior.state = BottomSheetBehavior.STATE_EXPANDED
            }
        }
        // Start listening automatically
        if (!isListening) {
            startListening()
        }
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        // Setup the SpeechRecognizer
        if (SpeechRecognizer.isRecognitionAvailable(requireContext())) {
            speechRecognizer = SpeechRecognizer.createSpeechRecognizer(requireContext())
            speechRecognizer?.setRecognitionListener(this)

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
            Toast.makeText(requireContext(), "Speech Recognition not available", Toast.LENGTH_LONG)
                .show()
            dismiss()
        }

        // The start/stop button
        binding.buttonStartStop.setOnClickListener {
            if (!isListening) {
                startListening()
            } else {
                stopListening()
            }
        }
    }

    private fun startListening() {
        speechRecognizer?.startListening(recognizerIntent)
        isListening = true

        // Show recognized textView
        binding.recognizedSpeechTextView.visibility = View.VISIBLE
        binding.recognizedSpeechTextView.text = ""
        binding.statusTextView.text = "“Try Asking about your daily horoscope or Kundli insights.”"

        // Hide button, show visualizer
        binding.visualizerContainer.visibility = View.VISIBLE
        binding.tapToSpeakText.visibility = View.GONE
        binding.buttonStartStop.visibility = View.GONE

        startBarAnimations()
    }

    private fun stopListening() {
        speechRecognizer?.stopListening()
        isListening = false

        // Cancel inactivity timer
        inactivityRunnable?.let { handler.removeCallbacks(it) }
        inactivityRunnable = null

        // Show mic button again, hide visualizer
        binding.visualizerContainer.visibility = View.GONE
        binding.buttonStartStop.visibility = View.VISIBLE
        binding.buttonStartStop.setImageResource(R.drawable.ic_mic)
        stopBarAnimations()
    }

    private fun startBarAnimations() {
        stopBarAnimations()
        val bars = listOf(binding.bar1, binding.bar2, binding.bar3, binding.bar4, binding.bar5)
        for ((index, bar) in bars.withIndex()) {
            val animator = ObjectAnimator.ofFloat(bar, "scaleY", 1f, 3f).apply {
                duration = 300L
                repeatCount = ObjectAnimator.INFINITE
                repeatMode = ObjectAnimator.REVERSE
                startDelay = index * 100L
            }
            animator.start()
            barAnimators.add(animator)
        }
    }

    private fun stopBarAnimations() {
        barAnimators.forEach { it.cancel() }
        barAnimators.clear()
    }

    // RecognitionListener

    override fun onReadyForSpeech(params: Bundle?) {}
    override fun onBeginningOfSpeech() {}
    override fun onRmsChanged(rmsdB: Float) {}
    override fun onBufferReceived(buffer: ByteArray?) {}

    /**
     * We IGNORE onEndOfSpeech => do NOT finalize here
     * to allow short breaks in speech. We'll rely on the inactivity timer + partial results.
     */
    override fun onEndOfSpeech() {
        // do nothing => ignoring default behavior
    }

    override fun onError(error: Int) {
        isListening = false
        stopBarAnimations()
        binding.visualizerContainer.visibility = View.GONE
        binding.buttonStartStop.visibility = View.VISIBLE
        binding.buttonStartStop.setImageResource(R.drawable.ic_mic)
        binding.statusTextView.text = "Try asking about your daily horoscope or Kundli insights."
        binding.tapToSpeakText.visibility = View.VISIBLE

        Toast.makeText(requireContext(), "Error: $error", Toast.LENGTH_SHORT).show()
    }

    /**
     * Each partial result resets a 4-second timer. If user stays silent for 4s => stopListening()
     */
    override fun onPartialResults(partialResults: Bundle?) {
        val data = partialResults?.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION)
        if (!data.isNullOrEmpty()) {
            binding.recognizedSpeechTextView.text = data[0]
            binding.statusTextView.text = "Listening..."

            // Cancel old timer, set a new one
            inactivityRunnable?.let { handler.removeCallbacks(it) }
            inactivityRunnable = Runnable {
                // 4s of silence => finalize => leads to onResults()
                stopListening()
            }
            handler.postDelayed(inactivityRunnable!!, inactivityDelayMillis)
        }
    }

    override fun onEvent(eventType: Int, params: Bundle?) {}

    override fun onResults(results: Bundle?) {
        // The system final results come here (likely after stopListening).
        // Cancel the inactivity timer
        inactivityRunnable?.let { handler.removeCallbacks(it) }
        inactivityRunnable = null

        val data = results?.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION)
        if (!data.isNullOrEmpty()) {
            val recognizedText = data[0]
            // Show final text
            binding.recognizedSpeechTextView.text = recognizedText
            binding.statusTextView.text = "Searching..."

            // Show check icon + animate
            binding.buttonStartStop.visibility = View.VISIBLE
            binding.buttonStartStop.setImageResource(R.drawable.ic_check)
            binding.buttonStartStop.scaleX = 0f
            binding.buttonStartStop.scaleY = 0f

            val scaleX = ObjectAnimator.ofFloat(binding.buttonStartStop, "scaleX", 0f, 1f)
            val scaleY = ObjectAnimator.ofFloat(binding.buttonStartStop, "scaleY", 0f, 1f)
            scaleX.duration = 500L
            scaleY.duration = 500L
            AnimatorSet().apply {
                playTogether(scaleX, scaleY)
                start()
            }
            binding.visualizerContainer.visibility = View.GONE

            // Delay 3s => callback => dismiss
            binding.buttonStartStop.postDelayed({
                listener?.onVoiceInputCompleted(recognizedText)
                dismiss()
            }, 3000L)
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        speechRecognizer?.destroy()
        _binding = null
    }
}
