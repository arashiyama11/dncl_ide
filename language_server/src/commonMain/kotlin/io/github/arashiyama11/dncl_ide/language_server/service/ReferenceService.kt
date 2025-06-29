package io.github.arashiyama11.dncl_ide.language_server.service

import io.github.arashiyama11.dncl_ide.interpreter.model.AstNode
import io.github.arashiyama11.dncl_ide.language_server.Location
import io.github.arashiyama11.dncl_ide.language_server.Range
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

        // 宣言も含める場合は定義位置を追加
        if (includeDeclaration) {
            references.add(createLocation(uri, code, symbol.range))
        }

        // ASTを走査して同じ名前のシンボルを探す
        collectReferences(code, astInfo.ast, symbol.name, references, uri)

        return references
    }

    private fun collectReferences(
        code: String,
        node: AstNode,
        targetName: String,
        references: MutableList<Location>,
        uri: String
    ) {
        when (node) {
            is AstNode.Program -> {
                node.statements.forEach { collectReferences(code, it, targetName, references, uri) }
            }

            is AstNode.FunctionStatement -> {
                // 関数名の参照をチェック
                if (node.name.literal == targetName) {
                    references.add(createLocation(uri, code, node.name.range))
                }
                collectReferences(code, node.block, targetName, references, uri)
            }

            is AstNode.Identifier -> {
                if (node.value == targetName) {
                    references.add(createLocation(uri, code, node.range))
                }
            }

            is AstNode.CallExpression -> {
                collectReferences(code, node.function, targetName, references, uri)
                node.arguments.forEach { collectReferences(code, it, targetName, references, uri) }
            }

            is AstNode.AssignStatement -> {
                node.assignments.forEach { (assignable, expression) ->
                    collectReferences(code, assignable, targetName, references, uri)
                    collectReferences(code, expression, targetName, references, uri)
                }
            }

            is AstNode.BlockStatement -> {
                node.statements.forEach { collectReferences(code, it, targetName, references, uri) }
            }

            is AstNode.IfStatement -> {
                collectReferences(code, node.condition, targetName, references, uri)
                collectReferences(code, node.consequence, targetName, references, uri)
                node.alternative?.let { collectReferences(code, it, targetName, references, uri) }
            }

            is AstNode.ForStatement -> {
                // ループカウンタの参照をチェック
                if (node.loopCounter.literal == targetName) {
                    references.add(createLocation(uri, code, node.loopCounter.range))
                }
                collectReferences(code, node.start, targetName, references, uri)
                collectReferences(code, node.end, targetName, references, uri)
                collectReferences(code, node.step, targetName, references, uri)
                collectReferences(code, node.block, targetName, references, uri)
            }

            is AstNode.WhileStatement -> {
                collectReferences(code, node.condition, targetName, references, uri)
                collectReferences(code, node.block, targetName, references, uri)
            }

            is AstNode.ExpressionStatement -> {
                collectReferences(code, node.expression, targetName, references, uri)
            }

            is AstNode.InfixExpression -> {
                collectReferences(code, node.left, targetName, references, uri)
                collectReferences(code, node.right, targetName, references, uri)
            }

            is AstNode.PrefixExpression -> {
                collectReferences(code, node.right, targetName, references, uri)
            }

            is AstNode.IndexExpression -> {
                collectReferences(code, node.left, targetName, references, uri)
                collectReferences(code, node.right, targetName, references, uri)
            }

            is AstNode.ArrayLiteral -> {
                node.elements.forEach { collectReferences(code, it, targetName, references, uri) }
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
