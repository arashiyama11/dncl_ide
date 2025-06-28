package io.github.arashiyama11.dncl_ide.language_server.service

import io.github.arashiyama11.dncl_ide.interpreter.model.AstNode
import io.github.arashiyama11.dncl_ide.language_server.Location
import io.github.arashiyama11.dncl_ide.language_server.Position
import io.github.arashiyama11.dncl_ide.language_server.Range
import io.github.arashiyama11.dncl_ide.language_server.ast.Symbol

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
            references.add(createLocation(uri, symbol.range))
        }

        // ASTを走査して同じ名前のシンボルを探す
        collectReferences(astInfo.ast, symbol.name, references, uri)

        return references
    }

    private fun collectReferences(
        node: AstNode,
        targetName: String,
        references: MutableList<Location>,
        uri: String
    ) {
        when (node) {
            is AstNode.Program -> {
                node.statements.forEach { collectReferences(it, targetName, references, uri) }
            }

            is AstNode.FunctionStatement -> {
                // 関数名の参照をチェック
                if (node.name.literal == targetName) {
                    references.add(createLocation(uri, node.name.range))
                }
                collectReferences(node.block, targetName, references, uri)
            }

            is AstNode.Identifier -> {
                if (node.value == targetName) {
                    references.add(createLocation(uri, node.range))
                }
            }

            is AstNode.CallExpression -> {
                collectReferences(node.function, targetName, references, uri)
                node.arguments.forEach { collectReferences(it, targetName, references, uri) }
            }

            is AstNode.AssignStatement -> {
                node.assignments.forEach { (assignable, expression) ->
                    collectReferences(assignable, targetName, references, uri)
                    collectReferences(expression, targetName, references, uri)
                }
            }

            is AstNode.BlockStatement -> {
                node.statements.forEach { collectReferences(it, targetName, references, uri) }
            }

            is AstNode.IfStatement -> {
                collectReferences(node.condition, targetName, references, uri)
                collectReferences(node.consequence, targetName, references, uri)
                node.alternative?.let { collectReferences(it, targetName, references, uri) }
            }

            is AstNode.ForStatement -> {
                // ループカウンタの参照をチェック
                if (node.loopCounter.literal == targetName) {
                    references.add(createLocation(uri, node.loopCounter.range))
                }
                collectReferences(node.start, targetName, references, uri)
                collectReferences(node.end, targetName, references, uri)
                collectReferences(node.step, targetName, references, uri)
                collectReferences(node.block, targetName, references, uri)
            }

            is AstNode.WhileStatement -> {
                collectReferences(node.condition, targetName, references, uri)
                collectReferences(node.block, targetName, references, uri)
            }

            is AstNode.ExpressionStatement -> {
                collectReferences(node.expression, targetName, references, uri)
            }

            is AstNode.InfixExpression -> {
                collectReferences(node.left, targetName, references, uri)
                collectReferences(node.right, targetName, references, uri)
            }

            is AstNode.PrefixExpression -> {
                collectReferences(node.right, targetName, references, uri)
            }

            is AstNode.IndexExpression -> {
                collectReferences(node.left, targetName, references, uri)
                collectReferences(node.right, targetName, references, uri)
            }

            is AstNode.ArrayLiteral -> {
                node.elements.forEach { collectReferences(it, targetName, references, uri) }
            }

            // リーフノードは処理不要
            else -> { /* Do nothing for leaf nodes */
            }
        }
    }

    private fun createLocation(uri: String, range: IntRange): Location {
        return Location(
            uri = uri,
            range = Range(
                start = Position(line = 0, character = range.first),
                end = Position(line = 0, character = range.last + 1)
            )
        )
    }
}
