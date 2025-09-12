package com.sky.androiddemo

import android.os.Bundle
import android.util.Log
import android.view.View
import android.widget.AdapterView
import android.widget.ArrayAdapter
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import com.google.mediapipe.tasks.genai.llminference.LlmInference
import com.sky.androiddemo.databinding.ActivityChatBinding
import java.io.File

class ChatActivity : AppCompatActivity() {

    private lateinit var binding: ActivityChatBinding
    private lateinit var chatAdapter: ChatAdapter
    private val messages = mutableListOf<Message>()
    private var llmInference: LlmInference? = null
    private val models = listOf("gemma-3-1b-it-int4.task", "gemma-3-1b-it-q4-block128.task")
    private var selectedModel = models[0]

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityChatBinding.inflate(layoutInflater)
        setContentView(binding.root)

        setupRecyclerView()
        setupModelSpinner()
        setupSendButton()

        // Inform the user to place the models in the correct directory
        Toast.makeText(this, "Please ensure models are in /data/local/tmp/llm/", Toast.LENGTH_LONG).show()

        initializeLlmInference()
    }

    private fun setupRecyclerView() {
        chatAdapter = ChatAdapter(messages)
        binding.chatRecyclerView.layoutManager = LinearLayoutManager(this)
        binding.chatRecyclerView.adapter = chatAdapter
    }

    private fun setupModelSpinner() {
        val adapter = ArrayAdapter(this, android.R.layout.simple_spinner_item, models)
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
        binding.modelSpinner.adapter = adapter
        binding.modelSpinner.onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
            override fun onItemSelected(parent: AdapterView<*>, view: View?, position: Int, id: Long) {
                selectedModel = models[position]
                initializeLlmInference()
            }

            override fun onNothingSelected(parent: AdapterView<*>) {}
        }
    }

    private fun setupSendButton() {
        binding.sendButton.setOnClickListener {
            val inputText = binding.inputText.text.toString()
            if (inputText.isNotBlank()) {
                addMessage(inputText, true)
                binding.inputText.text.clear()
                generateResponse(inputText)
            }
        }
    }

    private fun initializeLlmInference() {
        val modelPath = "/data/local/tmp/llm/$selectedModel"
        if (!File(modelPath).exists()) {
            Log.e("ChatActivity", "Model file does not exist at path: $modelPath")
            Toast.makeText(this, "Model not found. Please push the model to the device.", Toast.LENGTH_LONG).show()
            return
        }

        try {
            val options = LlmInference.LlmInferenceOptions.builder()
                .setModelPath(modelPath)
                .setMaxTokens(1024)
                .setResultListener { partialResult, done ->
                    if (done) {
                        // Add the final response to the chat
                        addMessage(partialResult, false)
                    } else {
                        // Stream the partial response
                        updateLastMessage(partialResult)
                    }
                }
                .setErrorListener { error ->
                    Log.e("ChatActivity", "Error: ${error.message}")
                }
                .build()

            llmInference = LlmInference.createFromOptions(this, options)
        } catch (e: Exception) {
            Log.e("ChatActivity", "Failed to initialize LLM Inference", e)
            Toast.makeText(this, "Failed to initialize LLM: ${e.message}", Toast.LENGTH_LONG).show()
        }
    }

    private fun generateResponse(prompt: String) {
        llmInference?.let {
            // Add a placeholder for the model's response
            addMessage("...", false)
            it.generateResponseAsync(prompt)
        }
    }

    private fun addMessage(text: String, fromUser: Boolean) {
        messages.add(Message(text, fromUser))
        chatAdapter.notifyItemInserted(messages.size - 1)
        binding.chatRecyclerView.scrollToPosition(messages.size - 1)
    }

    private fun updateLastMessage(text: String) {
        if (messages.isNotEmpty() && !messages.last().isFromUser) {
            messages[messages.size - 1] = Message(text, false)
            chatAdapter.notifyItemChanged(messages.size - 1)
        }
    }
}
