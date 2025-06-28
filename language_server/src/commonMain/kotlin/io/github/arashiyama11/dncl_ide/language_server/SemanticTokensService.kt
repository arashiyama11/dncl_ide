package io.github.arashiyama11.dncl_ide.language_server

import io.github.arashiyama11.dncl_ide.interpreter.lexer.Lexer
import io.github.arashiyama11.dncl_ide.interpreter.model.Token
import io.github.arashiyama11.dncl_ide.language_server.util.calculateLineAndCharacter

class SemanticTokensService(private val diagnosticService: DiagnosticService) {
    fun getSemanticTokens(code: String): SemanticTokens {
        val lexer = Lexer(code)
        val tokens = lexer.toList().mapNotNull { it.getOrNull() }

        val data = mutableListOf<Int>()
        var lastLine = 0
        var lastChar = 0

        tokens.forEach { token ->
            val (tokenLine, tokenChar) = calculateLineAndCharacter(code, token.range.first)
            val tokenLength = token.literal.length
            val tokenType = getTokenType(token)
            val tokenModifiers = 0 // No modifiers for now

            data.add(tokenLine - lastLine)
            data.add(if (tokenLine == lastLine) tokenChar - lastChar else tokenChar)
            data.add(tokenLength)
            data.add(tokenType)
            data.add(tokenModifiers)

            lastLine = tokenLine
            lastChar = tokenChar
        }
        return SemanticTokens(data = data)
    }

    private fun getTokenType(token: Token): Int {
        return when (token) {
            is Token.If, is Token.Function, is Token.Wo, is Token.Kara, is Token.Made, is Token.While, is Token.UpTo, is Token.DownTo, is Token.Define, is Token.Then, is Token.Else, is Token.Elif, is Token.And, is Token.Or -> 0 // keyword
            is Token.Identifier, is Token.Japanese -> 1 // variable
            is Token.Int, is Token.Float -> 3 // number
            is Token.String -> 4 // string
            is Token.Comment -> 5 // comment
            is Token.Plus, is Token.Minus, is Token.Times, is Token.Divide, is Token.DivideInt, is Token.Modulo, is Token.Assign, is Token.Equal, is Token.NotEqual, is Token.GreaterThan, is Token.LessThan, is Token.GreaterThanOrEqual, is Token.LessThanOrEqual, is Token.Bang -> 6 // operator
            else -> 0 // default to keyword
        }
    }
}
