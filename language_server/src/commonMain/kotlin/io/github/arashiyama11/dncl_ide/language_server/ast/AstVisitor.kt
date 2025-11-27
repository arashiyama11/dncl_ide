package io.github.arashiyama11.dncl_ide.language_server.ast

import io.github.arashiyama11.dncl_ide.interpreter.model.AllBuiltInFunction
import io.github.arashiyama11.dncl_ide.interpreter.model.AstNode

class AstVisitor {
    private val globalSymbolTable = SymbolTable().apply {
        // グローバルスコープに組み込み関数を定義
        AllBuiltInFunction.allIdentifiers().forEach {
            define(
                Symbol(
                    name = it,
                    kind = SymbolKind.BUILT_IN_FUNCTION,
                    range = IntRange(0, 0), // 組み込み関数は範囲を持たない
                    scopeRange = IntRange(0, Int.MAX_VALUE),
                    filePath = null,
                    definitionNode = null
                )
            )
        }
    }
    private val scopes: MutableList<Pair<SymbolTable, IntRange>> = mutableListOf()

    init {
        scopes.add(globalSymbolTable to IntRange(0, Int.MAX_VALUE))
    }

    fun visit(node: AstNode): SymbolTable {
        when (node) {
            is AstNode.Program -> visitProgram(node)
            is AstNode.BlockStatement -> visitBlockStatement(node)
            is AstNode.AssignStatement -> visitAssignStatement(node)
            is AstNode.FunctionStatement -> visitFunctionStatement(node)
            is AstNode.Identifier -> visitIdentifier(node)
            is AstNode.CallExpression -> visitCallExpression(node)
            is AstNode.IfStatement -> visitIfStatement(node)
            is AstNode.ForStatement -> visitForStatement(node)
            is AstNode.WhileStatement -> visitWhileStatement(node)
            is AstNode.ExpressionStatement -> visitExpressionStatement(node)
            is AstNode.InfixExpression -> visitInfixExpression(node)
            is AstNode.PrefixExpression -> visitPrefixExpression(node)
            is AstNode.IndexExpression -> visitIndexExpression(node)
            is AstNode.ArrayLiteral -> visitArrayLiteral(node)
            is AstNode.BooleanLiteral -> visitBooleanLiteral(node)
            is AstNode.FloatLiteral -> visitFloatLiteral(node)
            is AstNode.IntLiteral -> visitIntLiteral(node)
            is AstNode.StringLiteral -> visitStringLiteral(node)
            is AstNode.SystemLiteral -> visitSystemLiteral(node)
            is AstNode.FunctionLiteral -> visitFunctionLiteral(node)
            is AstNode.WhileExpression -> visitWhileExpression(node)
        }
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
        program.statements.forEach { visit(it) }
    }

    private fun visitBlockStatement(block: AstNode.BlockStatement) {
        enterScope(block.range)
        block.statements.forEach { visit(it) }
        exitScope()
    }

    private fun visitAssignStatement(assignStmt: AstNode.AssignStatement) {
        assignStmt.assignments.forEach { (assignable, expression) ->
            when (assignable) {
                is AstNode.Identifier -> {
                    currentScope().define(
                Symbol(
                    name = assignable.value,
                    kind = SymbolKind.VARIABLE,
                    range = assignable.range,
                    scopeRange = currentScopeRange(),
                    filePath = assignable.filePath,
                    definitionNode = assignable
                )
            )
                    visit(expression)
                }

                is AstNode.IndexExpression -> {
                    visit(assignable)
                    visit(expression)
                }
            }
        }
    }

    private fun visitFunctionStatement(functionStmt: AstNode.FunctionStatement) {
        currentScope().define(
                Symbol(
                    name = functionStmt.name.literal,
                    kind = SymbolKind.FUNCTION,
                    range = functionStmt.name.range,
                    scopeRange = currentScopeRange(),
                    filePath = functionStmt.name.filePath,
                    definitionNode = functionStmt
                )
            )
        enterScope(functionStmt.range)
        functionStmt.parameters.forEach { paramToken ->
            currentScope().define(
                Symbol(
                    name = paramToken.literal,
                    kind = SymbolKind.PARAMETER,
                    range = paramToken.range,
                    scopeRange = functionStmt.range,
                    filePath = paramToken.filePath,
                    definitionNode = null
                )
            )
        }
        visit(functionStmt.block)
        exitScope()
    }

    private fun visitIdentifier(identifier: AstNode.Identifier) {
        // 識別子の使用箇所を記録する必要がある場合はここに追加
        // 現状は定義のみを記録
    }

    private fun visitCallExpression(callExpression: AstNode.CallExpression) {
        visit(callExpression.function)
        callExpression.arguments.forEach { visit(it) }
    }

    private fun visitIfStatement(ifStmt: AstNode.IfStatement) {
        visit(ifStmt.condition)
        visit(ifStmt.consequence)
        ifStmt.alternative?.let { visit(it) }
    }

    private fun visitForStatement(forStmt: AstNode.ForStatement) {
        enterScope(forStmt.block.range)
        currentScope().define(
                Symbol(
                    name = forStmt.loopCounter.literal,
                    kind = SymbolKind.VARIABLE,
                    range = forStmt.loopCounter.range,
                    scopeRange = forStmt.block.range,
                    filePath = forStmt.loopCounter.filePath,
                    definitionNode = forStmt.loopCounter
                )
            )
        visit(forStmt.start)
        visit(forStmt.end)
        visit(forStmt.step)
        visit(forStmt.block)
        exitScope()
    }

    private fun visitWhileStatement(whileStmt: AstNode.WhileStatement) {
        visit(whileStmt.condition)
        visit(whileStmt.block)
    }

    private fun visitExpressionStatement(exprStmt: AstNode.ExpressionStatement) {
        visit(exprStmt.expression)
    }

    private fun visitInfixExpression(infixExpr: AstNode.InfixExpression) {
        visit(infixExpr.left)
        visit(infixExpr.right)
    }

    private fun visitPrefixExpression(prefixExpr: AstNode.PrefixExpression) {
        visit(prefixExpr.right)
    }

    private fun visitIndexExpression(indexExpr: AstNode.IndexExpression) {
        visit(indexExpr.left)
        visit(indexExpr.right)
    }

    private fun visitArrayLiteral(arrayLiteral: AstNode.ArrayLiteral) {
        arrayLiteral.elements.forEach { visit(it) }
    }

    private fun visitBooleanLiteral(booleanLiteral: AstNode.BooleanLiteral) {}
    private fun visitFloatLiteral(floatLiteral: AstNode.FloatLiteral) {}
    private fun visitIntLiteral(intLiteral: AstNode.IntLiteral) {}
    private fun visitStringLiteral(stringLiteral: AstNode.StringLiteral) {}
    private fun visitSystemLiteral(systemLiteral: AstNode.SystemLiteral) {}

    private fun visitFunctionLiteral(functionLiteral: AstNode.FunctionLiteral) {
        enterScope(functionLiteral.range)
        functionLiteral.parameters.forEach { paramToken ->
            currentScope().define(
                Symbol(
                    name = paramToken.literal,
                    kind = SymbolKind.PARAMETER,
                    range = paramToken.range,
                    scopeRange = functionLiteral.range,
                    filePath = paramToken.filePath,
                    definitionNode = null
                )
            )
        }
        visit(functionLiteral.body)
        exitScope()
    }

    private fun visitWhileExpression(whileExpression: AstNode.WhileExpression) {
        visit(whileExpression.condition)
        visit(whileExpression.block)
    }
}
