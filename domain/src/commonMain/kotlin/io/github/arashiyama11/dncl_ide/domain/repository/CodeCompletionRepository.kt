package io.github.arashiyama11.dncl_ide.domain.repository

import io.github.arashiyama11.dncl_ide.domain.model.Definition

interface CodeCompletionRepository {
    suspend fun fetchSuggestions(code: String, cursorPosition: Int): List<Definition>
}
