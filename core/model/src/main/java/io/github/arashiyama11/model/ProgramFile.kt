package io.github.arashiyama11.model

data class ProgramFile(
    val name: FileName,
    val content: FileContent,
    val cursorPosition: CursorPosition
)