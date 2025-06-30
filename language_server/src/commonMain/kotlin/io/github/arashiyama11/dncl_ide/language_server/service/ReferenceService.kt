package io.github.arashiyama11.dncl_ide.language_server.service

import io.github.arashiyama11.dncl_ide.interpreter.model.AstNode
import io.github.arashiyama11.dncl_ide.language_server.Location
import io.github.arashiyama11.dncl_ide.language_server.Range
import io.github.arashiyama11.dncl_ide.language_server.ast.Symbol
import io.github.arashiyama11.dncl_ide.language_server.util.calculatePosition

class ReferenceService(
    private val astInfoService: AstInfoService
) {
    fun findReferences(
        uri: String,
        code: String,
        offset: Int,
        includeDeclaration: Boolean = true
    ): List<Location> {
        val astInfo = astInfoService.parseAndAnalyze(code) ?: return emptyList()

        // カーソル位置のシンボルを取得
        val symbol = astInfoService.findSymbolAtOffset(astInfo, offset) ?: return emptyList()

        val references = mutableListOf<Location>()

        // ASTを走査して同じ名前のシンボルを探す
        collectReferences(code, astInfo.ast, symbol, references, uri)

        if (!includeDeclaration) {
            val i = references.indexOfFirst { it.range == symbol.definitionNode?.range }
            if (i != -1) {
                references.removeAt(i) // 定義位置を除外
            }
        }

        return references
    }

    private fun collectReferences(
        code: String,
        node: AstNode,
        targetSymbol: Symbol,
        references: MutableList<Location>,
        uri: String,
    ) {
        when (node) {
            is AstNode.Program -> {
                node.statements.forEach {
                    collectReferences(
                        code,
                        it,
                        targetSymbol,
                        references,
                        uri,
                    )
                }
            }

            is AstNode.FunctionStatement -> {
                // 関数名の参照をチェック
                if (node.name.literal == targetSymbol.name && node.name.range.first in targetSymbol.scopeRange) {
                    references.add(createLocation(uri, code, node.name.range))
                }
                collectReferences(code, node.block, targetSymbol, references, uri)
            }

            is AstNode.Identifier -> {
                if (node.value == targetSymbol.name && node.range.first in targetSymbol.scopeRange) {
                    references.add(createLocation(uri, code, node.range))
                }
            }

            is AstNode.CallExpression -> {
                collectReferences(code, node.function, targetSymbol, references, uri)
                node.arguments.forEach {
                    collectReferences(
                        code,
                        it,
                        targetSymbol,
                        references,
                        uri
                    )
                }
            }

            is AstNode.AssignStatement -> {
                node.assignments.forEach { (assignable, expression) ->
                    collectReferences(code, assignable, targetSymbol, references, uri)
                    collectReferences(code, expression, targetSymbol, references, uri)
                }
            }

            is AstNode.BlockStatement -> {
                node.statements.forEach {
                    collectReferences(
                        code,
                        it,
                        targetSymbol,
                        references,
                        uri
                    )
                }
            }

            is AstNode.IfStatement -> {
                collectReferences(code, node.condition, targetSymbol, references, uri)
                collectReferences(code, node.consequence, targetSymbol, references, uri)
                node.alternative?.let { collectReferences(code, it, targetSymbol, references, uri) }
            }

            is AstNode.ForStatement -> {
                // ループカウンタの参照をチェック
                if (node.loopCounter.literal == targetSymbol.name) {
                    references.add(createLocation(uri, code, node.loopCounter.range))
                }
                collectReferences(code, node.start, targetSymbol, references, uri)
                collectReferences(code, node.end, targetSymbol, references, uri)
                collectReferences(code, node.step, targetSymbol, references, uri)
                collectReferences(code, node.block, targetSymbol, references, uri)
            }

            is AstNode.WhileStatement -> {
                collectReferences(code, node.condition, targetSymbol, references, uri)
                collectReferences(code, node.block, targetSymbol, references, uri)
            }

            is AstNode.ExpressionStatement -> {
                collectReferences(code, node.expression, targetSymbol, references, uri)
            }

            is AstNode.InfixExpression -> {
                collectReferences(code, node.left, targetSymbol, references, uri)
                collectReferences(code, node.right, targetSymbol, references, uri)
            }

            is AstNode.PrefixExpression -> {
                collectReferences(code, node.right, targetSymbol, references, uri)
            }

            is AstNode.IndexExpression -> {
                collectReferences(code, node.left, targetSymbol, references, uri)
                collectReferences(code, node.right, targetSymbol, references, uri)
            }

            is AstNode.ArrayLiteral -> {
                node.elements.forEach { collectReferences(code, it, targetSymbol, references, uri) }
            }

            // リーフノードは処理不要
            else -> { /* Do nothing for leaf nodes */
            }
        }
    }

    private fun createLocation(uri: String, code: String, range: IntRange): Location {
        return Location(
            uri = uri,
            range = Range(
                start = calculatePosition(code, range.first),
                end = calculatePosition(code, range.last + 1)
            )
        )
    }
}
