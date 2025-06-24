package io.github.arashiyama11.dncl_ide.domain.usecase

import io.github.arashiyama11.dncl_ide.domain.repository.AiCompletionRepository

class AiSuggestionUseCase(private val repository: AiCompletionRepository) {
    suspend operator fun invoke(code: String, cursor: Int): List<String> {
        return repository.getCompletions(code, cursor)
    }
}
