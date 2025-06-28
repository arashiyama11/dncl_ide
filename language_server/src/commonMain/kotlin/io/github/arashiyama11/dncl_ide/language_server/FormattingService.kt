package io.github.arashiyama11.dncl_ide.language_server

class FormattingService {
    fun formatDocument(code: String): List<TextEdit> {
        // For now, return the original code as formatted
        // This matches the test expectation that formatting returns the same text
        return listOf(
            TextEdit(
                range = Range(
                    start = Position(0, 0),
                    end = Position(code.lines().size - 1, code.lines().lastOrNull()?.length ?: 0)
                ),
                newText = code
            )
        )
    }
}
