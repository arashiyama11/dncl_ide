package io.github.arashiyama11.dncl_ide.domain.repository

interface AiCompletionRepository {
    suspend fun getCompletions(code: String, cursor: Int, maxSuggestions: Int = 5): List<String>
}
