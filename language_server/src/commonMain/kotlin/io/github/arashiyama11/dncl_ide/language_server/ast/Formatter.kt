package io.github.arashiyama11.dncl_ide.language_server.ast

import io.github.arashiyama11.dncl_ide.interpreter.model.AstNode

class Formatter {
    private val indentSize = 4
    private var currentIndent = 0
    private val builder = StringBuilder()

    fun format(program: AstNode.Program): String {
        currentIndent = 0
        builder.clear()
        visitProgram(program)
        return builder.toString()
    }

    private fun appendIndent() {
        builder.append(" ".repeat(currentIndent))
    }

    private fun visitProgram(program: AstNode.Program) {
        program.statements.forEach { statement ->
            visitStatement(statement)
            builder.append("\n")
        }
    }

    private fun visitStatement(statement: AstNode.Statement) {
        when (statement) {
            is AstNode.AssignStatement -> {
                appendIndent()
                statement.assignments.forEachIndexed { index, (assignable, expression) ->
                    visitAssignable(assignable)
                    builder.append(" = ")
                    visitExpression(expression)
                    if (index < statement.assignments.size - 1) {
                        builder.append(", ")
                    }
                }
            }

            is AstNode.ExpressionStatement -> {
                appendIndent()
                visitExpression(statement.expression)
            }

            is AstNode.IfStatement -> {
                appendIndent()
                builder.append("もし ")
                visitExpression(statement.condition)
                builder.append(" ならば:\n")
                currentIndent += indentSize
                visitBlockStatement(statement.consequence)
                currentIndent -= indentSize
                statement.alternative?.let {
                    appendIndent()
                    builder.append("そうでなければ:\n")
                    currentIndent += indentSize
                    visitBlockStatement(it)
                    currentIndent -= indentSize
                }
            }

            is AstNode.ForStatement -> {
                appendIndent()
                builder.append("繰り返し ")
                visitIdentifier(
                    AstNode.Identifier(
                        statement.loopCounter.literal,
                        statement.loopCounter.range
                    )
                )
                builder.append(" を ")
                visitExpression(statement.start)
                builder.append(" から ")
                visitExpression(statement.end)
                builder.append(" まで ")
                builder.append(if (statement.stepType == AstNode.ForStatement.Companion.StepType.INCREMENT) "ずつ増やす" else "ずつ減らす")
                builder.append(" :\n")
                currentIndent += indentSize
                visitBlockStatement(statement.block)
                currentIndent -= indentSize
            }

            is AstNode.WhileStatement -> {
                appendIndent()
                builder.append("間 ")
                visitExpression(statement.condition)
                builder.append(" :\n")
                currentIndent += indentSize
                visitBlockStatement(statement.block)
                currentIndent -= indentSize
            }

            is AstNode.FunctionStatement -> {
                appendIndent()
                builder.append("関数 ")
                builder.append(statement.name)
                builder.append("(")
                statement.parameters.forEachIndexed { index, param ->
                    builder.append(param)
                    if (index < statement.parameters.size - 1) {
                        builder.append(", ")
                    }
                }
                builder.append(") :\n")
                currentIndent += indentSize
                visitBlockStatement(statement.block)
                currentIndent -= indentSize
                appendIndent()
                builder.append("定義終わり\n")
            }

            is AstNode.BlockStatement -> {
                visitBlockStatement(statement)
            }
        }
    }

    private fun visitBlockStatement(block: AstNode.BlockStatement) {
        block.statements.forEach { statement ->
            visitStatement(statement)
            builder.append("\n")
        }
    }

    private fun visitExpression(expression: AstNode.Expression) {
        when (expression) {
            is AstNode.Identifier -> visitIdentifier(expression)
            is AstNode.IntLiteral -> builder.append(expression.value)
            is AstNode.FloatLiteral -> builder.append(expression.value)
            is AstNode.StringLiteral -> builder.append("\"${expression.value}\"")
            is AstNode.BooleanLiteral -> builder.append(if (expression.value) "真" else "偽")
            is AstNode.SystemLiteral -> builder.append("＜${expression.value}＞")
            is AstNode.PrefixExpression -> {
                builder.append(expression.operator.literal)
                visitExpression(expression.right)
            }

            is AstNode.InfixExpression -> {
                visitExpression(expression.left)
                builder.append(" ${expression.operator.literal} ")
                visitExpression(expression.right)
            }

            is AstNode.IndexExpression -> {
                visitExpression(expression.left)
                builder.append("[")
                visitExpression(expression.right)
                builder.append("]")
            }

            is AstNode.CallExpression -> {
                visitExpression(expression.function)
                builder.append("(")
                expression.arguments.forEachIndexed { index, arg ->
                    visitExpression(arg)
                    if (index < expression.arguments.size - 1) {
                        builder.append(", ")
                    }
                }
                builder.append(")")
            }

            is AstNode.ArrayLiteral -> {
                builder.append("[")
                expression.elements.forEachIndexed { index, element ->
                    visitExpression(element)
                    if (index < expression.elements.size - 1) {
                        builder.append(", ")
                    }
                }
                builder.append("]")
            }

            is AstNode.FunctionLiteral -> {
                builder.append("関数(")
                expression.parameters.forEachIndexed { index, param ->
                    builder.append(param)
                    if (index < expression.parameters.size - 1) {
                        builder.append(", ")
                    }
                }
                builder.append(") :\n")
                currentIndent += indentSize
                visitBlockStatement(expression.body)
                currentIndent -= indentSize
                appendIndent()
                builder.append("定義終わり")
            }

            is AstNode.WhileExpression -> {
                builder.append("間 ")
                visitExpression(expression.condition)
                builder.append(" :\n")
                currentIndent += indentSize
                visitBlockStatement(expression.block)
                currentIndent -= indentSize
                appendIndent()
                builder.append("終わり")
            }
        }
    }

    private fun visitAssignable(assignable: AstNode.Assignable) {
        when (assignable) {
            is AstNode.Identifier -> visitIdentifier(assignable)
            is AstNode.IndexExpression -> visitExpression(assignable)
        }
    }

    private fun visitIdentifier(identifier: AstNode.Identifier) {
        builder.append(identifier.value)
    }
}