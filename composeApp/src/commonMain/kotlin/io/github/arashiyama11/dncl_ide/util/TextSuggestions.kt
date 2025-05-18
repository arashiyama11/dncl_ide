package io.github.arashiyama11.dncl_ide.util

import io.github.arashiyama11.dncl_ide.interpreter.lexer.Lexer
import io.github.arashiyama11.dncl_ide.interpreter.model.AstNode
import io.github.arashiyama11.dncl_ide.interpreter.model.BuiltInFunction
import io.github.arashiyama11.dncl_ide.interpreter.model.Token
import io.github.arashiyama11.dncl_ide.interpreter.parser.Parser
import kotlin.math.max
import kotlin.math.min

class TextSuggestions() {

    private data class Definition(val literal: String, val position: Int?)

    fun suggest(code: String, position: Int): List<String> {
        val parser = Parser(Lexer(code)).fold(ifLeft = { return emptyList() }) { it }
        val program = parser.parseProgram().fold(ifLeft = { return emptyList() }) { it }
        val tokens = Lexer(code).toList()
        val positionTokenIndex =
            tokens.indexOfFirst { it.getOrNull()?.range?.contains(position) == true }
        val currentToken =
            tokens.slice(
                max(
                    0,
                    positionTokenIndex - 1
                )..min(positionTokenIndex + 1, tokens.size - 1)
            )
                .firstOrNull { it.getOrNull() is Token.Identifier || it.getOrNull() is Token.Japanese }
        val globalVariables =
            BuiltInFunction.allIdentifiers().map { Definition(it, null) } + globalVariables(program)
        val words =
            (filterDefinition(
                statements = program.statements,
                Int.MAX_VALUE,
                position
            ) + globalVariables).distinctBy { it.literal }
        println("!!!!!!!!!!!!!!currentToken: $currentToken")
        return (if (currentToken?.isRight() == true) {
            val id = currentToken.getOrNull()!!.literal
            words.sortedWith(
                compareByDescending<Definition> {
                    val minLengths = min(it.literal.length, id.length)
                    for (i in 0 until minLengths) {
                        if (it.literal[i] != id[i]) return@compareByDescending i
                    }
                    minLengths
                }.thenBy {
                    if (it.position == null) return@thenBy Int.MAX_VALUE
                    if (position > it.position) code.length * 5 else
                        position - it.position
                }
            )
        } else words.sortedBy {
            if (it.position == null) return@sortedBy Int.MAX_VALUE
            if (position > it.position) -it.position else code.length * 5
        }).map { it.literal }
    }

    private fun globalVariables(program: AstNode.Program): List<Definition> {
        return filterDefinition(program.statements, 1, Int.MAX_VALUE)
    }

    private fun filterDefinition(
        statements: List<AstNode.Statement>,
        depth: Int,
        limitPosition: Int
    ): List<Definition> {
        if (depth == 0) return emptyList()
        if (statements.isEmpty()) return emptyList()
        val result = mutableListOf<Definition>()
        for (stmt in statements) {
            if (stmt.range.first > limitPosition) return result
            when (stmt) {
                is AstNode.AssignStatement -> {
                    result.addAll(stmt.assignments.map {
                        Definition(
                            it.first.literal,
                            it.first.range.first
                        )
                    })
                }

                is AstNode.BlockStatement -> {
                    result.addAll(filterDefinition(stmt.statements, depth - 1, limitPosition))
                }

                is AstNode.ExpressionStatement -> {}
                is AstNode.ForStatement -> {
                    result.add(Definition(stmt.loopCounter.literal, stmt.loopCounter.range.first))
                    result.addAll(filterDefinition(stmt.block.statements, depth - 1, limitPosition))
                }

                is AstNode.FunctionStatement -> {
                    result.add(Definition(stmt.name, stmt.range.first))
                    if (stmt.range.contains(limitPosition)) result.addAll(
                        filterDefinition(
                            stmt.block.statements,
                            depth - 1,
                            limitPosition
                        )
                    )
                }

                is AstNode.IfStatement -> {
                    result.addAll(
                        filterDefinition(
                            stmt.consequence.statements,
                            depth - 1,
                            limitPosition
                        )
                    )
                    stmt.alternative?.let {
                        result.addAll(
                            filterDefinition(
                                it.statements,
                                depth - 1, limitPosition
                            )
                        )
                    }
                }

                is AstNode.WhileStatement -> {
                    result.addAll(filterDefinition(stmt.block.statements, depth - 1, limitPosition))
                }
            }
        }
        return result
    }
}