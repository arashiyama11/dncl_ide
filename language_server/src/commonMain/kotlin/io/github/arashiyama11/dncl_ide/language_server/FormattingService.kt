package io.github.arashiyama11.dncl_ide.language_server

class FormattingService {
    fun formatDocument(code: String): List<TextEdit> {
        val formattedText = code.lines().joinToString("\n") { line ->
            line.trim()
        }
        return listOf(
            TextEdit(
                range = Range(
                    start = Position(0, 0),
                    end = Position(code.lines().size, 0)
                ),
                newText = formattedText
            )
        )
    }
}
