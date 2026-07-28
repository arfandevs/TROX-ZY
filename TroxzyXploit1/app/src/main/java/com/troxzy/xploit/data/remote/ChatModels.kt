package com.troxzy.xploit.data.remote

import com.google.gson.annotations.SerializedName

data class ChatRequest(
    val model: String = "glm/glm-5.2",
    val messages: List<ChatMessageDto>,
    val stream: Boolean = true,
    val temperature: Double = 0.7,
    @SerializedName("max_tokens")
    val maxTokens: Int = 4096
)

data class ChatMessageDto(
    val role: String,
    val content: String
)

data class ChatResponse(
    val id: String? = null,
    val choices: List<ChoiceDto>? = null,
    val usage: UsageDto? = null
)

data class ChoiceDto(
    val index: Int = 0,
    val delta: DeltaDto? = null,
    val message: ChatMessageDto? = null,
    @SerializedName("finish_reason")
    val finishReason: String? = null
)

data class DeltaDto(
    val content: String? = null,
    val role: String? = null
)

data class UsageDto(
    @SerializedName("prompt_tokens")
    val promptTokens: Int = 0,
    @SerializedName("completion_tokens")
    val completionTokens: Int = 0,
    @SerializedName("total_tokens")
    val totalTokens: Int = 0
)

data class NonStreamingChatResponse(
    val id: String? = null,
    val choices: List<NonStreamingChoice>? = null,
    val usage: UsageDto? = null
)

data class NonStreamingChoice(
    val index: Int = 0,
    val message: ChatMessageDto? = null,
    @SerializedName("finish_reason")
    val finishReason: String? = null
)
