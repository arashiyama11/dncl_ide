package io.github.arashiyama11.dncl_ide.language_server.ast

import io.github.arashiyama11.dncl_ide.interpreter.model.AstNode
import io.github.arashiyama11.dncl_ide.interpreter.model.Symbol
import io.github.arashiyama11.dncl_ide.interpreter.model.SymbolKind
import io.github.arashiyama11.dncl_ide.interpreter.model.SymbolTable

class SymbolTableBuilder {
    private val globalSymbolTable = SymbolTable()
    private val scopes: MutableList<Pair<SymbolTable, IntRange>> = mutableListOf()

    init {
        scopes.add(globalSymbolTable to IntRange(0, Int.MAX_VALUE))
    }

    fun build(program: AstNode.Program): SymbolTable {
        visitProgram(program)
        return globalSymbolTable
    }

    private fun currentScope(): SymbolTable = scopes.last().first
    private fun currentScopeRange(): IntRange = scopes.last().second

    private fun enterScope(range: IntRange) {
        scopes.add(currentScope().createChildScope() to range)
    }

    private fun exitScope() {
        if (scopes.size > 1) {
            scopes.removeLast()
        }
    }

    private fun visitProgram(program: AstNode.Program) {
        scopes[0] = scopes[0].first to program.range
        program.statements.forEach { visitStatement(it) }
    }

    private fun visitStatement(statement: AstNode.Statement) {
        when (statement) {
            is AstNode.AssignStatement -> {
                statement.assignments.forEach { (assignable, expression) ->
                    if (assignable is AstNode.Identifier) {
                        currentScope().define(
                            Symbol(
                                name = assignable.value,
                                kind = SymbolKind.VARIABLE,
                                range = assignable.range,
                                scopeRange = currentScopeRange(),
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
                enterScope(statement.consequence.range)
                visitStatement(statement.consequence)
                exitScope()
                statement.alternative?.let {
                    enterScope(it.range)
                    visitStatement(it)
                    exitScope()
                }
            }

            is AstNode.ForStatement -> {
                enterScope(statement.block.range)
                currentScope().define(
                    Symbol(
                        name = statement.loopCounter.literal,
                        kind = SymbolKind.VARIABLE,
                        range = statement.loopCounter.range,
                        scopeRange = statement.block.range
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
                enterScope(statement.block.range)
                visitStatement(statement.block)
                exitScope()
            }

            is AstNode.FunctionStatement -> {
                currentScope().define(
                    Symbol(
                        name = statement.name.literal,
                        kind = SymbolKind.FUNCTION,
                        range = statement.name.range,
                        scopeRange = currentScopeRange(),
                        definitionNode = statement
                    )
                )
                enterScope(statement.range)
                statement.parameters.forEach { param ->
                    println("defining parameter: ${param.literal} at ${statement.range}")
                    currentScope().define(
                        Symbol(
                            name = param.literal,
                            kind = SymbolKind.PARAMETER,
                            range = param.range, // パラメータの正確なrangeは後で改善
                            scopeRange = statement.range
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
                enterScope(expression.range)
                expression.parameters.forEach { param ->
                    println("defining parameter: ${param.literal} at ${expression.range}")
                    currentScope().define(
                        Symbol(
                            name = param.literal,
                            kind = SymbolKind.PARAMETER,
                            range = param.range, // パラメータの正確なrangeは後で改善
                            scopeRange = expression.range
                        )
                    )
                }
                visitStatement(expression.body)
                exitScope()
            }

            is AstNode.Identifier, is AstNode.IntLiteral, is AstNode.FloatLiteral, is AstNode.StringLiteral, is AstNode.BooleanLiteral, is AstNode.SystemLiteral, is AstNode.WhileExpression -> {
                // これらのノードは子ノードを持たないか、シンボル定義に関わらない
            }
        }
    }
}