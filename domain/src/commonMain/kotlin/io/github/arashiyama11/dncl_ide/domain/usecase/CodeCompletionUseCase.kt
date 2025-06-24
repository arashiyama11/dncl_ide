package io.github.arashiyama11.dncl_ide.domain.usecase

import io.github.arashiyama11.dncl_ide.domain.model.Definition
import io.github.arashiyama11.dncl_ide.domain.repository.CodeCompletionRepository

class CodeCompletionUseCase(private val repository: CodeCompletionRepository) {
    suspend fun fetch(code: String, cursorPosition: Int): List<Definition> {
        return repository.fetchSuggestions(code, cursorPosition)
    }
}
