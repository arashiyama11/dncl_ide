package io.github.arashiyama11.dncl_ide.language_server.service

import io.github.arashiyama11.dncl_ide.interpreter.lexer.Lexer
import io.github.arashiyama11.dncl_ide.interpreter.model.Token
import io.github.arashiyama11.dncl_ide.interpreter.model.AstNode
import io.github.arashiyama11.dncl_ide.language_server.ast.SymbolKind
import io.github.arashiyama11.dncl_ide.interpreter.model.AllBuiltInFunction
import io.github.arashiyama11.dncl_ide.language_server.FileResolver
import io.github.arashiyama11.dncl_ide.language_server.Hover
import io.github.arashiyama11.dncl_ide.language_server.MarkupContent
import io.github.arashiyama11.dncl_ide.language_server.util.calculatePosition

class HoverService(
    private val astInfoService: AstInfoService,
    private val fileResolver: FileResolver = StdlibOnlyFileResolver()
) {
    suspend fun getHover(code: String, offset: Int, cachedAstInfo: AstInfo? = null): Hover? {
        // First parse and analyze the code
        val astInfo = cachedAstInfo ?: return null

        val tokens = Lexer(code, astInfo.filePath)
            .toList()
            .mapNotNull { it.getOrNull() }

        val originalFilePath = astInfo.filePath ?: tokens.firstOrNull()?.filePath

        val hoveredToken = tokens.firstOrNull { token ->
            offset in token.range && token.filePath == originalFilePath
        }


        val hoverContent: String? = when (hoveredToken) {
            is Token.Japanese, is Token.Identifier -> {
                val symbol = astInfoService.findSymbolAtOffset(astInfo, offset, astInfo.filePath)
                when (symbol?.kind) {
                    SymbolKind.VARIABLE -> {
                        val start = calculatePosition(code, symbol.range.first)
                        """**変数**: `${symbol.name}`  
                           定義位置: ${start.line + 1}行${start.character + 1}文字目  
                           ${if (symbol.type != null) "型: ${symbol.type}" else ""}""".trimIndent()
                    }

                    SymbolKind.FUNCTION -> {
                        val functionNode = symbol.definitionNode as? AstNode.FunctionStatement
                        val params =
                            functionNode?.parameters?.joinToString(", ") { it.literal } ?: ""
                        val start = calculatePosition(code, symbol.range.first)
                        val funcImpl = run {
                            val c =
                                if (symbol.filePath == originalFilePath) code else resolveLibText(
                                    fileResolver,
                                    symbol.filePath!!
                                )
                            c.substring(expandWithLeadingComments(c, functionNode!!.range))
                        }
                        """関数: `${symbol.name}($params)`  
定義位置: ${start.line + 1}行${start.character + 1}文字目 
ファイル: ${symbol.filePath}

実装:
```
$funcImpl
```
""".trimIndent()
                    }

                    SymbolKind.PARAMETER -> {
                        val start = calculatePosition(code, symbol.range.first)
                        """**パラメータ**: `${symbol.name}`  
                       定義位置: ${start.line + 1}行${start.character + 1}文字目 
                       ファイル: ${symbol.filePath}
                       関数のパラメータとして定義されています""".trimIndent()
                    }

                    SymbolKind.BUILT_IN_FUNCTION -> {
                        builtInHoverContent(symbol.name, astInfo)
                    }

                    SymbolKind.UNKNOWN, null -> {
                        // シンボルが見つからない場合は組み込み関数かどうかチェック
                        val tokenText = hoveredToken.literal
                        val builtInFunction = AllBuiltInFunction.from(tokenText)
                        if (builtInFunction != null) {
                            builtInHoverContent(tokenText, astInfo)
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


    /**
     * 関数定義の前に連続している # コメント行を含めるように
     * Range の開始位置を上に広げる。
     */
    fun expandWithLeadingComments(src: String, original: IntRange): IntRange {
        var start = original.first
        var newlineBeforeCurrent = src.lastIndexOf('\n', start - 1)

        while (newlineBeforeCurrent >= 0) {
            val lineStart =
                src.lastIndexOf('\n', newlineBeforeCurrent - 1).let { if (it == -1) 0 else it + 1 }
            val lineEnd = newlineBeforeCurrent
            if (lineEnd <= lineStart) break

            val line = src.substring(lineStart, lineEnd)
            val trimmed = line.trimStart()
            if (trimmed.isEmpty() || !trimmed.startsWith("#")) break

            start = lineStart
            newlineBeforeCurrent = src.lastIndexOf('\n', lineStart - 1)
        }

        return start..original.last
    }

    private suspend fun builtInHoverContent(name: String, astInfo: AstInfo): String? {
        val signature = astInfo.builtInSignatures.firstOrNull { it.name == name }
        val params = signature?.params?.joinToString(", ")?.takeIf { it.isNotBlank() }
        val signatureText = params?.let { "$name($it)" } ?: "$name()"

        val prefix = "**組み込み関数**: `$signatureText`"
        val path = signature?.filePath ?: return prefix
        val range = signature.range ?: return prefix

        val source = resolveLibText(fileResolver, path)
        if (source.isEmpty()) return prefix

        val expanded = expandWithLeadingComments(source, range)
        val safeStart = expanded.first.coerceAtLeast(0).coerceAtMost(source.length)
        val safeEnd = (expanded.last + 1).coerceAtLeast(safeStart).coerceAtMost(source.length)
        if (safeStart >= safeEnd) return prefix

        val snippet = source.substring(safeStart, safeEnd)
        return """$prefix
```
$snippet
```""".trimIndent()
    }
}
