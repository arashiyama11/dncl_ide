package io.github.arashiyama11.dncl_ide.language_server.service

import io.github.arashiyama11.dncl_ide.interpreter.lexer.Lexer
import io.github.arashiyama11.dncl_ide.interpreter.model.Token
import io.github.arashiyama11.dncl_ide.interpreter.model.AstNode
import io.github.arashiyama11.dncl_ide.language_server.ast.SymbolKind
import io.github.arashiyama11.dncl_ide.interpreter.model.AllBuiltInFunction
import io.github.arashiyama11.dncl_ide.language_server.Hover
import io.github.arashiyama11.dncl_ide.language_server.MarkupContent
import io.github.arashiyama11.dncl_ide.language_server.util.calculatePosition

class HoverService(
    private val astInfoService: AstInfoService
) {
    fun getHover(code: String, offset: Int): Hover? {
        // First parse and analyze the code
        val astInfo = astInfoService.parseAndAnalyze(code) ?: return null

        val lexer = Lexer(code)
        val tokens = lexer.toList().mapNotNull { it.getOrNull() }

        val hoveredToken = tokens.firstOrNull { token ->
            offset in token.range
        }

        val hoverContent: String? = when (hoveredToken) {
            is Token.Japanese, is Token.Identifier -> {
                val symbol = astInfoService.findSymbolAtOffset(astInfo, offset)
                when (symbol?.kind) {
                    SymbolKind.VARIABLE -> {
                        val start = calculatePosition(code, symbol.range.first)
                        """**変数**: `${symbol.name}`  
                           定義位置: ${start.line + 1}行${start.character}文字目  
                           ${if (symbol.type != null) "型: ${symbol.type}" else ""}""".trimIndent()
                    }

                    SymbolKind.FUNCTION -> {
                        val functionNode = symbol.definitionNode as? AstNode.FunctionStatement
                        val params =
                            functionNode?.parameters?.joinToString(", ") { it.literal } ?: ""
                        val start = calculatePosition(code, symbol.range.first)
                        """**関数**: `${symbol.name}($params)`  
                           定義位置: ${start.line + 1}行${start.character + 1}文字目  
                           ${if (params.isNotEmpty()) "パラメータ: $params" else "パラメータなし"}
                        """.trimIndent()
                    }

                    SymbolKind.PARAMETER -> {
                        val start = calculatePosition(code, symbol.range.first)
                        """**パラメータ**: `${symbol.name}`
                           定義位置: ${start.line + 1}行${start.character + 1}文字目
                           関数のパラメータとして定義されています""".trimIndent()
                    }

                    SymbolKind.BUILT_IN_FUNCTION -> {
                        val builtInFunction = AllBuiltInFunction.from(symbol.name)
                        if (builtInFunction != null) {
                            "**組み込み関数**: `${builtInFunction.identifier}`\n\n${
                                descriptionOfBuiltInFunction(
                                    builtInFunction
                                )
                            }"
                        } else {
                            "**組み込み関数**: `${symbol.name}`"
                        }
                    }

                    SymbolKind.UNKNOWN, null -> {
                        // シンボルが見つからない場合は組み込み関数かどうかチェック
                        val tokenText = hoveredToken.literal
                        val builtInFunction = AllBuiltInFunction.from(tokenText)
                        if (builtInFunction != null) {
                            "**組み込み関数**: `${builtInFunction.identifier}`\n\n ${
                                descriptionOfBuiltInFunction(
                                    builtInFunction
                                )
                            }"
                        } else null
                    }
                }
            }

            is Token.Int -> {
                "**整数リテラル**: `${hoveredToken.literal}`\n\n" + "値: ${hoveredToken.literal}"
            }

            is Token.Float -> {
                "**浮動小数点リテラル**: `${hoveredToken.literal}`\n\n" +
                        "値: ${hoveredToken.literal}"
            }

            is Token.String -> {
                "**文字列リテラル**: `${hoveredToken.literal}`\n\n" +
                        "値: ${hoveredToken.literal}"
            }

            is Token.Boolean -> {
                "**ブール値リテラル**: `${hoveredToken.literal}`\n\n" +
                        "値: ${hoveredToken.value}"
            }

            else -> null
        }

        return hoverContent?.let { content ->
            Hover(
                contents = MarkupContent(
                    kind = "markdown",
                    value = content
                ),
                range = null
            )
        }
    }

    private fun descriptionOfBuiltInFunction(fn: AllBuiltInFunction): String {
        return when (fn) {
            AllBuiltInFunction.PRINT -> """
                与えられた引数を画面に表示する関数です。どの型の引数も受け付けます。  
                複数与えられた場合は、スペースで区切って表示されます。
                使用例
                ```
                表示する("こんにちは")
                表示する("さようなら", 1+2, 3.14)
                ```
            """.trimIndent()

            AllBuiltInFunction.LENGTH -> TODO()
            AllBuiltInFunction.DIFF -> TODO()
            AllBuiltInFunction.RETURN -> TODO()
            AllBuiltInFunction.CONCAT -> TODO()
            AllBuiltInFunction.PUSH -> TODO()
            AllBuiltInFunction.SHIFT -> TODO()
            AllBuiltInFunction.UNSHIFT -> TODO()
            AllBuiltInFunction.POP -> TODO()
            AllBuiltInFunction.INT -> TODO()
            AllBuiltInFunction.FLOAT -> TODO()
            AllBuiltInFunction.STRING -> TODO()
            AllBuiltInFunction.IMPORT -> TODO()
            AllBuiltInFunction.CHAR_CODE -> TODO()
            AllBuiltInFunction.FROM_CHAR_CODE -> TODO()
            AllBuiltInFunction.SLICE -> TODO()
            AllBuiltInFunction.JOIN -> TODO()
            AllBuiltInFunction.SORT -> TODO()
            AllBuiltInFunction.REVERSE -> TODO()
            AllBuiltInFunction.FIND -> TODO()
            AllBuiltInFunction.SUBSTRING -> TODO()
            AllBuiltInFunction.SPLIT -> TODO()
            AllBuiltInFunction.TRIM -> TODO()
            AllBuiltInFunction.REPLACE -> TODO()
            AllBuiltInFunction.ROUND -> TODO()
            AllBuiltInFunction.FLOOR -> TODO()
            AllBuiltInFunction.CEIL -> TODO()
            AllBuiltInFunction.RANDOM -> TODO()
            AllBuiltInFunction.MAX -> TODO()
            AllBuiltInFunction.MIN -> TODO()
            AllBuiltInFunction.IS_INT -> TODO()
            AllBuiltInFunction.IS_FLOAT -> TODO()
            AllBuiltInFunction.IS_STRING -> TODO()
            AllBuiltInFunction.IS_ARRAY -> TODO()
            AllBuiltInFunction.IS_BOOLEAN -> TODO()
            AllBuiltInFunction.CLEAR -> TODO()
            AllBuiltInFunction.SLEEP -> TODO()
        }
    }
}
