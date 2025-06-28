package io.github.arashiyama11.dncl_ide.language_server.service

import arrow.core.Either
import io.github.arashiyama11.dncl_ide.interpreter.lexer.Lexer
import io.github.arashiyama11.dncl_ide.interpreter.model.AstNode
import io.github.arashiyama11.dncl_ide.language_server.ast.Symbol
import io.github.arashiyama11.dncl_ide.language_server.ast.SymbolTable
import io.github.arashiyama11.dncl_ide.interpreter.parser.Parser
import io.github.arashiyama11.dncl_ide.language_server.ast.AstVisitor
import io.github.arashiyama11.dncl_ide.language_server.ast.SymbolKind

data class AstInfo(
    val ast: AstNode.Program,
    val symbolTable: SymbolTable
)

class AstInfoService {
    fun parseAndAnalyze(code: String): AstInfo? {
        return try {
            val lexer = Lexer(code)
            val parserResult = Parser(lexer)
            val parser = when (parserResult) {
                is Either.Left -> return null
                is Either.Right -> parserResult.value
            }

            val programResult = parser.parseProgram()
            val program = when (programResult) {
                is Either.Left -> return null
                is Either.Right -> programResult.value
            }

            val visitor = AstVisitor()
            val symbolTable = visitor.visit(program)

            AstInfo(program, symbolTable)
        } catch (e: Exception) {
            null
        }
    }

    fun findNodeAtOffset(astInfo: AstInfo, offset: Int): AstNode? {
        return findNodeRecursive(astInfo.ast, offset)
    }

    fun findSymbolAtOffset(astInfo: AstInfo, offset: Int): Symbol? {
        // First, find the node at the offset to understand the context
        val node = findNodeAtOffset(astInfo, offset)

        // If it's an identifier, try to resolve it from the appropriate scope
        if (node is AstNode.Identifier) {
            // Find the appropriate scope for this offset
            val scopeSymbolTable = findScopeForOffset(astInfo.ast, offset, astInfo.symbolTable)
            return scopeSymbolTable.resolve(node.value, offset)
        }

        // Also check if we're in a function name or parameter position
        return findSymbolInAst(astInfo.ast, offset, astInfo.symbolTable)
    }

    private fun findScopeForOffset(
        node: AstNode,
        offset: Int,
        globalScope: SymbolTable
    ): SymbolTable {
        if (offset !in node.range) return globalScope

        return when (node) {
            is AstNode.Program -> {
                // Check if we're inside any function
                node.statements.forEach { statement ->
                    val scope = findScopeForOffset(statement, offset, globalScope)
                    if (scope != globalScope) return scope
                }
                globalScope
            }

            is AstNode.FunctionStatement -> {
                // If we're inside the function body, return the function's scope
                if (offset in node.range) {
                    // Create function scope with parameters and local variables
                    return createFunctionScope(node, globalScope)
                }
                globalScope
            }

            is AstNode.BlockStatement -> {
                node.statements.forEach { statement ->
                    val scope = findScopeForOffset(statement, offset, globalScope)
                    if (scope != globalScope) return scope
                }
                globalScope
            }

            is AstNode.IfStatement -> {
                findScopeForOffset(
                    node.condition,
                    offset,
                    globalScope
                ).let { if (it != globalScope) return it }
                findScopeForOffset(
                    node.consequence,
                    offset,
                    globalScope
                ).let { if (it != globalScope) return it }
                node.alternative?.let { findScopeForOffset(it, offset, globalScope) }
                    ?.let { if (it != globalScope) return it }
                globalScope
            }

            is AstNode.ForStatement -> {
                // For loops have their own scope
                if (offset in node.block.range) {
                    // This would be a child scope, but for now return global
                    return globalScope
                }
                globalScope
            }

            is AstNode.WhileStatement -> {
                findScopeForOffset(
                    node.condition,
                    offset,
                    globalScope
                ).let { if (it != globalScope) return it }
                findScopeForOffset(
                    node.block,
                    offset,
                    globalScope
                ).let { if (it != globalScope) return it }
                globalScope
            }

            is AstNode.AssignStatement -> {
                node.assignments.forEach { (assignable, expression) ->
                    findScopeForOffset(
                        assignable,
                        offset,
                        globalScope
                    ).let { if (it != globalScope) return it }
                    findScopeForOffset(
                        expression,
                        offset,
                        globalScope
                    ).let { if (it != globalScope) return it }
                }
                globalScope
            }

            is AstNode.ExpressionStatement -> {
                findScopeForOffset(node.expression, offset, globalScope)
            }

            is AstNode.InfixExpression -> {
                findScopeForOffset(
                    node.left,
                    offset,
                    globalScope
                ).let { if (it != globalScope) return it }
                findScopeForOffset(
                    node.right,
                    offset,
                    globalScope
                ).let { if (it != globalScope) return it }
                globalScope
            }

            is AstNode.PrefixExpression -> {
                findScopeForOffset(node.right, offset, globalScope)
            }

            is AstNode.CallExpression -> {
                findScopeForOffset(
                    node.function,
                    offset,
                    globalScope
                ).let { if (it != globalScope) return it }
                node.arguments.forEach { arg ->
                    findScopeForOffset(
                        arg,
                        offset,
                        globalScope
                    ).let { if (it != globalScope) return it }
                }
                globalScope
            }

            is AstNode.IndexExpression -> {
                findScopeForOffset(
                    node.left,
                    offset,
                    globalScope
                ).let { if (it != globalScope) return it }
                findScopeForOffset(
                    node.right,
                    offset,
                    globalScope
                ).let { if (it != globalScope) return it }
                globalScope
            }

            is AstNode.ArrayLiteral -> {
                node.elements.forEach { element ->
                    findScopeForOffset(
                        element,
                        offset,
                        globalScope
                    ).let { if (it != globalScope) return it }
                }
                globalScope
            }

            else -> globalScope
        }
    }

    private fun createFunctionScope(
        functionNode: AstNode.FunctionStatement,
        globalScope: SymbolTable
    ): SymbolTable {
        val childScope = globalScope.createChildScope()

        // Add function parameters to the child scope
        functionNode.parameters.forEach { param ->
            childScope.define(
                Symbol(
                    name = param.literal,
                    kind = SymbolKind.PARAMETER,
                    range = param.range,
                    scopeRange = functionNode.range,
                    definitionNode = null // パラメータはTokenなのでAstNodeではない
                )
            )
        }

        // Add local variables defined within the function
        collectLocalVariables(functionNode.block, childScope, functionNode.range)

        return childScope
    }

    private fun collectLocalVariables(node: AstNode, scope: SymbolTable, scopeRange: IntRange) {
        when (node) {
            is AstNode.BlockStatement -> {
                node.statements.forEach { statement ->
                    collectLocalVariables(statement, scope, scopeRange)
                }
            }

            is AstNode.AssignStatement -> {
                node.assignments.forEach { (assignable, expression) ->
                    if (assignable is AstNode.Identifier) {
                        scope.define(
                            Symbol(
                                name = assignable.value,
                                kind = SymbolKind.VARIABLE,
                                range = assignable.range,
                                scopeRange = scopeRange,
                                definitionNode = assignable
                            )
                        )
                    }
                    collectLocalVariables(expression, scope, scopeRange)
                }
            }

            is AstNode.IfStatement -> {
                collectLocalVariables(node.condition, scope, scopeRange)
                collectLocalVariables(node.consequence, scope, scopeRange)
                node.alternative?.let { collectLocalVariables(it, scope, scopeRange) }
            }

            is AstNode.ForStatement -> {
                // Add loop counter as local variable
                scope.define(
                    Symbol(
                        name = node.loopCounter.literal,
                        kind = SymbolKind.VARIABLE,
                        range = node.loopCounter.range,
                        scopeRange = scopeRange,
                        definitionNode = node.loopCounter
                    )
                )
                collectLocalVariables(node.start, scope, scopeRange)
                collectLocalVariables(node.end, scope, scopeRange)
                collectLocalVariables(node.step, scope, scopeRange)
                collectLocalVariables(node.block, scope, scopeRange)
            }

            is AstNode.WhileStatement -> {
                collectLocalVariables(node.condition, scope, scopeRange)
                collectLocalVariables(node.block, scope, scopeRange)
            }

            is AstNode.ExpressionStatement -> {
                collectLocalVariables(node.expression, scope, scopeRange)
            }

            is AstNode.InfixExpression -> {
                collectLocalVariables(node.left, scope, scopeRange)
                collectLocalVariables(node.right, scope, scopeRange)
            }

            is AstNode.PrefixExpression -> {
                collectLocalVariables(node.right, scope, scopeRange)
            }

            is AstNode.CallExpression -> {
                collectLocalVariables(node.function, scope, scopeRange)
                node.arguments.forEach { arg ->
                    collectLocalVariables(arg, scope, scopeRange)
                }
            }

            is AstNode.IndexExpression -> {
                collectLocalVariables(node.left, scope, scopeRange)
                collectLocalVariables(node.right, scope, scopeRange)
            }

            is AstNode.ArrayLiteral -> {
                node.elements.forEach { element ->
                    collectLocalVariables(element, scope, scopeRange)
                }
            }

            // Leaf nodes don't contain variable definitions
            else -> { /* Do nothing for leaf nodes */
            }
        }
    }

    private fun findNodeRecursive(node: AstNode, offset: Int): AstNode? {
        if (offset !in node.range) {
            return null
        }

        // 複合ノードの場合、子ノードを再帰的に探索
        return when (node) {
            is AstNode.Program -> node.statements.firstNotNullOfOrNull {
                findNodeRecursive(it, offset)
            } ?: node

            is AstNode.FunctionStatement -> {
                // 関数名がオフセットに含まれるかチェック
                if (offset in node.name.range) return AstNode.Identifier(
                    node.name.literal,
                    node.name.range
                )

                // パラメータがオフセットに含まれるかチェック
                node.parameters.firstNotNullOfOrNull { paramToken ->
                    if (offset in paramToken.range) return AstNode.Identifier(
                        paramToken.literal,
                        paramToken.range
                    )
                    null
                } ?: findNodeRecursive(node.block, offset) ?: node
            }

            is AstNode.IfStatement -> {
                findNodeRecursive(node.condition, offset)
                    ?: findNodeRecursive(node.consequence, offset)
                    ?: node.alternative?.let { findNodeRecursive(it, offset) }
                    ?: node
            }

            is AstNode.WhileStatement -> {
                findNodeRecursive(node.condition, offset)
                    ?: findNodeRecursive(node.block, offset)
                    ?: node
            }

            is AstNode.ForStatement -> {
                // ループカウンタがオフセットに含まれるかチェック
                if (offset in node.loopCounter.range) return AstNode.Identifier(
                    node.loopCounter.literal,
                    node.loopCounter.range
                )

                findNodeRecursive(node.start, offset)
                    ?: findNodeRecursive(node.end, offset)
                    ?: findNodeRecursive(node.step, offset)
                    ?: findNodeRecursive(node.block, offset)
                    ?: node
            }

            is AstNode.AssignStatement -> {
                node.assignments.firstNotNullOfOrNull { (assignable, expression) ->
                    findNodeRecursive(assignable, offset)
                        ?: findNodeRecursive(expression, offset)
                } ?: node
            }

            is AstNode.ExpressionStatement -> {
                findNodeRecursive(node.expression, offset) ?: node
            }

            is AstNode.BlockStatement -> {
                node.statements.firstNotNullOfOrNull { findNodeRecursive(it, offset) } ?: node
            }

            is AstNode.InfixExpression -> {
                findNodeRecursive(node.left, offset)
                    ?: findNodeRecursive(node.right, offset)
                    ?: node
            }

            is AstNode.PrefixExpression -> {
                findNodeRecursive(node.right, offset) ?: node
            }

            is AstNode.CallExpression -> {
                findNodeRecursive(node.function, offset)
                    ?: node.arguments.firstNotNullOfOrNull { findNodeRecursive(it, offset) }
                    ?: node
            }

            is AstNode.IndexExpression -> {
                findNodeRecursive(node.left, offset)
                    ?: findNodeRecursive(node.right, offset)
                    ?: node
            }

            is AstNode.ArrayLiteral -> {
                node.elements.firstNotNullOfOrNull { findNodeRecursive(it, offset) } ?: node
            }

            // リーフノード
            is AstNode.Identifier,
            is AstNode.IntLiteral,
            is AstNode.FloatLiteral,
            is AstNode.StringLiteral,
            is AstNode.BooleanLiteral -> node

            else -> node
        }
    }

    private fun findSymbolInAst(node: AstNode, offset: Int, symbolTable: SymbolTable): Symbol? {
        if (offset !in node.range) return null

        return when (node) {
            is AstNode.Program -> {
                node.statements.firstNotNullOfOrNull {
                    findSymbolInAst(it, offset, symbolTable)
                }
            }

            is AstNode.FunctionStatement -> {
                // Check if offset is in function name
                if (offset in node.name.range) {
                    return symbolTable.resolve(node.name.literal, offset)
                }

                // Check if offset is in parameters
                node.parameters.forEach { param ->
                    if (offset in param.range) {
                        return symbolTable.resolve(param.literal, offset)
                    }
                }

                // Check function body
                findSymbolInAst(node.block, offset, symbolTable)
            }

            is AstNode.CallExpression -> {
                // Check if we're calling a function
                val funcResult = findSymbolInAst(node.function, offset, symbolTable)
                if (funcResult != null) return funcResult

                // Check arguments
                node.arguments.firstNotNullOfOrNull {
                    findSymbolInAst(it, offset, symbolTable)
                }
            }

            is AstNode.Identifier -> {
                if (offset in node.range) {
                    symbolTable.resolve(node.value, offset)
                } else null
            }

            is AstNode.AssignStatement -> {
                node.assignments.firstNotNullOfOrNull { (assignable, expression) ->
                    findSymbolInAst(assignable, offset, symbolTable)
                        ?: findSymbolInAst(expression, offset, symbolTable)
                }
            }

            is AstNode.BlockStatement -> {
                node.statements.firstNotNullOfOrNull {
                    findSymbolInAst(it, offset, symbolTable)
                }
            }

            is AstNode.ExpressionStatement -> {
                findSymbolInAst(node.expression, offset, symbolTable)
            }

            is AstNode.IfStatement -> {
                findSymbolInAst(node.condition, offset, symbolTable)
                    ?: findSymbolInAst(node.consequence, offset, symbolTable)
                    ?: node.alternative?.let { findSymbolInAst(it, offset, symbolTable) }
            }

            is AstNode.ForStatement -> {
                // Check loop counter
                if (offset in node.loopCounter.range) {
                    return symbolTable.resolve(node.loopCounter.literal, offset)
                }

                findSymbolInAst(node.start, offset, symbolTable)
                    ?: findSymbolInAst(node.end, offset, symbolTable)
                    ?: findSymbolInAst(node.step, offset, symbolTable)
                    ?: findSymbolInAst(node.block, offset, symbolTable)
            }

            is AstNode.WhileStatement -> {
                findSymbolInAst(node.condition, offset, symbolTable)
                    ?: findSymbolInAst(node.block, offset, symbolTable)
            }

            is AstNode.InfixExpression -> {
                findSymbolInAst(node.left, offset, symbolTable)
                    ?: findSymbolInAst(node.right, offset, symbolTable)
            }

            is AstNode.PrefixExpression -> {
                findSymbolInAst(node.right, offset, symbolTable)
            }

            is AstNode.IndexExpression -> {
                findSymbolInAst(node.left, offset, symbolTable)
                    ?: findSymbolInAst(node.right, offset, symbolTable)
            }

            is AstNode.ArrayLiteral -> {
                node.elements.firstNotNullOfOrNull {
                    findSymbolInAst(it, offset, symbolTable)
                }
            }

            else -> null
        }
    }
}
