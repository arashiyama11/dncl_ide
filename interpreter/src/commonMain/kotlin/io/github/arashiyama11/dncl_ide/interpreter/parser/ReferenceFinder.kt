package io.github.arashiyama11.dncl_ide.interpreter.parser

import io.github.arashiyama11.dncl_ide.interpreter.model.AstNode
import io.github.arashiyama11.dncl_ide.interpreter.model.SymbolTable
import io.github.arashiyama11.dncl_ide.interpreter.model.Symbol
import io.github.arashiyama11.dncl_ide.interpreter.model.SymbolKind

class ReferenceFinder(private val targetSymbol: Symbol) {
    private val references = mutableListOf<IntRange>()
    private var currentScope: SymbolTable? = null

    fun findReferences(program: AstNode.Program, globalScope: SymbolTable): List<IntRange> {
        currentScope = globalScope
        visitProgram(program)
        return references
    }

    private fun enterScope(newScope: SymbolTable) {
        currentScope = newScope
    }

    private fun exitScope() {
        // TODO: SymbolTableに親スコープへの参照機能を追加する必要がある
        // 現在の実装では簡略化
    }

    private fun visitProgram(program: AstNode.Program) {
        program.statements.forEach { visitStatement(it) }
    }

    private fun visitStatement(statement: AstNode.Statement) {
        when (statement) {
            is AstNode.AssignStatement -> {
                statement.assignments.forEach { (assignable, expression) ->
                    if (assignable is AstNode.Identifier) {
                        checkReference(assignable)
                    }
                    visitExpression(expression)
                }
            }

            is AstNode.ExpressionStatement -> visitExpression(statement.expression)
            is AstNode.IfStatement -> {
                visitExpression(statement.condition)
                // スコープの切り替えはSymbolTableBuilder���行われるため、ここではASTの走査のみ
                visitStatement(statement.consequence)
                statement.alternative?.let { visitStatement(it) }
            }

            is AstNode.ForStatement -> {
                checkReference(
                    AstNode.Identifier(
                        statement.loopCounter.literal,
                        statement.loopCounter.range
                    )
                )
                visitExpression(statement.start)
                visitExpression(statement.end)
                visitExpression(statement.step)
                visitStatement(statement.block)
            }

            is AstNode.WhileStatement -> {
                visitExpression(statement.condition)
                visitStatement(statement.block)
            }

            is AstNode.FunctionStatement -> {
                // 関数定義自体はSymbolTableBuilderで処理済み
                // 関数名が参照されているかチェック
                if (targetSymbol.kind == SymbolKind.FUNCTION && targetSymbol.name == statement.name.literal) {
                    references.add(statement.range) // 関数定義の範囲も参照として追加
                }
                statement.parameters.forEach { param ->
                    // パラメータも参照としてチェック
                    if (targetSymbol.kind == SymbolKind.PARAMETER && targetSymbol.name == param.literal) {
                        // TODO: パラメータのrangeを正確に取得する方法を検討
                        // references.add(paramのrange) // 現状はIdentifierではないので、rangeは取得できない
                    }
                }
                visitStatement(statement.block)
            }

            is AstNode.BlockStatement -> {
                statement.statements.forEach { visitStatement(it) }
            }
        }
    }

    private fun visitExpression(expression: AstNode.Expression) {
        when (expression) {
            is AstNode.PrefixExpression -> visitExpression(expression.right)
            is AstNode.InfixExpression -> {
                visitExpression(expression.left)
                visitExpression(expression.right)
            }

            is AstNode.IndexExpression -> {
                visitExpression(expression.left)
                visitExpression(expression.right)
            }

            is AstNode.CallExpression -> {
                visitExpression(expression.function)
                expression.arguments.forEach { visitExpression(it) }
            }

            is AstNode.ArrayLiteral -> expression.elements.forEach { visitExpression(it) }
            is AstNode.FunctionLiteral -> {
                expression.parameters.forEach { param ->
                    if (targetSymbol.kind == SymbolKind.PARAMETER && targetSymbol.name == param.literal) {
                        // TODO: パラメータのrangeを正確に取得する方法を検討
                        // references.add(paramのrange) // 現状はIdentifierではないので、rangeは取得できない
                    }
                }
                visitStatement(expression.body)
            }

            is AstNode.Identifier -> checkReference(expression)
            is AstNode.IntLiteral, is AstNode.FloatLiteral, is AstNode.StringLiteral, is AstNode.BooleanLiteral, is AstNode.SystemLiteral, is AstNode.WhileExpression -> {
                // これらのノードは参照ではない
            }
        }
    }

    private fun checkReference(identifier: AstNode.Identifier) {
        // 現在のスコープで識別子が解決できるか確認し、解決されたシンボルがターゲットシンボルと一致するか確認
        val resolvedSymbol = currentScope?.resolve(identifier.value, identifier.range.first)
        if (resolvedSymbol == targetSymbol) {
            references.add(identifier.range)
        }
    }
}