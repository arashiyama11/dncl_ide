package io.github.arashiyama11.dncl_ide.language_server.service

import arrow.core.Either
import io.github.arashiyama11.dncl_ide.interpreter.lexer.Lexer
import io.github.arashiyama11.dncl_ide.interpreter.model.DnclError
import io.github.arashiyama11.dncl_ide.interpreter.parser.Parser
import io.github.arashiyama11.dncl_ide.language_server.Diagnostic
import io.github.arashiyama11.dncl_ide.language_server.Position
import io.github.arashiyama11.dncl_ide.language_server.Range
import io.github.arashiyama11.dncl_ide.language_server.util.calculatePosition

class DiagnosticService {

    fun getDiagnostics(uri: String, text: String): List<Diagnostic> {
        val lexer = Lexer(text)
        val parser: Parser = when (val parserResult = Parser(lexer)) {
            is Either.Left -> {
                return listOf(parserResult.value.toDiagnostic(text))
            }

            is Either.Right -> parserResult.value
        }

        return when (val programResult = parser.parseProgram()) {
            is Either.Left -> listOf(programResult.value.toDiagnostic(text))
            is Either.Right -> emptyList()
        }
    }

    private fun DnclError.toDiagnostic(program: String): Diagnostic {
        val start = calculatePosition(program, this.errorRange?.first ?: 0)
        val end = calculatePosition(program, this.errorRange?.last ?: 0)

        return Diagnostic(
            range = Range(
                start = start,
                end = end
            ),
            severity = 1, // Error
            message = this.explain(program),
            source = "dncl-ls"
        )
    }
}
