package com.troxzy.xploit.data.remote

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.isActive
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import com.google.gson.Gson
import com.troxzy.xploit.data.remote.ChatRequest
import com.troxzy.xploit.data.remote.ChatMessageDto
import timber.log.Timber

class AiApiService(private val client: OkHttpClient) {

    companion object {
        private const val BASE_URL = "https://api.freetheai.xyz/v1/chat/completions"
        private const val API_KEY = "sta_4bc4d021bc423c04b745194a24e382e9cdf4403f37f06154"
        private const val CONTENT_TYPE = "application/json"
    }

    private val gson = Gson()

    fun streamChat(
        messages: List<ChatMessageDto>,
        model: String = "glm/glm-5.2",
        temperature: Double = 0.7,
        maxTokens: Int = 4096
    ): Flow<String> = flow {
        val request = ChatRequest(
            model = model,
            messages = messages,
            stream = true,
            temperature = temperature,
            maxTokens = maxTokens
        )
        val jsonBody = gson.toJson(request)
        val requestBody = jsonBody.toRequestBody(CONTENT_TYPE.toMediaType())

        val httpRequest = Request.Builder()
            .url(BASE_URL)
            .addHeader("Authorization", "Bearer $API_KEY")
            .addHeader("Content-Type", CONTENT_TYPE)
            .addHeader("Accept", "text/event-stream")
            .post(requestBody)
            .build()

        var retryCount = 0
        val maxRetries = 3

        while (retryCount < maxRetries) {
            try {
                val response = withContext(Dispatchers.IO) { client.newCall(httpRequest).execute() }
                if (!response.isSuccessful) {
                    retryCount++
                    if (retryCount >= maxRetries) {
                        emit("[ERROR] Server returned ${response.code}: ${response.body?.string()?.take(200)}")
                        break
                    }
                    kotlinx.coroutines.delay(1000L * retryCount)
                    continue
                }

                val body = response.body ?: run {
                    retryCount++
                    continue
                }
                val reader = body.byteStream().bufferedReader()

                try {
                    var currentData = StringBuilder()
                    while (withContext(Dispatchers.IO) { isActive }) {
                        val line = withContext(Dispatchers.IO) { reader.readLine() }
                        if (line == null) break

                        if (line.startsWith("data: ")) {
                            val data = line.removePrefix("data: ").trim()
                            if (data == "[DONE]") {
                                emit("[DONE]")
                                break
                            }
                            try {
                                val chatResponse = gson.fromJson(data, ChatResponse::class.java)
                                val content = chatResponse.choices?.firstOrNull()?.delta?.content
                                if (content != null && content.isNotEmpty()) {
                                    emit(content)
                                }
                            } catch (e: Exception) {
                                Timber.d(e, "SSE parse error for line: $data")
                            }
                        } else if (line.isNotEmpty()) {
                            currentData.append(line).append("\n")
                        }
                    }
                } finally {
                    withContext(Dispatchers.IO) { reader.close() }
                    withContext(Dispatchers.IO) { body.close() }
                }
                break
            } catch (e: Exception) {
                retryCount++
                Timber.e(e, "Stream chat error, retry $retryCount/$maxRetries")
                if (retryCount >= maxRetries) {
                    emit("[ERROR] ${e.message ?: "Connection failed"}")
                    break
                }
                kotlinx.coroutines.delay(1000L * retryCount)
            }
        }
    }.flowOn(Dispatchers.IO)

    suspend fun sendChatNonStreaming(
        messages: List<ChatMessageDto>,
        model: String = "glm/glm-5.2",
        temperature: Double = 0.7,
        maxTokens: Int = 4096
    ): String = withContext(Dispatchers.IO) {
        val request = ChatRequest(
            model = model,
            messages = messages,
            stream = false,
            temperature = temperature,
            maxTokens = maxTokens
        )
        val jsonBody = gson.toJson(request)
        val requestBody = jsonBody.toRequestBody(CONTENT_TYPE.toMediaType())

        val httpRequest = Request.Builder()
            .url(BASE_URL)
            .addHeader("Authorization", "Bearer $API_KEY")
            .addHeader("Content-Type", CONTENT_TYPE)
            .post(requestBody)
            .build()

        var retryCount = 0
        val maxRetries = 3

        while (retryCount < maxRetries) {
            try {
                val response = client.newCall(httpRequest).execute()
                if (!response.isSuccessful) {
                    retryCount++
                    if (retryCount >= maxRetries) {
                        return@withContext "[ERROR] Server returned ${response.code}"
                    }
                    kotlinx.coroutines.delay(1000L * retryCount)
                    continue
                }
                val responseBody = response.body?.string() ?: return@withContext "[ERROR] Empty response"
                val chatResponse = gson.fromJson(responseBody, NonStreamingChatResponse::class.java)
                return@withContext chatResponse.choices?.firstOrNull()?.message?.content ?: "[ERROR] No content in response"
            } catch (e: Exception) {
                retryCount++
                if (retryCount >= maxRetries) {
                    return@withContext "[ERROR] ${e.message ?: "Connection failed"}"
                }
                kotlinx.coroutines.delay(1000L * retryCount)
            }
        }
        return@withContext "[ERROR] Max retries exceeded"
    }
}
