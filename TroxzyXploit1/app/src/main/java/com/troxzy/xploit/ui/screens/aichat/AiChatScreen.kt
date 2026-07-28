package com.troxzy.xploit.ui.screens.aichat

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
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.SmartToy
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import com.troxzy.xploit.data.local.entity.ChatSessionEntity
import com.troxzy.xploit.ui.components.BottomNavBar
import com.troxzy.xploit.ui.components.GlitchText
import com.troxzy.xploit.ui.theme.AmoledBlack
import com.troxzy.xploit.ui.theme.DarkCard
import com.troxzy.xploit.ui.theme.NeonCyan
import com.troxzy.xploit.ui.theme.NeonGreen
import com.troxzy.xploit.ui.theme.NeonPurple

/**
 * Main chat session list screen.
 * Displays all AI chat sessions with ability to create, delete, and rename sessions.
 */
@OptIn(ExperimentalMaterial3Api::class, ExperimentalFoundationApi::class)
@Composable
fun AiChatScreen(
    onOpenSession: (Long) -> Unit,
    viewModel: AiChatViewModel = hiltViewModel()
) {
    val sessions by viewModel.sessions.collectAsState()
    var expandedMenuSessionId by remember { mutableStateOf<Long?>(null) }
    var showRenameDialog by remember { mutableStateOf<ChatSessionEntity?>(null) }
    var renameText by remember { mutableStateOf("") }

    Scaffold(
        containerColor = AmoledBlack,
        topBar = {
            TopAppBar(
                title = {
                    GlitchText(
                        text = "TROXZY AI BRAIN",
                        fontSize = 20.sp,
                        fontWeight = FontWeight.Bold,
                        color = NeonCyan
                    )
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = AmoledBlack,
                    titleContentColor = NeonCyan
                )
            )
        },
        floatingActionButton = {
            FloatingActionButton(
                onClick = { viewModel.createSession() },
                containerColor = NeonPurple,
                contentColor = Color.White
            ) {
                Icon(
                    imageVector = Icons.Filled.Add,
                    contentDescription = "New Chat Session",
                    modifier = Modifier.size(24.dp)
                )
            }
        },
        bottomBar = {
            BottomNavBar()
        }
    ) { paddingValues ->
        if (sessions.isEmpty()) {
            // Empty state
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(paddingValues),
                contentAlignment = Alignment.Center
            ) {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center
                ) {
                    Icon(
                        imageVector = Icons.Filled.SmartToy,
                        contentDescription = null,
                        modifier = Modifier.size(64.dp),
                        tint = NeonPurple.copy(alpha = 0.5f)
                    )
                    Spacer(modifier = Modifier.height(16.dp))
                    GlitchText(
                        text = "No sessions yet",
                        fontSize = 22.sp,
                        fontWeight = FontWeight.Bold,
                        color = NeonPurple
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = "Tap + to create your first AI chat session",
                        color = Color.White.copy(alpha = 0.5f),
                        fontSize = 14.sp,
                        textAlign = TextAlign.Center
                    )
                }
            }
        } else {
            // Session list
            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(paddingValues),
                contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                items(
                    items = sessions,
                    key = { it.id }
                ) { session ->
                    SessionCard(
                        session = session,
                        onClick = { onOpenSession(session.id) },
                        onLongPress = { expandedMenuSessionId = session.id },
                        expandedMenu = expandedMenuSessionId == session.id,
                        onDismissMenu = { expandedMenuSessionId = null },
                        onDelete = {
                            viewModel.deleteSession(session.id)
                            expandedMenuSessionId = null
                        },
                        onRename = {
                            renameText = session.title
                            showRenameDialog = session
                            expandedMenuSessionId = null
                        }
                    )
                }
            }
        }
    }

    // Rename dialog
    if (showRenameDialog != null) {
        val sessionToRename = showRenameDialog!!
        androidx.compose.material3.AlertDialog(
            onDismissRequest = { showRenameDialog = null },
            title = {
                Text(
                    text = "Rename Session",
                    color = NeonCyan,
                    fontWeight = FontWeight.Bold
                )
            },
            text = {
                androidx.compose.material3.OutlinedTextField(
                    value = renameText,
                    onValueChange = { renameText = it },
                    label = { Text("Session Title") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth(),
                    colors = androidx.compose.material3.OutlinedTextFieldDefaults.colors(
                        focusedTextColor = Color.White,
                        unfocusedTextColor = Color.White,
                        focusedBorderColor = NeonCyan,
                        unfocusedBorderColor = Color.White.copy(alpha = 0.3f),
                        cursorColor = NeonCyan
                    )
                )
            },
            confirmButton = {
                androidx.compose.material3.TextButton(
                    onClick = {
                        if (renameText.isNotBlank()) {
                            viewModel.renameSession(sessionToRename.id, renameText.trim())
                        }
                        showRenameDialog = null
                    }
                ) {
                    Text("Rename", color = NeonPurple, fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                androidx.compose.material3.TextButton(
                    onClick = { showRenameDialog = null }
                ) {
                    Text("Cancel", color = Color.White.copy(alpha = 0.6f))
                }
            },
            containerColor = DarkCard
        )
    }
}

/**
 * Individual session card composable.
 */
@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun SessionCard(
    session: ChatSessionEntity,
    onClick: () -> Unit,
    onLongPress: () -> Unit,
    expandedMenu: Boolean,
    onDismissMenu: () -> Unit,
    onDelete: () -> Unit,
    onRename: () -> Unit
) {
    // Subtle glow animation for the card border
    val infiniteTransition = rememberInfiniteTransition(label = "cardGlow")
    val glowAlpha by infiniteTransition.animateFloat(
        initialValue = 0.3f,
        targetValue = 0.7f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 2000),
            repeatMode = RepeatMode.Reverse
        ),
        label = "glowAlpha"
    )

    Box(modifier = Modifier.fillMaxWidth()) {
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .combinedClickable(
                    onClick = onClick,
                    onLongClick = onLongPress
                ),
            colors = CardDefaults.cardColors(
                containerColor = DarkCard
            ),
            elevation = CardDefaults.cardElevation(
                defaultElevation = 4.dp
            )
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Session icon
                Icon(
                    imageVector = Icons.Filled.SmartToy,
                    contentDescription = null,
                    modifier = Modifier.size(40.dp),
                    tint = NeonPurple.copy(alpha = glowAlpha)
                )

                Spacer(modifier = Modifier.width(12.dp))

                // Session info
                Column(
                    modifier = Modifier.weight(1f)
                ) {
                    Text(
                        text = session.title,
                        color = Color.White,
                        fontSize = 16.sp,
                        fontWeight = FontWeight.SemiBold,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = session.lastMessagePreview ?: "No messages yet",
                        color = Color.White.copy(alpha = 0.5f),
                        fontSize = 13.sp,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    Spacer(modifier = Modifier.height(6.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text(
                            text = session.modelName,
                            color = NeonGreen,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Medium
                        )
                        Text(
                            text = formatDate(session.updatedAt),
                            color = Color.White.copy(alpha = 0.4f),
                            fontSize = 11.sp
                        )
                    }
                }
            }
        }

        // Dropdown menu for long-press actions
        DropdownMenu(
            expanded = expandedMenu,
            onDismissRequest = onDismissMenu,
            modifier = Modifier
                .background(DarkCard)
        ) {
            DropdownMenuItem(
                text = {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = Icons.Filled.Edit,
                            contentDescription = null,
                            tint = NeonCyan,
                            modifier = Modifier.size(18.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Rename", color = Color.White)
                    }
                },
                onClick = onRename
            )
            DropdownMenuItem(
                text = {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = Icons.Filled.Delete,
                            contentDescription = null,
                            tint = Color.Red,
                            modifier = Modifier.size(18.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Delete", color = Color.Red)
                    }
                },
                onClick = onDelete
            )
        }
    }
}

/**
 * Formats a timestamp into a human-readable date string.
 */
private fun formatDate(timestamp: Long): String {
    val sdf = java.text.SimpleDateFormat("MMM dd, HH:mm", java.util.Locale.getDefault())
    return sdf.format(java.util.Date(timestamp))
}

/**
 * Extension modifier to set background color on DropdownMenu.
 */
private fun Modifier.background(color: Color): Modifier =
    this.then(androidx.compose.ui.draw.drawBehind {
        drawRect(color)
    })
