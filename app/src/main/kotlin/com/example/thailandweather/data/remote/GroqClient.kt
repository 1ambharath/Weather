package com.example.thailandweather.data.remote

import com.example.thailandweather.core.model.ConnectionStatus
import kotlinx.coroutines.delay
import kotlinx.serialization.json.Json
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import retrofit2.Response
import retrofit2.Retrofit
import retrofit2.converter.kotlinx.serialization.asConverterFactory
import java.io.IOException
import java.net.SocketTimeoutException
import java.util.concurrent.TimeUnit

sealed class GroqException(message: String) : Exception(message) {
    class InvalidKey(message: String = "Invalid API key") : GroqException(message)
    class RateLimitExceeded(message: String = "Rate limit exceeded") : GroqException(message)
    class ModelUnavailable(message: String = "Model is currently unavailable") : GroqException(message)
    class NetworkFailure(message: String = "Network connection failed") : GroqException(message)
    class ServerError(val code: Int, message: String = "Groq server error: $code") : GroqException(message)
    class EmptyResponse(message: String = "Received empty response from Groq") : GroqException(message)
}

class GroqClient(
    private val okHttpClient: OkHttpClient = createGroqOkHttpClient(),
    baseUrl: String = "https://api.groq.com/"
) {
    private val json = Json {
        ignoreUnknownKeys = true
        coerceInputValues = true
        isLenient = true
        encodeDefaults = false
        explicitNulls = false
    }

    private val contentType = "application/json".toMediaType()

    private val retrofit = Retrofit.Builder()
        .baseUrl(baseUrl)
        .client(okHttpClient)
        .addConverterFactory(json.asConverterFactory(contentType))
        .build()

    val apiService: GroqApiService = retrofit.create(GroqApiService::class.java)

    /**
     * Extracts descriptive error message from Groq API error response body.
     */
    private fun extractGroqErrorMessage(response: Response<*>, defaultMessage: String): String {
        return try {
            val errorBody = response.errorBody()?.string()
            if (!errorBody.isNullOrBlank()) {
                val parsed = json.decodeFromString<GroqErrorResponse>(errorBody)
                val msg = parsed.error?.message
                if (!msg.isNullOrBlank()) {
                    return msg
                }
            }
            defaultMessage
        } catch (_: Exception) {
            defaultMessage
        }
    }

    /**
     * Queries available models for an authenticated API key.
     */
    suspend fun getAvailableModels(apiKey: String): Result<List<String>> {
        val trimmedKey = apiKey.trim()
        if (trimmedKey.isEmpty()) {
            return Result.success(emptyList())
        }
        val authHeader = "Bearer $trimmedKey"
        return try {
            val response = apiService.listModels(authHeader)
            if (response.isSuccessful) {
                val ids = response.body()?.data?.map { it.id }.orEmpty()
                Result.success(ids)
            } else {
                val errorMsg = extractGroqErrorMessage(response, "HTTP ${response.code()} from Groq")
                Result.failure(GroqException.ServerError(response.code(), errorMsg))
            }
        } catch (e: Exception) {
            Result.failure(GroqException.NetworkFailure(e.localizedMessage ?: "Failed to reach Groq"))
        }
    }

    /**
     * Performs an authenticated verification call to verify credentials and model accessibility.
     * 1. Validates the key and retrieves account's accessible models via GET /openai/v1/models.
     * 2. Checks if the requested model is enabled for this account.
     * 3. Runs a minimal ping completion with clean non-null serialization.
     */
    suspend fun testConnection(apiKey: String, model: String): Result<ConnectionStatus> {
        val trimmedKey = apiKey.trim()
        if (trimmedKey.isEmpty()) {
            return Result.success(ConnectionStatus.NOT_CONFIGURED)
        }

        val authHeader = "Bearer $trimmedKey"

        // 1. Verify credentials and query account's accessible models via GET /openai/v1/models
        val modelsResponse = try {
            apiService.listModels(authHeader)
        } catch (e: SocketTimeoutException) {
            return Result.failure(GroqException.NetworkFailure("Request timed out while connecting to Groq"))
        } catch (e: IOException) {
            return Result.failure(GroqException.NetworkFailure("Network error: ${e.message ?: "Unable to reach api.groq.com"}"))
        } catch (e: Exception) {
            return Result.failure(GroqException.NetworkFailure(e.localizedMessage ?: "Unexpected connection error"))
        }

        if (!modelsResponse.isSuccessful) {
            val errorMsg = extractGroqErrorMessage(modelsResponse, "Connection failed with HTTP ${modelsResponse.code()}")
            return when (modelsResponse.code()) {
                401 -> Result.failure(GroqException.InvalidKey(errorMsg))
                429 -> Result.failure(GroqException.RateLimitExceeded(errorMsg))
                else -> Result.failure(GroqException.ServerError(modelsResponse.code(), errorMsg))
            }
        }

        val availableModelIds = modelsResponse.body()?.data?.map { it.id }.orEmpty()

        // 2. If the user selected a model that is not in the account's active models list
        if (availableModelIds.isNotEmpty() && !availableModelIds.any { it.equals(model, ignoreCase = true) }) {
            val suggestion = availableModelIds.firstOrNull { it.contains("llama", ignoreCase = true) }
                ?: availableModelIds.firstOrNull()
            val hint = if (suggestion != null) " (Try switching to $suggestion)" else ""
            return Result.failure(
                GroqException.ModelUnavailable("Selected model '$model' is not available for this account.$hint")
            )
        }

        // 3. Perform a minimal ping chat completion with a clean payload (no null fields)
        val pingRequest = GroqChatRequest(
            model = model,
            messages = listOf(GroqMessage(role = "user", content = "ping")),
            maxTokens = 5,
            responseFormat = null
        )

        return try {
            val response = apiService.createChatCompletion(authHeader, pingRequest)
            if (response.isSuccessful) {
                Result.success(ConnectionStatus.CONNECTED)
            } else {
                val errorMsg = extractGroqErrorMessage(response, "HTTP ${response.code()} from Groq")
                when (response.code()) {
                    401 -> Result.failure(GroqException.InvalidKey(errorMsg))
                    429 -> Result.failure(GroqException.RateLimitExceeded(errorMsg))
                    400, 404 -> Result.failure(GroqException.ModelUnavailable(errorMsg))
                    in 500..599 -> Result.failure(GroqException.ServerError(response.code(), errorMsg))
                    else -> Result.failure(GroqException.ServerError(response.code(), errorMsg))
                }
            }
        } catch (e: SocketTimeoutException) {
            Result.failure(GroqException.NetworkFailure("Request timed out during ping"))
        } catch (e: IOException) {
            Result.failure(GroqException.NetworkFailure("Network error during ping"))
        } catch (e: Exception) {
            Result.failure(GroqException.NetworkFailure(e.localizedMessage ?: "Unexpected error during ping"))
        }
    }

    /**
     * Executes chat completion with bounded retries for transient failures.
     * Permanent failures (401, 429, 400, 404) are handled with exact error diagnostics.
     */
    suspend fun generateBriefingCompletion(
        apiKey: String,
        model: String,
        systemPrompt: String,
        userPrompt: String,
        maxRetries: Int = 1
    ): Result<String> {
        val trimmedKey = apiKey.trim()
        if (trimmedKey.isEmpty()) {
            return Result.failure(GroqException.InvalidKey("API key is missing or not configured"))
        }

        val authHeader = "Bearer $trimmedKey"
        val request = GroqChatRequest(
            model = model,
            messages = listOf(
                GroqMessage(role = "system", content = systemPrompt),
                GroqMessage(role = "user", content = userPrompt)
            ),
            temperature = 0.2,
            maxTokens = 1800,
            responseFormat = GroqResponseFormat(type = "json_object")
        )

        var attempts = 0
        while (attempts <= maxRetries) {
            attempts++
            try {
                val response: Response<GroqChatResponse> = apiService.createChatCompletion(authHeader, request)

                if (response.isSuccessful) {
                    val content = response.body()?.choices?.firstOrNull()?.message?.content
                    return if (!content.isNullOrBlank()) {
                        Result.success(content)
                    } else {
                        Result.failure(GroqException.EmptyResponse())
                    }
                }

                val errorMsg = extractGroqErrorMessage(response, "Groq error HTTP ${response.code()}")
                when (response.code()) {
                    401 -> return Result.failure(GroqException.InvalidKey(errorMsg))
                    429 -> return Result.failure(GroqException.RateLimitExceeded(errorMsg))
                    400 -> {
                        // If 400 occurs with json_object format, fallback by retrying without responseFormat
                        if (request.responseFormat != null) {
                            try {
                                val fallbackRequest = request.copy(responseFormat = null)
                                val fallbackResponse = apiService.createChatCompletion(authHeader, fallbackRequest)
                                if (fallbackResponse.isSuccessful) {
                                    val content = fallbackResponse.body()?.choices?.firstOrNull()?.message?.content
                                    if (!content.isNullOrBlank()) {
                                        return Result.success(content)
                                    }
                                }
                            } catch (_: Exception) {
                                // Fall through to standard error return
                            }
                        }
                        return Result.failure(GroqException.ModelUnavailable(errorMsg))
                    }
                    404 -> return Result.failure(GroqException.ModelUnavailable(errorMsg))
                    in 500..599 -> {
                        if (attempts <= maxRetries) {
                            delay(1500)
                            continue
                        }
                        return Result.failure(GroqException.ServerError(response.code(), errorMsg))
                    }
                    else -> return Result.failure(GroqException.ServerError(response.code(), errorMsg))
                }
            } catch (e: SocketTimeoutException) {
                if (attempts <= maxRetries) {
                    delay(1000)
                    continue
                }
                return Result.failure(GroqException.NetworkFailure("Request timed out"))
            } catch (e: IOException) {
                if (attempts <= maxRetries) {
                    delay(1000)
                    continue
                }
                return Result.failure(GroqException.NetworkFailure("Network connection error"))
            } catch (e: Exception) {
                return Result.failure(GroqException.NetworkFailure(e.localizedMessage ?: "Unknown error"))
            }
        }

        return Result.failure(GroqException.NetworkFailure("Exceeded retry attempts"))
    }

    companion object {
        /**
         * Builds an OkHttpClient strictly configured to avoid logging Authorization headers.
         */
        fun createGroqOkHttpClient(): OkHttpClient {
            return OkHttpClient.Builder()
                .dns(ResilientDns)
                .connectTimeout(25, TimeUnit.SECONDS)
                .readTimeout(35, TimeUnit.SECONDS)
                .writeTimeout(25, TimeUnit.SECONDS)
                // Note: We do NOT attach HttpLoggingInterceptor with Level.BODY or Level.HEADERS
                // to guarantee the Bearer key is NEVER logged to logcat, files, or bug reports.
                .build()
        }
    }
}
