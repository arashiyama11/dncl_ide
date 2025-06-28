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

        // カーソル位置のシンボルを取得（スコープ認識版を使用）
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
                    checkNodeForReference(assignable, targetSymbol, code, uri, references)
                    findReferencesInNode(expression, targetSymbol, code, uri, references)
                }
            }

            is AstNode.ExpressionStatement -> {
                findReferencesInNode(node.expression, targetSymbol, code, uri, references)
            }

            is AstNode.FunctionStatement -> {
                // 関数名のチェック
                if (node.name.literal == targetSymbol.name &&
                    isSameSymbol(node.name.range, targetSymbol)
                ) {
                    addReferenceLocation(node.name.range, code, uri, references)
                }
                // パラメータのチェック
                node.parameters.forEach { param ->
                    if (param.literal == targetSymbol.name &&
                        isSameSymbol(param.range, targetSymbol)
                    ) {
                        addReferenceLocation(param.range, code, uri, references)
                    }
                }
                // 関数本体をチェック
                findReferencesInNode(node.block, targetSymbol, code, uri, references)
            }

            is AstNode.Identifier -> {
                checkNodeForReference(node, targetSymbol, code, uri, references)
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
                if (node.loopCounter.value == targetSymbol.name &&
                    isSameSymbol(node.loopCounter.range, targetSymbol)
                ) {
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

    private fun checkNodeForReference(
        node: AstNode,
        targetSymbol: Symbol,
        code: String,
        uri: String,
        references: MutableList<Location>
    ) {
        if (node is AstNode.Identifier && node.value == targetSymbol.name) {
            // より簡単なスコープチェック：範囲で比較
            if (isInSameScope(node.range, targetSymbol)) {
                addReferenceLocation(node.range, code, uri, references)
            }
        }
    }

    private fun isInSameScope(nodeRange: IntRange, targetSymbol: Symbol): Boolean {
        // シンボルが同じスコープにあるかチェック
        // 簡単な実装：ターゲットシンボルのスコープ範囲内��ノードがあるかチェック
        return nodeRange.first >= targetSymbol.scopeRange.first &&
                nodeRange.last <= targetSymbol.scopeRange.last
    }

    private fun isSameSymbol(range: IntRange, targetSymbol: Symbol): Boolean {
        // より厳密でないシンボル比較：名前とスコープ範囲で判定
        return range == targetSymbol.range || isInSameScope(range, targetSymbol)
    }

    private fun isSameSymbol(symbol1: Symbol, symbol2: Symbol): Boolean {
        // シンボルが同じかどうかをチェック（名前、種類、範囲で判定）
        return symbol1.name == symbol2.name &&
                symbol1.kind == symbol2.kind &&
                symbol1.range == symbol2.range
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
