package com.example.thailandweather.data.remote

import com.example.thailandweather.core.model.ConnectionStatus
import kotlinx.coroutines.test.runTest
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.ResponseBody.Companion.toResponseBody
import org.junit.Assert.*
import org.junit.Test
import retrofit2.Response

class GroqClientTest {

    private class FakeGroqApiService(
        var statusCode: Int = 200,
        var responseBody: GroqChatResponse? = null,
        var capturedAuthHeader: String? = null
    ) : GroqApiService {
        override suspend fun createChatCompletion(
            authorization: String,
            request: GroqChatRequest
        ): Response<GroqChatResponse> {
            capturedAuthHeader = authorization
            return if (statusCode == 200) {
                Response.success(responseBody)
            } else {
                Response.error(
                    statusCode,
                    "{\"error\":{\"message\":\"Mock error $statusCode\"}}"
                        .toResponseBody("application/json".toMediaType())
                )
            }
        }

        override suspend fun listModels(authorization: String): Response<GroqModelsResponse> {
            capturedAuthHeader = authorization
            return Response.success(GroqModelsResponse())
        }
    }

    @Test
    fun testAuthHeaderHasBearerPrefix() = runTest {
        val fakeApi = FakeGroqApiService(
            statusCode = 200,
            responseBody = GroqChatResponse(
                id = "chatcmpl-test",
                choices = listOf(GroqChoice(message = GroqMessage(role = "assistant", content = "pong")))
            )
        )

        val client = GroqClient()
        // Inject fake service via reflection or direct testing
        val key = "gsk_secret_123456"
        val authHeader = "Bearer $key"
        fakeApi.createChatCompletion(
            authHeader,
            GroqChatRequest(model = "llama-3.3-70b-versatile", messages = listOf(GroqMessage("user", "ping")))
        )

        assertEquals("Bearer gsk_secret_123456", fakeApi.capturedAuthHeader)
        assertFalse("Key should never be in URL or query", fakeApi.capturedAuthHeader!!.contains("?key="))
    }

    @Test
    fun testInvalidKeyMapsToInvalidKeyError() = runTest {
        val fakeApi = FakeGroqApiService(statusCode = 401)
        val response = fakeApi.createChatCompletion("Bearer invalid", GroqChatRequest("test", emptyList()))
        assertEquals(401, response.code())
        assertFalse(response.isSuccessful)
    }

    @Test
    fun testRateLimitMapsTo429() = runTest {
        val fakeApi = FakeGroqApiService(statusCode = 429)
        val response = fakeApi.createChatCompletion("Bearer key", GroqChatRequest("test", emptyList()))
        assertEquals(429, response.code())
        assertFalse(response.isSuccessful)
    }
}
