package io.github.arashiyama11.dncl_ide.repository

import io.github.arashiyama11.dncl_ide.domain.model.Definition
import io.github.arashiyama11.dncl_ide.domain.repository.CodeCompletionRepository
import io.ktor.client.HttpClient
import io.ktor.client.call.body
import io.ktor.client.request.post
import io.ktor.client.request.setBody
import io.ktor.http.ContentType
import io.ktor.http.contentType
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

import io.github.arashiyama11.dncl_ide.network.NetworkClient

class GeminiCompletionRepository(private val client: HttpClient = NetworkClient.httpClient) : CodeCompletionRepository {

    @Serializable
    private data class CompletionRequest(
        val code: String,
        @SerialName("cursor") val cursorPosition: Int
    )

    @Serializable
    private data class CompletionResponse(val suggestions: List<String>)

    override suspend fun fetchSuggestions(code: String, cursorPosition: Int): List<Definition> {
        val response: CompletionResponse = client.post("https://example.com/gemini") {
            contentType(ContentType.Application.Json)
            setBody(CompletionRequest(code, cursorPosition))
        }.body()
        return if (response.suggestions.isEmpty()) {
            emptyList()
        } else {
            response.suggestions.map { Definition(it, null, false) }
        }
    }
}
