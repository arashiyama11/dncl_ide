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
                    findReferencesInNode(
                        it,
                        targetSymbol,
                        code,
                        uri,
                        references
                    )
                }
            }

            is AstNode.BlockStatement -> {
                node.statements.forEach {
                    findReferencesInNode(
                        it,
                        targetSymbol,
                        code,
                        uri,
                        references
                    )
                }
            }

            is AstNode.AssignStatement -> {
                node.assignments.forEach { (assignable, expression) ->
                    findReferencesInNode(assignable, targetSymbol, code, uri, references)
                    findReferencesInNode(expression, targetSymbol, code, uri, references)
                }
            }

            is AstNode.FunctionStatement -> {
                // 関数名の参照をチェック
                if (targetSymbol.name == node.name && targetSymbol.kind == io.github.arashiyama11.dncl_ide.interpreter.model.SymbolKind.FUNCTION) {
                    addReference(node.range, code, uri, references)
                }
                findReferencesInNode(node.block, targetSymbol, code, uri, references)
            }

            is AstNode.Identifier -> {
                // 識別子の参照をチェ��ク
                val symbolAtNode = astInfoService.findSymbolAtOffset(node.range.first)
                if (symbolAtNode != null && isSameSymbol(symbolAtNode, targetSymbol)) {
                    addReference(node.range, code, uri, references)
                }
            }

            is AstNode.CallExpression -> {
                findReferencesInNode(node.function, targetSymbol, code, uri, references)
                node.arguments.forEach {
                    findReferencesInNode(
                        it,
                        targetSymbol,
                        code,
                        uri,
                        references
                    )
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

            is AstNode.ForStatement -> {
                findReferencesInNode(node.start, targetSymbol, code, uri, references)
                findReferencesInNode(node.end, targetSymbol, code, uri, references)
                findReferencesInNode(node.step, targetSymbol, code, uri, references)
                findReferencesInNode(node.block, targetSymbol, code, uri, references)
            }

            is AstNode.WhileStatement -> {
                findReferencesInNode(node.condition, targetSymbol, code, uri, references)
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
                node.elements.forEach {
                    findReferencesInNode(
                        it,
                        targetSymbol,
                        code,
                        uri,
                        references
                    )
                }
            }

            is AstNode.FunctionLiteral -> {
                findReferencesInNode(node.body, targetSymbol, code, uri, references)
            }

            is AstNode.WhileExpression -> {
                findReferencesInNode(node.condition, targetSymbol, code, uri, references)
                findReferencesInNode(node.block, targetSymbol, code, uri, references)
            }
            // リーフノードは処理不要
            else -> { /* リーフノード */
            }
        }
    }

    private fun isSameSymbol(symbol1: Symbol, symbol2: Symbol): Boolean {
        // 同じシンボルかどうかを判定
        // 名前、種類、定義範囲で比較
        return symbol1.name == symbol2.name &&
                symbol1.kind == symbol2.kind &&
                symbol1.range == symbol2.range
    }

    private fun addReference(
        range: IntRange,
        code: String,
        uri: String,
        references: MutableList<Location>
    ) {
        val (startLine, startChar) = diagnosticService.calculateLineAndCharacter(code, range.first)
        val (endLine, endChar) = diagnosticService.calculateLineAndCharacter(code, range.last)

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
