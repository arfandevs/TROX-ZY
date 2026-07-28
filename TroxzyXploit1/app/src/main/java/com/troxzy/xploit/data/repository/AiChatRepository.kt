package com.troxzy.xploit.data.repository

import com.troxzy.xploit.data.remote.AiApiService
import com.troxzy.xploit.data.remote.ChatMessageDto
import kotlinx.coroutines.flow.Flow

class AiChatRepository(private val aiApiService: AiApiService) {

    fun streamChat(
        messages: List<ChatMessageDto>,
        model: String = "glm/glm-5.2",
        temperature: Double = 0.7,
        maxTokens: Int = 4096
    ): Flow<String> {
        return aiApiService.streamChat(messages, model, temperature, maxTokens)
    }

    suspend fun sendChatNonStreaming(
        messages: List<ChatMessageDto>,
        model: String = "glm/glm-5.2",
        temperature: Double = 0.7,
        maxTokens: Int = 4096
    ): String {
        return aiApiService.sendChatNonStreaming(messages, model, temperature, maxTokens)
    }
}
