package io.github.arashiyama11.dncl_ide.adapter

import io.github.arashiyama11.dncl_ide.domain.model.Definition
import io.github.arashiyama11.dncl_ide.language_server.CompletionItem

internal fun List<CompletionItem>.toDefinitionList(): List<Definition> =
    map { completion ->
        Definition(
            literal = completion.label,
            position = null,
            isFunction = completion.kind == 2
        )
    }
