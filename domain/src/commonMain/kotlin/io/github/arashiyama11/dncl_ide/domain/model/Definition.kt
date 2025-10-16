package io.github.arashiyama11.dncl_ide.domain.model

enum class DefinitionKind {
    FUNCTION,
    VARIABLE,
    KEYWORD,
    SNIPPET,
    TEXT,
    UNKNOWN
}

enum class DefinitionInsertTextFormat {
    PLAIN_TEXT,
    SNIPPET
}

data class Definition(
    val literal: String,
    val position: Int?,
    val kind: DefinitionKind,
    val detail: String? = null,
    val insertText: String = literal,
    val insertTextFormat: DefinitionInsertTextFormat = DefinitionInsertTextFormat.PLAIN_TEXT
) {
    val isCallable: Boolean get() = kind == DefinitionKind.FUNCTION
    val isSnippet: Boolean get() = kind == DefinitionKind.SNIPPET
    val isKeyword: Boolean get() = kind == DefinitionKind.KEYWORD
}
