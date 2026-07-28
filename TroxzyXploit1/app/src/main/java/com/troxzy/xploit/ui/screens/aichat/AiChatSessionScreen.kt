package com.troxzy.xploit.ui.screens.aichat

import android.content.Intent
import android.speech.RecognizerIntent
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Stop
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import com.troxzy.xploit.data.local.entity.ChatMessageEntity
import com.troxzy.xploit.ui.components.GlitchText
import com.troxzy.xploit.ui.theme.AmoledBlack
import com.troxzy.xploit.ui.theme.DarkCard
import com.troxzy.xploit.ui.theme.NeonCyan
import com.troxzy.xploit.ui.theme.NeonGreen
import com.troxzy.xploit.ui.theme.NeonPurple

/**
 * AI Chat session screen - the actual chat interface for a specific session.
 * Supports streaming, markdown rendering, code blocks, voice input, and more.
 */
@OptIn(ExperimentalMaterial3Api::class, ExperimentalFoundationApi::class)
@Composable
fun AiChatSessionScreen(
    sessionId: Long,
    onBack: () -> Unit,
    viewModel: AiChatSessionViewModel = hiltViewModel()
) {
    val messages by viewModel.messages.collectAsState()
    val streamingText by viewModel.streamingText.collectAsState()
    val isStreaming by viewModel.isStreaming.collectAsState()
    val currentModel by viewModel.currentModel.collectAsState()
    val temperature by viewModel.temperature.collectAsState()
    val maxTokens by viewModel.maxTokens.collectAsState()
    val sessionTitle by viewModel.sessionTitle.collectAsState()
    val totalTokens by viewModel.totalTokens.collectAsState()

    var inputText by remember { mutableStateOf("") }
    var showModelMenu by remember { mutableStateOf(false) }
    var showBottomSheet by remember { mutableStateOf(false) }
    var showMessagePopup by remember { mutableStateOf<ChatMessageEntity?>(null) }
    var isEditingTitle by remember { mutableStateOf(false) }
    var editTitleText by remember { mutableStateOf("") }

    val listState = rememberLazyListState()
    val clipboardManager = LocalClipboardManager.current
    val context = LocalContext.current

    // Voice input launcher
    val voiceInputLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.StartActivityForResult()
    ) { result ->
        val results = result.data?.getStringArrayListExtra(RecognizerIntent.EXTRA_RESULTS)
        if (!results.isNullOrEmpty()) {
            inputText = results[0]
        }
    }

    // Initialize session
    LaunchedEffect(sessionId) {
        viewModel.loadSession(sessionId)
    }

    // Auto-scroll to bottom when new messages arrive or streaming updates
    LaunchedEffect(messages.size, streamingText) {
        if (messages.isNotEmpty() || streamingText.isNotEmpty()) {
            val targetIndex = if (streamingText.isNotEmpty()) messages.size else messages.size - 1
            if (targetIndex >= 0) {
                listState.animateScrollToItem(targetIndex)
            }
        }
    }

    // Message popup for long-press actions
    if (showMessagePopup != null) {
        val msg = showMessagePopup!!
        androidx.compose.material3.AlertDialog(
            onDismissRequest = { showMessagePopup = null },
            title = {
                Text(
                    text = "Message Actions",
                    color = NeonCyan,
                    fontWeight = FontWeight.Bold
                )
            },
            text = {
                Text(
                    text = msg.content.take(100) + if (msg.content.length > 100) "..." else "",
                    color = Color.White.copy(alpha = 0.7f),
                    fontSize = 13.sp,
                    maxLines = 3,
                    overflow = TextOverflow.Ellipsis
                )
            },
            confirmButton = {},
            dismissButton = {},
            containerColor = DarkCard
        )
        // Using a side-effect free approach: show buttons inline
        // This is handled by the DropdownMenu below instead
    }

    Scaffold(
        containerColor = AmoledBlack,
        topBar = {
            TopAppBar(
                title = {
                    if (isEditingTitle) {
                        OutlinedTextField(
                            value = editTitleText,
                            onValueChange = { editTitleText = it },
                            singleLine = true,
                            modifier = Modifier.fillMaxWidth(),
                            colors = androidx.compose.material3.OutlinedTextFieldDefaults.colors(
                                focusedTextColor = Color.White,
                                unfocusedTextColor = Color.White,
                                focusedBorderColor = NeonCyan,
                                unfocusedBorderColor = Color.White.copy(alpha = 0.3f),
                                cursorColor = NeonCyan
                            ),
                            textStyle = MaterialTheme.typography.titleMedium.copy(
                                fontWeight = FontWeight.SemiBold
                            )
                        )
                    } else {
                        Column {
                            Text(
                                text = sessionTitle ?: "Chat",
                                color = Color.White,
                                fontWeight = FontWeight.SemiBold,
                                fontSize = 16.sp,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                            Text(
                                text = currentModel,
                                color = NeonGreen,
                                fontSize = 11.sp
                            )
                        }
                    }
                },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Back",
                            tint = Color.White
                        )
                    }
                },
                actions = {
                    // Token count display
                    Text(
                        text = "${totalTokens} tok",
                        color = NeonPurple,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Medium,
                        modifier = Modifier.padding(end = 8.dp)
                    )

                    // Model selector
                    Box {
                        IconButton(onClick = { showModelMenu = true }) {
                            Icon(
                                imageVector = Icons.Filled.MoreVert,
                                contentDescription = "Model & Options",
                                tint = Color.White
                            )
                        }
                        DropdownMenu(
                            expanded = showModelMenu,
                            onDismissRequest = { showModelMenu = false },
                            modifier = Modifier.drawBehind { drawRect(DarkCard) }
                        ) {
                            Text(
                                text = "Select Model",
                                color = NeonCyan,
                                fontWeight = FontWeight.Bold,
                                fontSize = 13.sp,
                                modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)
                            )
                            listOf("glm/glm-5.2", "gpt-4", "gpt-3.5-turbo").forEach { model ->
                                DropdownMenuItem(
                                    text = {
                                        Row(verticalAlignment = Alignment.CenterVertically) {
                                            if (model == currentModel) {
                                                Box(
                                                    modifier = Modifier
                                                        .size(8.dp)
                                                        .drawBehind { drawCircle(NeonGreen) }
                                                )
                                                Spacer(modifier = Modifier.width(8.dp))
                                            }
                                            Text(
                                                text = model,
                                                color = if (model == currentModel) NeonGreen else Color.White
                                            )
                                        }
                                    },
                                    onClick = {
                                        viewModel.updateSessionConfig(
                                            model = model,
                                            temperature = temperature,
                                            maxTokens = maxTokens
                                        )
                                        showModelMenu = false
                                    }
                                )
                            }
                            // Settings option
                            DropdownMenuItem(
                                text = {
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        Icon(
                                            imageVector = Icons.Filled.KeyboardArrowDown,
                                            contentDescription = null,
                                            tint = NeonPurple,
                                            modifier = Modifier.size(18.dp)
                                        )
                                        Spacer(modifier = Modifier.width(8.dp))
                                        Text("Settings", color = NeonPurple)
                                    }
                                },
                                onClick = {
                                    showModelMenu = false
                                    showBottomSheet = true
                                }
                            )
                            // Export option
                            DropdownMenuItem(
                                text = {
                                    Text("Export Chat", color = NeonCyan)
                                },
                                onClick = {
                                    showModelMenu = false
                                    exportChatAsTxt(context, messages, sessionTitle ?: "chat")
                                }
                            )
                        }
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = AmoledBlack
                )
            )
        }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .imePadding()
        ) {
            // Chat message list
            LazyColumn(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth(),
                state = listState,
                contentPadding = androidx.compose.foundation.layout.PaddingValues(
                    horizontal = 12.dp,
                    vertical = 8.dp
                ),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                items(
                    items = messages,
                    key = { it.id }
                ) { message ->
                    ChatMessageBubble(
                        message = message,
                        onLongPress = { showMessagePopup = message }
                    )
                }

                // Streaming indicator and partial response
                if (isStreaming || streamingText.isNotEmpty()) {
                    item {
                        StreamingMessageBubble(
                            streamingText = streamingText,
                            isStreaming = isStreaming
                        )
                    }
                }
            }

            // Message action popup (replaces AlertDialog approach for better UX)
            showMessagePopup?.let { msg ->
                MessageActionPopup(
                    message = msg,
                    onCopy = {
                        clipboardManager.setText(AnnotatedString(msg.content))
                        showMessagePopup = null
                    },
                    onDelete = {
                        viewModel.deleteMessage(msg.id)
                        showMessagePopup = null
                    },
                    onRegenerate = {
                        viewModel.regenerateMessage(msg.id)
                        showMessagePopup = null
                    },
                    onDismiss = { showMessagePopup = null }
                )
            }

            // Bottom input bar
            BottomInputBar(
                inputText = inputText,
                onInputTextChange = { inputText = it },
                onSend = {
                    if (inputText.isNotBlank()) {
                        viewModel.sendMessage(inputText.trim())
                        inputText = ""
                    }
                },
                onVoiceInput = {
                    val intent = Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH).apply {
                        putExtra(
                            RecognizerIntent.EXTRA_LANGUAGE_MODEL,
                            RecognizerIntent.LANGUAGE_MODEL_FREE_FORM
                        )
                        putExtra(RecognizerIntent.EXTRA_PROMPT, "Speak your message...")
                    }
                    voiceInputLauncher.launch(intent)
                },
                isStreaming = isStreaming,
                onStop = { viewModel.stopStreaming() }
            )
        }
    }

    // Settings bottom sheet
    if (showBottomSheet) {
        SettingsBottomSheet(
            currentModel = currentModel,
            temperature = temperature,
            maxTokens = maxTokens,
            onUpdateConfig = { model, temp, tokens ->
                viewModel.updateSessionConfig(model, temp, tokens)
            },
            onDismiss = { showBottomSheet = false }
        )
    }
}

/**
 * Individual chat message bubble.
 */
@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun ChatMessageBubble(
    message: ChatMessageEntity,
    onLongPress: () -> Unit
) {
    val isUser = message.role == "user"

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .combinedClickable(
                onClick = {},
                onLongClick = onLongPress
            ),
        horizontalArrangement = if (isUser) Arrangement.End else Arrangement.Start
    ) {
        if (!isUser) {
            // AI icon
            Box(
                modifier = Modifier
                    .size(32.dp)
                    .drawBehind { drawCircle(NeonCyan.copy(alpha = 0.3f)) },
                contentAlignment = Alignment.Center
            ) {
                Text("AI", color = NeonCyan, fontSize = 11.sp, fontWeight = FontWeight.Bold)
            }
            Spacer(modifier = Modifier.width(8.dp))
        }

        Card(
            modifier = Modifier
                .fillMaxWidth(fraction = 0.82f),
            colors = CardDefaults.cardColors(
                containerColor = if (isUser) {
                    NeonPurple.copy(alpha = 0.15f)
                } else {
                    DarkCard
                }
            ),
            shape = RoundedCornerShape(
                topStart = if (isUser) 16.dp else 4.dp,
                topEnd = if (isUser) 4.dp else 16.dp,
                bottomStart = 16.dp,
                bottomEnd = 16.dp
            )
        ) {
            Column(
                modifier = Modifier.padding(12.dp)
            ) {
                // Render message content with markdown support
                val renderedContent = renderMarkdown(message.content)
                Text(
                    text = renderedContent,
                    color = Color.White,
                    fontSize = 14.sp,
                    lineHeight = 20.sp
                )

                // Code blocks
                val codeBlocks = extractCodeBlocks(message.content)
                codeBlocks.forEach { codeBlock ->
                    Spacer(modifier = Modifier.height(8.dp))
                    CodeBlockView(code = codeBlock)
                }

                // Timestamp and token count
                Spacer(modifier = Modifier.height(6.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text(
                        text = formatTimestamp(message.timestamp),
                        color = Color.White.copy(alpha = 0.3f),
                        fontSize = 10.sp
                    )
                    if (message.tokenCount > 0) {
                        Text(
                            text = "${message.tokenCount} tokens",
                            color = NeonPurple.copy(alpha = 0.6f),
                            fontSize = 10.sp
                        )
                    }
                }
            }
        }

        if (isUser) {
            Spacer(modifier = Modifier.width(8.dp))
            // User icon
            Box(
                modifier = Modifier
                    .size(32.dp)
                    .drawBehind { drawCircle(NeonPurple.copy(alpha = 0.3f)) },
                contentAlignment = Alignment.Center
            ) {
                Text("U", color = NeonPurple, fontSize = 12.sp, fontWeight = FontWeight.Bold)
            }
        }
    }
}

/**
 * Streaming message bubble showing the AI's partial response.
 */
@Composable
private fun StreamingMessageBubble(
    streamingText: String,
    isStreaming: Boolean
) {
    // Glowing dot animation
    val infiniteTransition = rememberInfiniteTransition(label = "streamGlow")
    val glowAlpha by infiniteTransition.animateFloat(
        initialValue = 0.3f,
        targetValue = 1.0f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 800),
            repeatMode = RepeatMode.Reverse
        ),
        label = "glowAlpha"
    )

    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.Start
    ) {
        // AI icon
        Box(
            modifier = Modifier
                .size(32.dp)
                .drawBehind { drawCircle(NeonCyan.copy(alpha = 0.3f)) },
            contentAlignment = Alignment.Center
        ) {
            Text("AI", color = NeonCyan, fontSize = 11.sp, fontWeight = FontWeight.Bold)
        }

        Spacer(modifier = Modifier.width(8.dp))

        Card(
            modifier = Modifier.fillMaxWidth(fraction = 0.82f),
            colors = CardDefaults.cardColors(containerColor = DarkCard),
            shape = RoundedCornerShape(
                topStart = 4.dp,
                topEnd = 16.dp,
                bottomStart = 16.dp,
                bottomEnd = 16.dp
            )
        ) {
            Column(modifier = Modifier.padding(12.dp)) {
                if (streamingText.isNotEmpty()) {
                    val renderedContent = renderMarkdown(streamingText)
                    Text(
                        text = renderedContent,
                        color = Color.White,
                        fontSize = 14.sp,
                        lineHeight = 20.sp
                    )

                    // Code blocks in streaming text
                    val codeBlocks = extractCodeBlocks(streamingText)
                    codeBlocks.forEach { codeBlock ->
                        Spacer(modifier = Modifier.height(8.dp))
                        CodeBlockView(code = codeBlock)
                    }
                }

                if (isStreaming) {
                    Spacer(modifier = Modifier.height(8.dp))
                    Row(
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        // Glowing dot
                        Box(
                            modifier = Modifier
                                .size(8.dp)
                                .drawBehind {
                                    drawCircle(
                                        NeonPurple.copy(alpha = glowAlpha)
                                    )
                                }
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        GlitchText(
                            text = "Thinking...",
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Medium,
                            color = NeonPurple.copy(alpha = glowAlpha)
                        )
                    }
                }
            }
        }
    }
}

/**
 * Code block view with JetBrains Mono font and copy button.
 */
@Composable
private fun CodeBlockView(code: String) {
    val clipboardManager = LocalClipboardManager.current

    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = AmoledBlack
        ),
        shape = RoundedCornerShape(8.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(8.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.Top
        ) {
            Text(
                text = code.trimIndent(),
                color = NeonGreen,
                fontSize = 12.sp,
                fontFamily = FontFamily.Monospace,
                modifier = Modifier.weight(1f),
                lineHeight = 16.sp
            )
            IconButton(
                onClick = { clipboardManager.setText(AnnotatedString(code)) },
                modifier = Modifier.size(24.dp)
            ) {
                Icon(
                    imageVector = Icons.Filled.ContentCopy,
                    contentDescription = "Copy Code",
                    tint = Color.White.copy(alpha = 0.5f),
                    modifier = Modifier.size(14.dp)
                )
            }
        }
    }
}

/**
 * Message action popup for Copy, Delete, Regenerate.
 */
@Composable
private fun MessageActionPopup(
    message: ChatMessageEntity,
    onCopy: () -> Unit,
    onDelete: () -> Unit,
    onRegenerate: () -> Unit,
    onDismiss: () -> Unit
) {
    androidx.compose.material3.AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(
                text = "Message Actions",
                color = NeonCyan,
                fontWeight = FontWeight.Bold
            )
        },
        text = {
            Column {
                // Copy
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    IconButton(onClick = onCopy) {
                        Icon(
                            imageVector = Icons.Filled.ContentCopy,
                            contentDescription = "Copy",
                            tint = NeonCyan,
                            modifier = Modifier.size(20.dp)
                        )
                    }
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Copy", color = Color.White)
                }
                // Delete
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    IconButton(onClick = onDelete) {
                        Icon(
                            imageVector = Icons.Filled.Delete,
                            contentDescription = "Delete",
                            tint = Color.Red,
                            modifier = Modifier.size(20.dp)
                        )
                    }
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Delete", color = Color.Red)
                }
                // Regenerate (only for AI messages)
                if (message.role == "assistant") {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        IconButton(onClick = onRegenerate) {
                            Icon(
                                imageVector = Icons.Filled.Refresh,
                                contentDescription = "Regenerate",
                                tint = NeonPurple,
                                modifier = Modifier.size(20.dp)
                            )
                        }
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Regenerate", color = NeonPurple)
                    }
                }
            }
        },
        confirmButton = {},
        containerColor = DarkCard
    )
}

/**
 * Bottom input bar with text field, send button, voice input, and stop button.
 */
@Composable
private fun BottomInputBar(
    inputText: String,
    onInputTextChange: (String) -> Unit,
    onSend: () -> Unit,
    onVoiceInput: () -> Unit,
    isStreaming: Boolean,
    onStop: () -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = DarkCard),
        shape = RoundedCornerShape(topStart = 16.dp, topEnd = 16.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 12.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Voice input button
            IconButton(
                onClick = onVoiceInput,
                modifier = Modifier.size(40.dp)
            ) {
                Icon(
                    imageVector = Icons.Filled.Mic,
                    contentDescription = "Voice Input",
                    tint = NeonCyan,
                    modifier = Modifier.size(22.dp)
                )
            }

            Spacer(modifier = Modifier.width(4.dp))

            // Text input field
            OutlinedTextField(
                value = inputText,
                onValueChange = onInputTextChange,
                modifier = Modifier.weight(1f),
                placeholder = {
                    Text(
                        "Type a message...",
                        color = Color.White.copy(alpha = 0.3f)
                    )
                },
                colors = androidx.compose.material3.OutlinedTextFieldDefaults.colors(
                    focusedTextColor = Color.White,
                    unfocusedTextColor = Color.White,
                    focusedBorderColor = NeonCyan,
                    unfocusedBorderColor = Color.White.copy(alpha = 0.2f),
                    cursorColor = NeonCyan,
                    focusedContainerColor = Color.Transparent,
                    unfocusedContainerColor = Color.Transparent
                ),
                shape = RoundedCornerShape(24.dp),
                maxLines = 4,
                textStyle = MaterialTheme.typography.bodyMedium
            )

            Spacer(modifier = Modifier.width(8.dp))

            // Send / Stop button
            if (isStreaming) {
                // Stop button
                IconButton(
                    onClick = onStop,
                    modifier = Modifier
                        .size(44.dp)
                        .drawBehind {
                            drawCircle(Color.Red.copy(alpha = 0.2f))
                        }
                ) {
                    Icon(
                        imageVector = Icons.Filled.Stop,
                        contentDescription = "Stop",
                        tint = Color.Red,
                        modifier = Modifier.size(24.dp)
                    )
                }
            } else {
                // Send button
                IconButton(
                    onClick = onSend,
                    enabled = inputText.isNotBlank(),
                    modifier = Modifier
                        .size(44.dp)
                        .drawBehind {
                            if (inputText.isNotBlank()) {
                                drawCircle(NeonPurple.copy(alpha = 0.8f))
                            } else {
                                drawCircle(Color.White.copy(alpha = 0.1f))
                            }
                        }
                ) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.Send,
                        contentDescription = "Send",
                        tint = if (inputText.isNotBlank()) Color.White else Color.White.copy(alpha = 0.3f),
                        modifier = Modifier.size(22.dp)
                    )
                }
            }
        }
    }
}

/**
 * Settings bottom sheet for model, temperature, and max tokens.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun SettingsBottomSheet(
    currentModel: String,
    temperature: Double,
    maxTokens: Int,
    onUpdateConfig: (String, Double, Int) -> Unit,
    onDismiss: () -> Unit
) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    var selectedModel by remember { mutableStateOf(currentModel) }
    var tempSlider by remember { mutableStateOf(temperature.toFloat()) }
    var maxTokensInput by remember { mutableStateOf(maxTokens.toString()) }

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        containerColor = DarkCard,
        shape = RoundedCornerShape(topStart = 24.dp, topEnd = 24.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 24.dp, vertical = 16.dp)
        ) {
            Text(
                text = "Session Settings",
                color = NeonCyan,
                fontWeight = FontWeight.Bold,
                fontSize = 18.sp
            )

            Spacer(modifier = Modifier.height(24.dp))

            // Model selector
            Text(
                text = "Model",
                color = Color.White,
                fontWeight = FontWeight.Medium,
                fontSize = 14.sp
            )
            Spacer(modifier = Modifier.height(8.dp))
            listOf("glm/glm-5.2", "gpt-4", "gpt-3.5-turbo").forEach { model ->
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 4.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    androidx.compose.material3.RadioButton(
                        selected = model == selectedModel,
                        onClick = { selectedModel = model },
                        colors = androidx.compose.material3.RadioButtonDefaults.colors(
                            selectedColor = NeonPurple,
                            unselectedColor = Color.White.copy(alpha = 0.4f)
                        )
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = model,
                        color = if (model == selectedModel) NeonPurple else Color.White.copy(alpha = 0.7f),
                        fontSize = 14.sp
                    )
                }
            }

            Spacer(modifier = Modifier.height(24.dp))

            // Temperature slider
            Text(
                text = "Temperature: ${String.format("%.2f", tempSlider.toDouble())}",
                color = Color.White,
                fontWeight = FontWeight.Medium,
                fontSize = 14.sp
            )
            Spacer(modifier = Modifier.height(8.dp))
            Slider(
                value = tempSlider,
                onValueChange = { tempSlider = it },
                valueRange = 0f..2f,
                colors = SliderDefaults.colors(
                    thumbColor = NeonPurple,
                    activeTrackColor = NeonPurple,
                    inactiveTrackColor = Color.White.copy(alpha = 0.2f)
                )
            )

            Spacer(modifier = Modifier.height(24.dp))

            // Max tokens input
            Text(
                text = "Max Tokens",
                color = Color.White,
                fontWeight = FontWeight.Medium,
                fontSize = 14.sp
            )
            Spacer(modifier = Modifier.height(8.dp))
            OutlinedTextField(
                value = maxTokensInput,
                onValueChange = { maxTokensInput = it },
                singleLine = true,
                modifier = Modifier.fillMaxWidth(),
                colors = androidx.compose.material3.OutlinedTextFieldDefaults.colors(
                    focusedTextColor = Color.White,
                    unfocusedTextColor = Color.White,
                    focusedBorderColor = NeonCyan,
                    unfocusedBorderColor = Color.White.copy(alpha = 0.3f),
                    cursorColor = NeonCyan
                ),
                shape = RoundedCornerShape(8.dp)
            )

            Spacer(modifier = Modifier.height(32.dp))

            // Apply button
            Button(
                onClick = {
                    val tokens = maxTokensInput.toIntOrNull() ?: 4096
                    onUpdateConfig(selectedModel, tempSlider.toDouble(), tokens)
                    onDismiss()
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(48.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = NeonPurple,
                    contentColor = Color.White
                ),
                shape = RoundedCornerShape(12.dp)
            ) {
                Text(
                    text = "Apply Settings",
                    fontWeight = FontWeight.Bold,
                    fontSize = 14.sp
                )
            }

            Spacer(modifier = Modifier.height(16.dp))
        }
    }
}

// ============================================================
// Markdown & Text Processing Utilities
// ============================================================

/**
 * Renders markdown-like text into an AnnotatedString.
 * Supports: bold (**text**), italic (*text*), inline code (`code`),
 * lists (- item), headers (# ## ###).
 */
private fun renderMarkdown(text: String): AnnotatedString {
    // Strip code blocks first - they are rendered separately
    val textWithoutCodeBlocks = text.replace(Regex("```[\\s\\S]*?```"), "")

    return buildAnnotatedString {
        val lines = textWithoutCodeBlocks.split("\n")
        var isFirstLine = true

        for (line in lines) {
            if (!isFirstLine) append("\n")
            isFirstLine = false

            // Headers
            val headerMatch = Regex("^(#{1,3})\\s+(.+)$").find(line)
            if (headerMatch != null) {
                val level = headerMatch.groupValues[1].length
                val headerText = headerMatch.groupValues[2]
                pushStyle(
                    SpanStyle(
                        fontWeight = FontWeight.Bold,
                        fontSize = when (level) {
                            1 -> 20.sp
                            2 -> 18.sp
                            else -> 16.sp
                        },
                        color = NeonCyan
                    )
                )
                append(headerText)
                pop()
                continue
            }

            // List items
            val listMatch = Regex("^\\s*[-*]\\s+(.+)$").find(line)
            if (listMatch != null) {
                append("• ")
                appendFormattedText(listMatch.groupValues[1])
                continue
            }

            // Regular line with inline formatting
            appendFormattedText(line)
        }
    }
}

/**
 * Appends text with inline formatting: bold, italic, inline code.
 */
private fun AnnotatedString.Builder.appendFormattedText(text: String) {
    val pattern = Regex("""(\*\*(.+?)\*\*|\*(.+?)\*|`(.+?)`)""")
    var lastIndex = 0

    for (match in pattern.findAll(text)) {
        // Append text before this match
        if (match.range.first > lastIndex) {
            append(text.substring(lastIndex, match.range.first))
        }

        val fullMatch = match.groupValues[0]
        when {
            fullMatch.startsWith("**") -> {
                pushStyle(SpanStyle(fontWeight = FontWeight.Bold, color = Color.White))
                append(match.groupValues[2])
                pop()
            }
            fullMatch.startsWith("*") && !fullMatch.startsWith("**") -> {
                pushStyle(SpanStyle(fontStyle = androidx.compose.ui.text.font.FontStyle.Italic))
                append(match.groupValues[3])
                pop()
            }
            fullMatch.startsWith("`") -> {
                pushStyle(
                    SpanStyle(
                        fontFamily = FontFamily.Monospace,
                        background = Color.White.copy(alpha = 0.1f),
                        color = NeonGreen
                    )
                )
                append(match.groupValues[4])
                pop()
            }
        }

        lastIndex = match.range.last + 1
    }

    // Append remaining text
    if (lastIndex < text.length) {
        append(text.substring(lastIndex))
    }
}

/**
 * Extracts code blocks from text (content between ``` markers).
 */
private fun extractCodeBlocks(text: String): List<String> {
    val pattern = Regex("```[\\s\\S]*?```")
    return pattern.findAll(text).map { match ->
        match.value.removeSurrounding("```").trim()
    }.toList()
}

/**
 * Formats a timestamp into a readable time string.
 */
private fun formatTimestamp(timestamp: Long): String {
    val sdf = java.text.SimpleDateFormat("HH:mm", java.util.Locale.getDefault())
    return sdf.format(java.util.Date(timestamp))
}

/**
 * Exports the chat messages as a TXT file and shares via intent.
 */
private fun exportChatAsTxt(
    context: android.content.Context,
    messages: List<ChatMessageEntity>,
    title: String
) {
    val sb = StringBuilder()
    sb.appendLine("=== $title ===")
    sb.appendLine()
    for (msg in messages) {
        val role = if (msg.role == "user") "You" else "AI"
        val time = formatTimestamp(msg.timestamp)
        sb.appendLine("[$role] ($time)")
        sb.appendLine(msg.content)
        sb.appendLine()
    }

    // Write to cache and share
    try {
        val cacheDir = context.cacheDir
        val file = java.io.File(cacheDir, "${title.replace(" ", "_")}.txt")
        file.writeText(sb.toString())
        val uri = androidx.core.content.FileProvider.getUriForFile(
            context,
            "${context.packageName}.fileprovider",
            file
        )
        val shareIntent = Intent(Intent.ACTION_SEND).apply {
            type = "text/plain"
            putExtra(Intent.EXTRA_STREAM, uri)
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        }
        context.startActivity(Intent.createChooser(shareIntent, "Export Chat"))
    } catch (e: Exception) {
        // Fallback: share as text
        val shareIntent = Intent(Intent.ACTION_SEND).apply {
            type = "text/plain"
            putExtra(Intent.EXTRA_TEXT, sb.toString())
        }
        context.startActivity(Intent.createChooser(shareIntent, "Export Chat"))
    }
}
