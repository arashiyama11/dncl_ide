package io.github.arashiyama11.dncl_ide.language_server.util

/**
 * Calculates the zero-based character offset in the program text for given line and character.
 */
fun calculateOffset(program: String, line: Int, character: Int): Int {
    var offset = 0
    val lines = program.lines()
    for (i in 0 until line) {
        if (i < lines.size) {
            offset += lines[i].length + 1
        }
    }
    return offset + character
}

/**
 * Calculates the zero-based line and character position for a given offset in program text.
 */
fun calculateLineAndCharacter(program: String, offset: Int): Pair<Int, Int> {
    var currentOffset = 0
    program.lines().forEachIndexed { idx, lineText ->
        val lineLength = lineText.length + 1 // account for newline
        if (currentOffset + lineLength > offset) {
            return Pair(idx, offset - currentOffset)
        }
        currentOffset += lineLength
    }
    val lastLine = program.lines().size - 1
    val lastChar = program.lines().lastOrNull()?.length ?: 0
    return Pair(lastLine, lastChar)
}
