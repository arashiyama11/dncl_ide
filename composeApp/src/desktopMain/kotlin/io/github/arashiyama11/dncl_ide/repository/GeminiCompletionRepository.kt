package io.github.arashiyama11.dncl_ide.repository

import io.github.arashiyama11.dncl_ide.domain.repository.AiCompletionRepository
import kotlinx.serialization.Serializable
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import java.net.URI
import java.net.http.HttpClient
import java.net.http.HttpRequest
import java.net.http.HttpResponse

actual class GeminiCompletionRepository : AiCompletionRepository {
    private val apiKey = "MOCK_API_KEY"
    private val endpoint = "https://example.com/v1beta/models/gemini-flash-lite:generateContent"

    override suspend fun getCompletions(code: String, cursor: Int, maxSuggestions: Int): List<String> {
        val prompt = code.take(cursor)
        val requestBody = Json.encodeToString(GeminiRequest(listOf(Content("user", listOf(Part(prompt))))) )
        return runCatching {
            val request = HttpRequest.newBuilder()
                .uri(URI.create("$endpoint?key=$apiKey"))
                .header("Content-Type", "application/json")
                .POST(HttpRequest.BodyPublishers.ofString(requestBody))
                .build()
            val response = HttpClient.newHttpClient().send(request, HttpResponse.BodyHandlers.ofString())
            parseResponse(response.body())
        }.getOrElse { emptyList() }
    }

    private fun parseResponse(body: String): List<String> {
        // TODO: parse JSON response
        return emptyList()
    }
}

@Serializable
private data class GeminiRequest(val contents: List<Content>)
@Serializable
private data class Content(val role: String, val parts: List<Part>)
@Serializable
private data class Part(val text: String)
