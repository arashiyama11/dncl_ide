package io.github.arashiyama11.dncl_ide.interpreter.parser

import io.github.arashiyama11.dncl_ide.interpreter.model.AstNode
import io.github.arashiyama11.dncl_ide.interpreter.model.Symbol
import io.github.arashiyama11.dncl_ide.interpreter.model.SymbolKind
import io.github.arashiyama11.dncl_ide.interpreter.model.SymbolTable
import io.github.arashiyama11.dncl_ide.interpreter.model.Token

class AstVisitor {
    private val globalSymbolTable = SymbolTable()
    private val scopes: MutableList<SymbolTable> = mutableListOf(globalSymbolTable)

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

    private fun currentScope(): SymbolTable = scopes.last()

    private fun enterScope() {
        scopes.add(currentScope().createChildScope())
    }

    private fun exitScope() {
        if (scopes.size > 1) {
            scopes.removeLast()
        }
    }

    private fun visitProgram(program: AstNode.Program) {
        program.statements.forEach { visit(it) }
    }

    private fun visitBlockStatement(block: AstNode.BlockStatement) {
        enterScope()
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
                name = functionStmt.name,
                kind = SymbolKind.FUNCTION,
                range = functionStmt.range,
                definitionNode = functionStmt
            )
        )
        enterScope()
        functionStmt.parameters.forEach { paramName ->
            // パラメータはIdentifierではないため、ダミーのAstNode.Identifierを作成してrangeを渡す
            val dummyRange = functionStmt.range // 適切な範囲を設定する必要がある
            currentScope().define(
                Symbol(
                    name = paramName,
                    kind = SymbolKind.PARAMETER,
                    range = dummyRange, // TODO: パラメータの正確な範囲を取得する
                    definitionNode = null // パラメータ自体はASTノードとして存在しないためnull
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
        enterScope()
        currentScope().define(
            Symbol(
                name = forStmt.loopCounter.literal,
                kind = SymbolKind.VARIABLE,
                range = forStmt.loopCounter.range,
                definitionNode = null // Token.IdentifierはAstNodeではないのでnull
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
        enterScope()
        functionLiteral.parameters.forEach { paramName ->
            val dummyRange = functionLiteral.range // 適切���範囲を設定する必要がある
            currentScope().define(
                Symbol(
                    name = paramName,
                    kind = SymbolKind.PARAMETER,
                    range = dummyRange, // TODO: パラメータの正確な範囲を取得する
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
