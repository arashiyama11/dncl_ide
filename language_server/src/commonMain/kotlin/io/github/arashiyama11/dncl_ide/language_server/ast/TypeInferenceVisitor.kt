
package io.github.arashiyama11.dncl_ide.language_server.ast

import io.github.arashiyama11.dncl_ide.interpreter.model.AstNode

class TypeInferenceVisitor(private val symbolTable: SymbolTable) {
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
        return symbolTable
    }

    private fun visitProgram(program: AstNode.Program) {
        program.statements.forEach { visit(it) }
    }

    private fun visitBlockStatement(block: AstNode.BlockStatement) {
        block.statements.forEach { visit(it) }
    }

    private fun visitAssignStatement(assignStmt: AstNode.AssignStatement) {
        assignStmt.assignments.forEach { (assignable, expression) ->
            val expressionType = inferExpressionType(expression)
            if (assignable is AstNode.Identifier) {
                val symbol = symbolTable.resolve(assignable.value, assignable.range.first)
                if (symbol != null) {
                    symbolTable.define(symbol.copy(type = expressionType))
                }
            }
        }
    }

    private fun inferExpressionType(expression: AstNode): String {
        return when (expression) {
            is AstNode.IntLiteral -> "Int"
            is AstNode.FloatLiteral -> "Float"
            is AstNode.StringLiteral -> "String"
            is AstNode.BooleanLiteral -> "Boolean"
            else -> "Unknown"
        }
    }

    private fun visitFunctionStatement(functionStmt: AstNode.FunctionStatement) {
        visit(functionStmt.block)
    }

    private fun visitIdentifier(identifier: AstNode.Identifier) {}

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
        visit(forStmt.start)
        visit(forStmt.end)
        visit(forStmt.step)
        visit(forStmt.block)
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
        visit(functionLiteral.body)
    }

    private fun visitWhileExpression(whileExpression: AstNode.WhileExpression) {
        visit(whileExpression.condition)
        visit(whileExpression.block)
    }
}
