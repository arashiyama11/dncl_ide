package io.github.arashiyama11.dncl_ide.domain.usecase

import arrow.core.Either
import io.github.arashiyama11.dncl_ide.domain.model.Definition
import io.github.arashiyama11.dncl_ide.interpreter.lexer.Lexer
import io.github.arashiyama11.dncl_ide.interpreter.model.AllBuiltInFunction
import io.github.arashiyama11.dncl_ide.interpreter.model.AstNode
import io.github.arashiyama11.dncl_ide.interpreter.model.DnclError
import io.github.arashiyama11.dncl_ide.interpreter.model.Token
import io.github.arashiyama11.dncl_ide.interpreter.parser.Parser
import kotlin.math.max
import kotlin.math.min

class SuggestionUseCase() {
    fun suggestWhenFailingParse(
        code: String,
        position: Int
    ): List<Definition> {
        val fixedCode = code.substring(0 until position) + "u" + code.substring(position)
        val lexer = Lexer(fixedCode)
        val prog = (Parser(lexer)).fold({
            return emptyList()
        }) { it.parseProgram() }.fold({ return emptyList() }) { it }
        return suggestWithParsedData(
            code, position, lexer.toList(), prog
        )

    }

    fun suggestWithParsedData(
        code: String,
        position: Int,
        tokens: List<Either<DnclError, Token>>,
        program: AstNode.Program
    ): List<Definition> {
        val positionTokenIndex =
            tokens.indexOfFirst { it.getOrNull()?.range?.contains(position) == true }
        val currentToken =
            tokens.slice(
                max(
                    0,
                    positionTokenIndex - 1
                ) until min(positionTokenIndex + 1, tokens.size)
            )
                .firstOrNull { it.getOrNull() is Token.Identifier || it.getOrNull() is Token.Japanese }
                ?: tokens.getOrNull(positionTokenIndex)
        val globalVariables =
            AllBuiltInFunction.allIdentifiers()
                .map { Definition(it, null, true) } + globalVariables(
                program
            )
        val words =
            (filterDefinition(
                statements = program.statements,
                Int.MAX_VALUE,
                position
            ) + globalVariables).distinctBy { it.literal }

        return (if (currentToken?.isRight() == true) {
            val compByWord = when (val token = currentToken.getOrNull()!!) {
                is Token.Japanese, is Token.Identifier -> {
                    val id = token.literal
                    compareByDescending<Definition> {
                        val minLengths = min(it.literal.length, id.length)
                        for (i in 0 until minLengths) {
                            if (it.literal[i] != id[i]) return@compareByDescending i
                        }
                        minLengths
                    }
                }

                else -> {
                    Comparator<Definition> { _, _ -> 0 }
                }
            }

            words.sortedWith(
                compByWord.thenBy {
                    if (it.position == null) return@thenBy Int.MAX_VALUE
                    if (position > it.position) code.length * 5 else
                        position - it.position
                }
            )
        } else words.sortedBy {
            if (it.position == null) return@sortedBy Int.MAX_VALUE
            if (position > it.position) -it.position else code.length * 5
        })
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
                            it.first.range.first,
                            it.second is AstNode.FunctionLiteral
                        )
                    })
                }

                is AstNode.BlockStatement -> {
                    result.addAll(filterDefinition(stmt.statements, depth - 1, limitPosition))
                }

                is AstNode.ExpressionStatement -> {}
                is AstNode.ForStatement -> {
                    result.add(
                        Definition(
                            stmt.loopCounter.literal,
                            stmt.loopCounter.range.first,
                            false
                        )
                    )
                    result.addAll(filterDefinition(stmt.block.statements, depth - 1, limitPosition))
                }

                is AstNode.FunctionStatement -> {
                    result.add(Definition(stmt.name, stmt.range.first, true))
                    if (stmt.range.contains(limitPosition)) {
                        result.addAll(stmt.parameters.map {
                            Definition(
                                it,
                                stmt.range.first,
                                false
                            )
                        })
                        result.addAll(
                            filterDefinition(
                                stmt.block.statements,
                                depth - 1,
                                limitPosition
                            )
                        )
                    }
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