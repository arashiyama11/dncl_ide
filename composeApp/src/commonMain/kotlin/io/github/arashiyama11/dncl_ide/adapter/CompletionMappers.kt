package io.github.arashiyama11.dncl_ide.adapter

import io.github.arashiyama11.dncl_ide.domain.model.Definition
import io.github.arashiyama11.dncl_ide.domain.model.DefinitionInsertTextFormat
import io.github.arashiyama11.dncl_ide.domain.model.DefinitionKind
import io.github.arashiyama11.dncl_ide.language_server.CompletionItem

private const val LSP_INSERT_TEXT_SNIPPET = 2

internal fun List<CompletionItem>.toDefinitionList(): List<Definition> =
    map { it.toDefinition() }

private fun CompletionItem.toDefinition(): Definition {
    val definitionKind = when (kind) {
        2, 3 -> DefinitionKind.FUNCTION
        4 -> DefinitionKind.FUNCTION
        5, 6, 10 -> DefinitionKind.VARIABLE
        14 -> DefinitionKind.KEYWORD
        15 -> DefinitionKind.SNIPPET
        null -> DefinitionKind.TEXT
        else -> DefinitionKind.TEXT
    }

    val insertFormat = when (insertTextFormat) {
        LSP_INSERT_TEXT_SNIPPET -> DefinitionInsertTextFormat.SNIPPET
        else -> DefinitionInsertTextFormat.PLAIN_TEXT
    }

    val rawInsertText = insertText ?: label
    val normalizedInsertText = when (insertFormat) {
        DefinitionInsertTextFormat.SNIPPET -> rawInsertText.toPlainTextSnippet()
        DefinitionInsertTextFormat.PLAIN_TEXT -> rawInsertText
    }.let { text ->
        if (definitionKind == DefinitionKind.FUNCTION &&
            insertFormat == DefinitionInsertTextFormat.PLAIN_TEXT &&
            insertText.isNullOrBlank()
        ) {
            "$text()"
        } else {
            text
        }
    }

    return Definition(
        literal = label,
        position = null,
        kind = definitionKind,
        detail = detail,
        insertText = normalizedInsertText,
        insertTextFormat = insertFormat
    )
}

private val snippetPlaceholderWithDefault = Regex("\\$\\{\\d+:([^}]+)}")
private val snippetPlaceholderWithoutDefault = Regex("\\$\\{\\d+}")
private val simpleTabStop = Regex("\\$\\d+")

private fun String.toPlainTextSnippet(): String =
    this.replace(snippetPlaceholderWithDefault) { matchResult ->
        matchResult.groupValues.getOrNull(1).orEmpty()
    }.replace(snippetPlaceholderWithoutDefault, "")
        .replace(simpleTabStop, "")
