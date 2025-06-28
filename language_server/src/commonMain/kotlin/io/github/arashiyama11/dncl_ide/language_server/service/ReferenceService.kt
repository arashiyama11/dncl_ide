package io.github.arashiyama11.dncl_ide.language_server.service

import io.github.arashiyama11.dncl_ide.interpreter.model.AstNode
import io.github.arashiyama11.dncl_ide.language_server.Location
import io.github.arashiyama11.dncl_ide.language_server.Position
import io.github.arashiyama11.dncl_ide.language_server.Range
import io.github.arashiyama11.dncl_ide.language_server.ast.Symbol
import io.github.arashiyama11.dncl_ide.language_server.util.calculateLineAndCharacter

class ReferenceService(
    private val astInfoService: AstInfoService
) {
    fun getReferences(uri: String, code: String, offset: Int): List<Location> {
        // ASTを解析
        astInfoService.parseAndAnalyze(code)
        println("offset: $offset")

        // カーソル位置のシンボルを取得（スコープ認識版を使用）
        val targetSymbol = astInfoService.findSymbolAtOffset(offset)
            ?: return emptyList()

        println("targetSymbol: $targetSymbol")

        // ASTを走査して、同じシンボルの全参照箇所を検索
        val program = astInfoService.getAst()
            ?: return emptyList()


        val references = mutableListOf<Location>()

        // 定義箇所を追加
        addReferenceLocation(targetSymbol.range, code, uri, references)

        // ASTを走査して参照箇所を収集
        fun collectReferences(node: AstNode) {
            when (node) {
                is AstNode.Program -> node.statements.forEach { collectReferences(it) }
                is AstNode.BlockStatement -> node.statements.forEach { collectReferences(it) }
                is AstNode.AssignStatement -> {
                    node.assignments.forEach { (assignable, expression) ->
                        if (assignable is AstNode.Identifier) {
                            val resolvedSymbol =
                                astInfoService.findSymbolAtOffset(assignable.range.first)
                            if (resolvedSymbol != null && isSameSymbol(
                                    resolvedSymbol,
                                    targetSymbol
                                )
                            ) {
                                addReferenceLocation(assignable.range, code, uri, references)
                            }
                        }
                        collectReferences(expression)
                    }
                }

                is AstNode.ExpressionStatement -> collectReferences(node.expression)
                is AstNode.FunctionStatement -> {
                    val resolvedSymbol = astInfoService.findSymbolAtOffset(node.name.range.first)
                    if (resolvedSymbol != null && isSameSymbol(resolvedSymbol, targetSymbol)) {
                        addReferenceLocation(node.name.range, code, uri, references)
                    }
                    node.parameters.forEach { param ->
                        val paramResolvedSymbol =
                            astInfoService.findSymbolAtOffset(param.range.first)
                        if (paramResolvedSymbol != null && isSameSymbol(
                                paramResolvedSymbol,
                                targetSymbol
                            )
                        ) {
                            addReferenceLocation(param.range, code, uri, references)
                        }
                    }
                    collectReferences(node.block)
                }

                is AstNode.Identifier -> {
                    val resolvedSymbol = astInfoService.findSymbolAtOffset(node.range.first)
                    if (resolvedSymbol != null && isSameSymbol(resolvedSymbol, targetSymbol)) {
                        addReferenceLocation(node.range, code, uri, references)
                    }
                }

                is AstNode.CallExpression -> {
                    val resolvedSymbol =
                        astInfoService.findSymbolAtOffset(node.function.range.first)
                    if (resolvedSymbol != null && isSameSymbol(resolvedSymbol, targetSymbol)) {
                        addReferenceLocation(node.function.range, code, uri, references)
                    }
                    collectReferences(node.function)
                    node.arguments.forEach { collectReferences(it) }
                }

                is AstNode.IfStatement -> {
                    collectReferences(node.condition)
                    collectReferences(node.consequence)
                    node.alternative?.let { collectReferences(it) }
                }

                is AstNode.WhileStatement -> {
                    collectReferences(node.condition)
                    collectReferences(node.block)
                }

                is AstNode.ForStatement -> {
                    val resolvedSymbol =
                        astInfoService.findSymbolAtOffset(node.loopCounter.range.first)
                    if (resolvedSymbol != null && isSameSymbol(resolvedSymbol, targetSymbol)) {
                        addReferenceLocation(node.loopCounter.range, code, uri, references)
                    }
                    collectReferences(node.start)
                    collectReferences(node.end)
                    collectReferences(node.step)
                    collectReferences(node.block)
                }

                is AstNode.InfixExpression -> {
                    collectReferences(node.left)
                    collectReferences(node.right)
                }

                is AstNode.PrefixExpression -> collectReferences(node.right)
                is AstNode.IndexExpression -> {
                    collectReferences(node.left)
                    collectReferences(node.right)
                }

                is AstNode.ArrayLiteral -> node.elements.forEach { collectReferences(it) }
                is AstNode.IntLiteral, is AstNode.FloatLiteral, is AstNode.StringLiteral, is AstNode.BooleanLiteral, is AstNode.SystemLiteral, is AstNode.FunctionLiteral, is AstNode.WhileExpression -> {
                    // リーフノードまたは処理不要なノード
                }
            }
        }

        collectReferences(program)

        // 重複を削除し、ソート
        return references.distinctBy { it.range.start.line to it.range.start.character }
            .sortedWith(compareBy({ it.range.start.line }, { it.range.start.character }))
    }

    private fun isSameSymbol(symbol1: Symbol, symbol2: Symbol): Boolean {
        // シンボルが同じかどうかを厳密にチェック（名前、種類、定義範囲、スコープ範囲で判定）
        return symbol1.name == symbol2.name &&
                symbol1.kind == symbol2.kind &&
                symbol1.scopeRange == symbol2.scopeRange
    }

    private fun addReferenceLocation(
        range: IntRange,
        code: String,
        uri: String,
        references: MutableList<Location>
    ) {
        val (startLine, startChar) = calculateLineAndCharacter(code, range.first)
        val (endLine, endChar) = calculateLineAndCharacter(code, range.last)

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