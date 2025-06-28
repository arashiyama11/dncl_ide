package io.github.arashiyama11.dncl_ide.language_server

import io.github.arashiyama11.dncl_ide.domain.usecase.SuggestionUseCase

class CompletionService {
    fun getCompletionItems(code: String, offset: Int): List<CompletionItem> {
        val suggestionUseCase = SuggestionUseCase()
        val suggestions = suggestionUseCase.suggestWhenFailingParse(code, offset)
        return suggestions.map { def ->
            CompletionItem(
                label = def.literal,
                kind = if (def.isFunction) 2 else 1
            ) // 2 for Function, 1 for Text
        }
    }
}
