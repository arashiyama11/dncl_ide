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
        val program = astInfoService.getAst()
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

            is AstNode.ExpressionStatement -> {
                findReferencesInNode(node.expression, targetSymbol, code, uri, references)
            }

            is AstNode.FunctionStatement -> {
                // 関数名のチェック
                if (node.name.literal == targetSymbol.name) {
                    addReferenceLocation(node.name.range, code, uri, references)
                }
                // パラメータのチェック
                node.parameters.forEach { param ->
                    if (param.literal == targetSymbol.name) {
                        addReferenceLocation(param.range, code, uri, references)
                    }
                }
                // 関数本体をチェック
                findReferencesInNode(node.block, targetSymbol, code, uri, references)
            }

            is AstNode.Identifier -> {
                if (node.value == targetSymbol.name) {
                    addReferenceLocation(node.range, code, uri, references)
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
                    findReferencesInNode(it, targetSymbol, code, uri, references)
                }
            }

            is AstNode.WhileStatement -> {
                findReferencesInNode(node.condition, targetSymbol, code, uri, references)
                findReferencesInNode(node.block, targetSymbol, code, uri, references)
            }

            is AstNode.ForStatement -> {
                // ループカウンタのチェック
                if (node.loopCounter.value == targetSymbol.name) {
                    addReferenceLocation(node.loopCounter.range, code, uri, references)
                }
                findReferencesInNode(node.start, targetSymbol, code, uri, references)
                findReferencesInNode(node.end, targetSymbol, code, uri, references)
                findReferencesInNode(node.step, targetSymbol, code, uri, references)
                findReferencesInNode(node.block, targetSymbol, code, uri, references)
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
                    findReferencesInNode(it, targetSymbol, code, uri, references)
                }
            }

            // リーフノードは処理済み
            else -> { /* Do nothing for leaf nodes */
            }
        }
    }

    private fun addReferenceLocation(
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
