package io.github.arashiyama11.dncl_ide.interpreter.parser

import io.github.arashiyama11.dncl_ide.interpreter.model.AstNode
import io.github.arashiyama11.dncl_ide.interpreter.model.Symbol
import io.github.arashiyama11.dncl_ide.interpreter.model.SymbolKind
import io.github.arashiyama11.dncl_ide.interpreter.model.SymbolTable

class SymbolTableBuilder {
    private val globalSymbolTable = SymbolTable()
    private var currentScope: SymbolTable = globalSymbolTable

    fun build(program: AstNode.Program): SymbolTable {
        visitProgram(program)
        return globalSymbolTable
    }

    private fun enterScope() {
        currentScope = currentScope.createChildScope()
    }

    private fun exitScope() {
        // TODO: 親スコープに戻る仕組み��実装する必要がある
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
                        currentScope.define(
                            Symbol(
                                name = assignable.value,
                                kind = SymbolKind.VARIABLE,
                                range = assignable.range,
                                definitionNode = assignable
                            )
                        )
                    }
                    visitExpression(expression)
                }
            }

            is AstNode.ExpressionStatement -> visitExpression(statement.expression)
            is AstNode.IfStatement -> {
                visitExpression(statement.condition)
                enterScope()
                visitStatement(statement.consequence)
                exitScope()
                statement.alternative?.let {
                    enterScope()
                    visitStatement(it)
                    exitScope()
                }
            }

            is AstNode.ForStatement -> {
                enterScope()
                currentScope().define(
                    Symbol(
                        name = statement.loopCounter.literal,
                        kind = SymbolKind.VARIABLE,
                        range = statement.loopCounter.range,
                        definitionNode = null // Token.IdentifierはAstNodeではないのでnull
                    )
                )
                visitExpression(statement.start)
                visitExpression(statement.end)
                visitExpression(statement.step)
                visitStatement(statement.block)
                exitScope()
            }

            is AstNode.WhileStatement -> {
                visitExpression(statement.condition)
                enterScope()
                visitStatement(statement.block)
                exitScope()
            }

            is AstNode.FunctionStatement -> {
                currentScope.define(
                    Symbol(
                        name = statement.name,
                        kind = SymbolKind.FUNCTION,
                        range = statement.range,
                        definitionNode = statement
                    )
                )
                enterScope()
                statement.parameters.forEach { param ->
                    // パラメータもシンボルとして定義
                    // TODO: パラメータのrangeを正確に取得する方法を検討
                    currentScope.define(
                        Symbol(
                            name = param,
                            kind = SymbolKind.PARAMETER,
                            range = IntRange.EMPTY,
                            definitionNode = null
                        )
                    )
                }
                visitStatement(statement.block)
                exitScope()
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
                enterScope()
                expression.parameters.forEach { param ->
                    // TODO: パラメータのrangeを正確に取得する方法を検討
                    currentScope.define(
                        Symbol(
                            name = param,
                            kind = SymbolKind.PARAMETER,
                            range = IntRange.EMPTY,
                            definitionNode = null
                        )
                    )
                }
                visitStatement(expression.body)
                exitScope()
            }

            is AstNode.Identifier, is AstNode.IntLiteral, is AstNode.FloatLiteral, is AstNode.StringLiteral, is AstNode.BooleanLiteral, is AstNode.SystemLiteral, is AstNode.WhileExpression -> {
                // これらのノードは子ノードを持たないか、シンボル定義��関わらない
            }
        }
    }
}