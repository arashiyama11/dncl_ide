package io.github.arashiyama11.dncl_ide.language_server

import arrow.core.Either
import io.github.arashiyama11.dncl_ide.interpreter.lexer.Lexer
import io.github.arashiyama11.dncl_ide.interpreter.model.DnclError
import io.github.arashiyama11.dncl_ide.interpreter.parser.Parser

class DiagnosticService {

    fun getDiagnostics(uri: String, text: String): List<Diagnostic> {
        val diagnostics = mutableListOf<Diagnostic>()
        val lexer = Lexer(text)
        val parser: Parser = when (val parserResult = Parser(lexer)) {
            is Either.Left -> {
                diagnostics.add(parserResult.value.toDiagnostic(text))
                return diagnostics
            }
            is Either.Right -> parserResult.value
        }

        when (val programResult = parser.parseProgram()) {
            is Either.Left -> diagnostics.add(programResult.value.toDiagnostic(text))
            is Either.Right -> { /* Success, no diagnostics from parser */ }
        }
        return diagnostics
    }

    private fun DnclError.toDiagnostic(program: String): Diagnostic {
        val (line, character) = calculateLineAndCharacter(program, this.errorRange?.first ?: 0)
        val (endLine, endCharacter) = calculateLineAndCharacter(program, this.errorRange?.last ?: 0)

        return Diagnostic(
            range = Range(
                start = Position(line, character),
                end = Position(endLine, endCharacter)
            ),
            severity = 1, // Error
            message = this.explain(program),
            source = "dncl-ls"
        )
    }

    fun calculateLineAndCharacter(program: String, offset: Int): Pair<Int, Int> {
        var line = 0
        var character = 0
        var currentOffset = 0

        val lines = program.lines()
        for ((idx, s) in lines.withIndex()) {
            if (currentOffset + s.length + 1 > offset) { // +1 for newline character
                line = idx
                character = offset - currentOffset
                break
            }
            currentOffset += s.length + 1
        }
        return Pair(line, character)
    }
}
