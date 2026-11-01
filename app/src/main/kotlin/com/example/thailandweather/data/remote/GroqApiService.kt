package com.example.thailandweather.data.remote

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import retrofit2.Response
import retrofit2.http.Body
import retrofit2.http.GET
import retrofit2.http.Header
import retrofit2.http.POST

@Serializable
data class GroqChatRequest(
    val model: String,
    val messages: List<GroqMessage>,
    val temperature: Double = 0.2,
    @SerialName("max_tokens") val maxTokens: Int? = 1500,
    @SerialName("response_format") val responseFormat: GroqResponseFormat? = null
)

@Serializable
data class GroqMessage(
    val role: String,
    val content: String
)

@Serializable
data class GroqResponseFormat(
    val type: String = "json_object"
)

@Serializable
data class GroqChatResponse(
    val id: String? = null,
    val model: String? = null,
    val choices: List<GroqChoice> = emptyList()
)

@Serializable
data class GroqChoice(
    val index: Int = 0,
    val message: GroqMessage? = null,
    @SerialName("finish_reason") val finishReason: String? = null
)

@Serializable
data class GroqModelsResponse(
    val data: List<GroqModelData> = emptyList()
)

@Serializable
data class GroqModelData(
    val id: String,
    val active: Boolean? = true
)

@Serializable
data class GroqErrorResponse(
    val error: GroqErrorDetail? = null
)

@Serializable
data class GroqErrorDetail(
    val message: String? = null,
    val type: String? = null,
    val code: String? = null
)

interface GroqApiService {
    @POST("openai/v1/chat/completions")
    suspend fun createChatCompletion(
        @Header("Authorization") authorization: String,
        @Body request: GroqChatRequest
    ): Response<GroqChatResponse>

    @GET("openai/v1/models")
    suspend fun listModels(
        @Header("Authorization") authorization: String
    ): Response<GroqModelsResponse>
}
