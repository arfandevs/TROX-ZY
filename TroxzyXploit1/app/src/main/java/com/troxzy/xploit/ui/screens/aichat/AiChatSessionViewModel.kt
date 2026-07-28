package com.troxzy.xploit.ui.screens.aichat

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.troxzy.xploit.data.local.dao.ChatMessageDao
import com.troxzy.xploit.data.local.dao.ChatSessionDao
import com.troxzy.xploit.data.local.entity.ChatMessageEntity
import com.troxzy.xploit.data.local.entity.ChatSessionEntity
import com.troxzy.xploit.data.remote.ChatMessageDto
import com.troxzy.xploit.data.repository.AiChatRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

/**
 * ViewModel that manages a specific AI chat session.
 * Handles message sending, streaming, regeneration, and session configuration.
 */
@HiltViewModel
class AiChatSessionViewModel @Inject constructor(
    private val chatMessageDao: ChatMessageDao,
    private val chatSessionDao: ChatSessionDao,
    private val aiChatRepository: AiChatRepository
) : ViewModel() {

    // Current session ID
    private var currentSessionId: Long = -1L

    // Streaming job for cancellation support
    private var streamingJob: Job? = null

    // ============================================================
    // StateFlows exposed to UI
    // ============================================================

    /**
     * All messages for the current session, ordered by timestamp ascending.
     */
    val messages: StateFlow<List<ChatMessageEntity>> = chatMessageDao
        .getMessagesForSession(currentSessionId)
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = emptyList()
        )

    /**
     * Accumulated streaming text from the AI response.
     * Cleared when streaming completes or is stopped.
     */
    private val _streamingText = MutableStateFlow("")
    val streamingText: StateFlow<String> = _streamingText.asStateFlow()

    /**
     * Whether the AI is currently streaming a response.
     */
    private val _isStreaming = MutableStateFlow(false)
    val isStreaming: StateFlow<Boolean> = _isStreaming.asStateFlow()

    /**
     * Currently selected model for the session.
     */
    private val _currentModel = MutableStateFlow("glm/glm-5.2")
    val currentModel: StateFlow<String> = _currentModel.asStateFlow()

    /**
     * Current temperature setting for generation.
     */
    private val _temperature = MutableStateFlow(0.7)
    val temperature: StateFlow<Double> = _temperature.asStateFlow()

    /**
     * Current max tokens setting for generation.
     */
    private val _maxTokens = MutableStateFlow(4096)
    val maxTokens: StateFlow<Int> = _maxTokens.asStateFlow()

    /**
     * Title of the current session.
     */
    private val _sessionTitle = MutableStateFlow<String?>(null)
    val sessionTitle: StateFlow<String?> = _sessionTitle.asStateFlow()

    /**
     * Total token count for the current session (sum of all message tokens).
     */
    private val _totalTokens = MutableStateFlow(0)
    val totalTokens: StateFlow<Int> = _totalTokens.asStateFlow()

    // Re-observable messages flow that updates when sessionId changes
    private val _messagesFlow = MutableStateFlow<List<ChatMessageEntity>>(emptyList())

    /**
     * Alternative messages flow that properly tracks sessionId changes.
     */
    val messagesTracked: StateFlow<List<ChatMessageEntity>> = _messagesFlow.asStateFlow()

    // ============================================================
    // Public Functions
    // ============================================================

    /**
     * Loads a specific chat session by ID.
     * Updates all relevant state flows and begins observing messages.
     *
     * @param sessionId The ID of the session to load.
     */
    fun loadSession(sessionId: Long) {
        currentSessionId = sessionId
        viewModelScope.launch {
            // Load session configuration
            val session = chatSessionDao.getSessionById(sessionId)
            if (session != null) {
                _currentModel.value = session.modelName
                _temperature.value = session.temperature
                _maxTokens.value = session.maxTokens
                _sessionTitle.value = session.title
            }

            // Observe messages for this session
            chatMessageDao.getMessagesForSession(sessionId).collect { messageList ->
                _messagesFlow.value = messageList
                _totalTokens.value = messageList.sumOf { it.tokenCount }
            }
        }
    }

    /**
     * Sends a user message and begins streaming the AI response.
     *
     * Process:
     * 1. Insert user message into DB
     * 2. Build ChatMessageDto list from all messages + system prompt
     * 3. Call aiChatRepository.streamChat()
     * 4. Collect Flow<String> tokens, accumulate into streamingText
     * 5. On [DONE], insert complete AI message into DB, clear streaming state
     * 6. On [ERROR], show error state
     * 7. Handle cancellation properly
     *
     * @param content The user's message content.
     */
    fun sendMessage(content: String) {
        if (_isStreaming.value) return // Prevent duplicate sends

        viewModelScope.launch {
            // Step 1: Insert user message into DB
            val userMessage = ChatMessageEntity(
                sessionId = currentSessionId,
                role = "user",
                content = content,
                timestamp = System.currentTimeMillis(),
                tokenCount = estimateTokenCount(content)
            )
            chatMessageDao.insertMessage(userMessage)

            // Update session's last message preview
            chatSessionDao.updateLastMessage(
                sessionId = currentSessionId,
                lastMessagePreview = content.take(100),
                updatedAt = System.currentTimeMillis()
            )

            // Step 2: Build ChatMessageDto list from all messages + system prompt
            val allMessages = _messagesFlow.value + userMessage
            val messageDtos = buildMessageDtos(allMessages)

            // Step 3-6: Start streaming
            startStreaming(messageDtos)
        }
    }

    /**
     * Regenerates the last AI message.
     * Deletes the last AI message and re-sends with the same context.
     *
     * @param messageId The ID of the AI message to regenerate.
     */
    fun regenerateMessage(messageId: Long) {
        if (_isStreaming.value) return

        viewModelScope.launch {
            // Delete the specified AI message
            chatMessageDao.deleteMessage(messageId)

            // Re-build context from remaining messages
            val remainingMessages = _messagesFlow.value.filter { it.id != messageId }
            val messageDtos = buildMessageDtos(remainingMessages)

            // Start streaming with existing context
            startStreaming(messageDtos)
        }
    }

    /**
     * Deletes a specific message by its ID.
     *
     * @param messageId The ID of the message to delete.
     */
    fun deleteMessage(messageId: Long) {
        viewModelScope.launch {
            chatMessageDao.deleteMessage(messageId)
        }
    }

    /**
     * Updates the session configuration (model, temperature, max tokens).
     * Persists changes to the database.
     *
     * @param model The new model name.
     * @param temperature The new temperature value.
     * @param maxTokens The new max tokens value.
     */
    fun updateSessionConfig(model: String, temperature: Double, maxTokens: Int) {
        _currentModel.value = model
        _temperature.value = temperature
        _maxTokens.value = maxTokens

        viewModelScope.launch {
            chatSessionDao.updateSessionConfig(
                sessionId = currentSessionId,
                modelName = model,
                temperature = temperature,
                maxTokens = maxTokens,
                updatedAt = System.currentTimeMillis()
            )
        }
    }

    /**
     * Stops the current streaming operation.
     */
    fun stopStreaming() {
        streamingJob?.cancel()
        streamingJob = null

        // If we have partial streaming text, save it as a partial message
        val partialText = _streamingText.value
        if (partialText.isNotEmpty()) {
            viewModelScope.launch {
                val aiMessage = ChatMessageEntity(
                    sessionId = currentSessionId,
                    role = "assistant",
                    content = partialText + "\n\n*[Response interrupted]*",
                    timestamp = System.currentTimeMillis(),
                    tokenCount = estimateTokenCount(partialText)
                )
                chatMessageDao.insertMessage(aiMessage)

                chatSessionDao.updateLastMessage(
                    sessionId = currentSessionId,
                    lastMessagePreview = partialText.take(100),
                    updatedAt = System.currentTimeMillis()
                )
            }
        }

        _streamingText.value = ""
        _isStreaming.value = false
    }

    // ============================================================
    // Private Functions
    // ============================================================

    /**
     * Starts the streaming process for AI response.
     *
     * @param messageDtos The list of message DTOs to send as context.
     */
    private fun startStreaming(messageDtos: List<ChatMessageDto>) {
        _isStreaming.value = true
        _streamingText.value = ""

        streamingJob = viewModelScope.launch {
            try {
                val streamFlow = aiChatRepository.streamChat(
                    messages = messageDtos,
                    model = _currentModel.value,
                    temperature = _temperature.value,
                    maxTokens = _maxTokens.value
                )

                streamFlow.collect { token ->
                    when {
                        token == "[DONE]" -> {
                            // Step 5: Insert complete AI message into DB
                            val completeText = _streamingText.value
                            val aiMessage = ChatMessageEntity(
                                sessionId = currentSessionId,
                                role = "assistant",
                                content = completeText,
                                timestamp = System.currentTimeMillis(),
                                tokenCount = estimateTokenCount(completeText)
                            )
                            chatMessageDao.insertMessage(aiMessage)

                            // Update session's last message preview
                            chatSessionDao.updateLastMessage(
                                sessionId = currentSessionId,
                                lastMessagePreview = completeText.take(100),
                                updatedAt = System.currentTimeMillis()
                            )

                            // Clear streaming state
                            _streamingText.value = ""
                            _isStreaming.value = false
                        }
                        token.startsWith("[ERROR]") -> {
                            // Step 6: Handle error state
                            val errorMessage = token.removePrefix("[ERROR]")
                            _streamingText.value = ""
                            _isStreaming.value = false

                            // Insert error message as AI response
                            val aiMessage = ChatMessageEntity(
                                sessionId = currentSessionId,
                                role = "assistant",
                                content = "⚠️ Error: $errorMessage",
                                timestamp = System.currentTimeMillis(),
                                tokenCount = 0
                            )
                            chatMessageDao.insertMessage(aiMessage)
                        }
                        else -> {
                            // Step 4: Accumulate token into streamingText
                            _streamingText.update { current -> current + token }
                        }
                    }
                }
            } catch (e: kotlinx.coroutines.CancellationException) {
                // Step 7: Handle cancellation properly
                // Cancellation is handled by stopStreaming() or natural coroutine lifecycle
                val partialText = _streamingText.value
                if (partialText.isNotEmpty()) {
                    val aiMessage = ChatMessageEntity(
                        sessionId = currentSessionId,
                        role = "assistant",
                        content = partialText + "\n\n*[Response interrupted]*",
                        timestamp = System.currentTimeMillis(),
                        tokenCount = estimateTokenCount(partialText)
                    )
                    chatMessageDao.insertMessage(aiMessage)
                }
                _streamingText.value = ""
                _isStreaming.value = false
            } catch (e: Exception) {
                // Handle unexpected errors
                _streamingText.value = ""
                _isStreaming.value = false

                val aiMessage = ChatMessageEntity(
                    sessionId = currentSessionId,
                    role = "assistant",
                    content = "⚠️ Unexpected error: ${e.message ?: "Unknown error"}",
                    timestamp = System.currentTimeMillis(),
                    tokenCount = 0
                )
                chatMessageDao.insertMessage(aiMessage)
            }
        }
    }

    /**
     * Builds the list of ChatMessageDto from ChatMessageEntity list.
     * Prepends a system prompt to guide the AI's behavior.
     *
     * @param messages The list of message entities to convert.
     * @return List of ChatMessageDto ready for the API.
     */
    private fun buildMessageDtos(messages: List<ChatMessageEntity>): List<ChatMessageDto> {
        val systemPrompt = ChatMessageDto(
            role = "system",
            content = "You are Troxzy AI Brain, an advanced AI assistant built into the TroxzyXploit framework. " +
                    "You are knowledgeable, precise, and helpful. You provide clear, well-formatted responses " +
                    "using markdown when appropriate. You can assist with coding, analysis, security research, " +
                    "and general knowledge queries."
        )

        val conversationMessages = messages.map { entity ->
            ChatMessageDto(
                role = entity.role,
                content = entity.content
            )
        }

        return listOf(systemPrompt) + conversationMessages
    }

    /**
     * Estimates the token count for a given text.
     * Uses a simple heuristic: ~4 characters per token for English text.
     *
     * @param text The text to estimate tokens for.
     * @return Estimated token count.
     */
    private fun estimateTokenCount(text: String): Int {
        return (text.length / 4).coerceAtLeast(1)
    }

    override fun onCleared() {
        super.onCleared()
        // Cancel any ongoing streaming when ViewModel is destroyed
        streamingJob?.cancel()
    }
}
