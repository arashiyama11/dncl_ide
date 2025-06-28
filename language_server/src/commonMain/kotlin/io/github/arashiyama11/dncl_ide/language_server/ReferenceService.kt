package io.github.arashiyama11.dncl_ide.language_server

import io.github.arashiyama11.dncl_ide.interpreter.model.AstNode
import io.github.arashiyama11.dncl_ide.interpreter.model.Symbol

class ReferenceService(
    private val diagnosticService: DiagnosticService,
    private val astInfoService: AstInfoService
) {
    fun getReferences(uri: String, code: String, offset: Int): List<Location> {
        // ASTを解析
        astInfoService.parseAndAnalyze(code)

        // カーソル位置のシンボルを取得
        val targetSymbol = astInfoService.findSymbolAtOffset(offset)
            ?: return emptyList()

        // ASTを走査して、同じシンボルの全参照箇所を検索
        val program = astInfoService.findNodeAtOffset(0) as? AstNode.Program
            ?: return emptyList()

        val references = mutableListOf<Location>()
        findReferencesInNode(program, targetSymbol, code, uri, references)

        return references
    }

    private fun findReferencesInNode(
        node: AstNode,
        targetSymbol: Symbol,
        code: String,
        uri: String,
        references: MutableList<Location>
    ) {
        when (node) {
            is AstNode.Program -> {
                node.statements.forEach {
                    findReferencesInNode(it, targetSymbol, code, uri, references)
                }
            }

            is AstNode.BlockStatement -> {
                node.statements.forEach {
                    findReferencesInNode(it, targetSymbol, code, uri, references)
                }
            }

            is AstNode.AssignStatement -> {
                node.assignments.forEach { (assignable, expression) ->
                    findReferencesInNode(assignable, targetSymbol, code, uri, references)
                    findReferencesInNode(expression, targetSymbol, code, uri, references)
                }
            }

            is AstNode.FunctionStatement -> {
                // 関数名のチェックは文字列なので、範囲を作成して確認
                if (node.name == targetSymbol.name) {
                    addReferenceLocation(node, code, uri, references)
                }
                // パラメータのチェック（文字列リスト）
                node.parameters.forEach { param ->
                    if (param == targetSymbol.name) {
                        addReferenceLocation(node, code, uri, references) // パラメータの正確な範囲は後で改善
                    }
                }
                // 関数本体をチェック
                findReferencesInNode(node.block, targetSymbol, code, uri, references)
            }

            is AstNode.Identifier -> {
                if (node.value == targetSymbol.name) {
                    addReferenceLocation(node, code, uri, references)
                }
            }

            is AstNode.CallExpression -> {
                findReferencesInNode(node.function, targetSymbol, code, uri, references)
                node.arguments.forEach { arg ->
                    findReferencesInNode(arg, targetSymbol, code, uri, references)
                }
            }

            is AstNode.IfStatement -> {
                findReferencesInNode(node.condition, targetSymbol, code, uri, references)
                findReferencesInNode(node.consequence, targetSymbol, code, uri, references)
                node.alternative?.let {
                    findReferencesInNode(
                        it,
                        targetSymbol,
                        code,
                        uri,
                        references
                    )
                }
            }

            is AstNode.WhileStatement -> {
                findReferencesInNode(node.condition, targetSymbol, code, uri, references)
                findReferencesInNode(node.block, targetSymbol, code, uri, references)
            }

            is AstNode.ForStatement -> {
                findReferencesInNode(node.start, targetSymbol, code, uri, references)
                findReferencesInNode(node.end, targetSymbol, code, uri, references)
                findReferencesInNode(node.step, targetSymbol, code, uri, references)
                findReferencesInNode(node.block, targetSymbol, code, uri, references)
            }

            is AstNode.ExpressionStatement -> {
                findReferencesInNode(node.expression, targetSymbol, code, uri, references)
            }

            is AstNode.InfixExpression -> {
                findReferencesInNode(node.left, targetSymbol, code, uri, references)
                findReferencesInNode(node.right, targetSymbol, code, uri, references)
            }

            is AstNode.PrefixExpression -> {
                findReferencesInNode(node.right, targetSymbol, code, uri, references)
            }

            is AstNode.IndexExpression -> {
                findReferencesInNode(node.left, targetSymbol, code, uri, references)
                findReferencesInNode(node.right, targetSymbol, code, uri, references)
            }

            is AstNode.ArrayLiteral -> {
                node.elements.forEach { element ->
                    findReferencesInNode(element, targetSymbol, code, uri, references)
                }
            }

            // リーフノード - 何もしない
            is AstNode.IntLiteral,
            is AstNode.FloatLiteral,
            is AstNode.StringLiteral,
            is AstNode.BooleanLiteral -> {
                // リテラル内に参照は存在しない
            }

            else -> {
                // その他のノードタイプを処理
            }
        }
    }

    private fun addReferenceLocation(
        node: AstNode,
        code: String,
        uri: String,
        references: MutableList<Location>
    ) {
        val (startLine, startChar) = diagnosticService.calculateLineAndCharacter(
            code,
            node.range.first
        )
        val (endLine, endChar) = diagnosticService.calculateLineAndCharacter(code, node.range.last)

        references.add(
            Location(
                uri = uri,
                range = Range(
                    start = Position(startLine, startChar),
                    end = Position(endLine, endChar)
                )
            )
        )
    }
}
