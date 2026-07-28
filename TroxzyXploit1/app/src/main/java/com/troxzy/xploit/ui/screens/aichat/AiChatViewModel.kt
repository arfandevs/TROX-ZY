package com.troxzy.xploit.ui.screens.aichat

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.troxzy.xploit.data.local.dao.ChatSessionDao
import com.troxzy.xploit.data.local.entity.ChatSessionEntity
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

/**
 * ViewModel that manages chat sessions.
 * Handles creating, deleting, and renaming AI chat sessions.
 */
@HiltViewModel
class AiChatViewModel @Inject constructor(
    private val chatSessionDao: ChatSessionDao
) : ViewModel() {

    /**
     * All chat sessions as a StateFlow, ordered by most recent update.
     */
    val sessions: StateFlow<List<ChatSessionEntity>> = chatSessionDao
        .getAllSessions()
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = emptyList()
        )

    /**
     * Creates a new chat session with default configuration.
     * Default model: "glm/glm-5.2", temperature: 0.7, maxTokens: 4096.
     */
    fun createSession() {
        viewModelScope.launch {
            val newSession = ChatSessionEntity(
                title = "New Chat ${System.currentTimeMillis() % 10000}",
                modelName = "glm/glm-5.2",
                temperature = 0.7,
                maxTokens = 4096,
                lastMessagePreview = null,
                createdAt = System.currentTimeMillis(),
                updatedAt = System.currentTimeMillis()
            )
            chatSessionDao.insertSession(newSession)
        }
    }

    /**
     * Deletes a chat session by its ID.
     * The DAO should handle cascading deletion of associated messages.
     *
     * @param sessionId The ID of the session to delete.
     */
    fun deleteSession(sessionId: Long) {
        viewModelScope.launch {
            chatSessionDao.deleteSession(sessionId)
        }
    }

    /**
     * Renames a chat session by updating its title.
     *
     * @param sessionId The ID of the session to rename.
     * @param newTitle The new title for the session.
     */
    fun renameSession(sessionId: Long, newTitle: String) {
        viewModelScope.launch {
            chatSessionDao.updateSessionTitle(
                sessionId = sessionId,
                newTitle = newTitle,
                updatedAt = System.currentTimeMillis()
            )
        }
    }
}
