package io.github.arashiyama11.dncl_ide.language_server

import arrow.core.Either
import io.github.arashiyama11.dncl_ide.domain.usecase.SuggestionUseCase
import io.github.arashiyama11.dncl_ide.interpreter.lexer.Lexer
import io.github.arashiyama11.dncl_ide.interpreter.parser.Parser

class DefinitionService(private val diagnosticService: DiagnosticService) {
    fun getDefinitionLocation(uri: String, code: String, offset: Int): Location? {
        val lexer = Lexer(code)
        val parser: Parser = when (val parserResult = Parser(lexer)) {
            is Either.Left -> {
                return null
            }
            is Either.Right -> parserResult.value
        }

        val program = when (val programResult = parser.parseProgram()) {
            is Either.Left -> {
                return null
            }
            is Either.Right -> programResult.value
        }

        val suggestionUseCase = SuggestionUseCase()
        val definitions =
            suggestionUseCase.suggestWithParsedData(code, offset, lexer.toList(), program)

        return definitions.firstOrNull { def ->
            def.position?.let { pos -> pos <= offset && (pos + def.literal.length) >= offset } == true
        }?.let { def ->
            val pos =
                def.position!! // Now this is safe because the filter ensures it's not null
            val (startLine, startChar) = diagnosticService.calculateLineAndCharacter(code, pos)
            val (endLine, endChar) = diagnosticService.calculateLineAndCharacter(code, pos + def.literal.length)
            Location(
                uri = uri,
                range = Range(Position(startLine, startChar), Position(endLine, endChar))
            )
        }
    }
}
