package io.github.arashiyama11.dncl_ide.language_server.service

import arrow.core.Either
import io.github.arashiyama11.dncl_ide.interpreter.lexer.Lexer
import io.github.arashiyama11.dncl_ide.interpreter.model.AstNode
import io.github.arashiyama11.dncl_ide.interpreter.model.BuiltInFunctionSignature
import io.github.arashiyama11.dncl_ide.interpreter.model.DnclError
import io.github.arashiyama11.dncl_ide.interpreter.parser.Parser
import io.github.arashiyama11.dncl_ide.interpreter.preprocessor.preProcess
import io.github.arashiyama11.dncl_ide.language_server.Diagnostic
import io.github.arashiyama11.dncl_ide.language_server.Range
import io.github.arashiyama11.dncl_ide.language_server.FileResolver
import io.github.arashiyama11.dncl_ide.language_server.service.StdlibOnlyFileResolver
import io.github.arashiyama11.dncl_ide.language_server.service.resolveLibText
import io.github.arashiyama11.dncl_ide.language_server.util.calculatePosition
import kotlinx.coroutines.flow.toList

data class DiagnosticResult(
    val diagnostics: List<Diagnostic>,
    val program: AstNode.Program?,
    val builtInSignatures: List<BuiltInFunctionSignature> = emptyList()
)

class DiagnosticService(
    private val fileResolver: FileResolver = StdlibOnlyFileResolver()
) {

    @Suppress("UNUSED_PARAMETER")
    suspend fun analyze(uri: String, text: String): DiagnosticResult {
        val builtIns = mutableListOf<BuiltInFunctionSignature>()
        val lexer = preProcess(
            Lexer(text, uri),
            resolveLib = { path -> resolveLibText(fileResolver, path) },
            onBuiltInSignature = { builtIns += it }
        ).toList()
        val parser: Parser = when (val parserResult = Parser(lexer)) {
            is Either.Left -> {
                return DiagnosticResult(
                    diagnostics = listOf(parserResult.value.toDiagnostic(text)),
                    program = null,
                    builtInSignatures = builtIns
                )
            }

            is Either.Right -> parserResult.value
        }

        return when (val programResult = parser.parseProgram()) {
            is Either.Left -> DiagnosticResult(
                diagnostics = listOf(programResult.value.toDiagnostic(text)),
                program = null,
                builtInSignatures = builtIns
            )

            is Either.Right -> DiagnosticResult(
                diagnostics = emptyList(),
                program = programResult.value,
                builtInSignatures = builtIns
            )
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
