package io.github.arashiyama11.dncl_ide.language_server

import arrow.core.Either
import io.github.arashiyama11.dncl_ide.interpreter.lexer.Lexer
import io.github.arashiyama11.dncl_ide.interpreter.model.AstNode
import io.github.arashiyama11.dncl_ide.interpreter.model.Symbol
import io.github.arashiyama11.dncl_ide.interpreter.model.SymbolTable
import io.github.arashiyama11.dncl_ide.interpreter.parser.Parser
import io.github.arashiyama11.dncl_ide.interpreter.parser.AstVisitor

class AstInfoService {
    private var currentAst: AstNode.Program? = null
    private var currentSymbolTable: SymbolTable? = null

    fun parseAndAnalyze(code: String) {
        try {
            val lexer = Lexer(code)
            val parserResult = Parser(lexer)
            val parser = when (parserResult) {
                is Either.Left -> {
                    println("Parser creation failed: ${parserResult.value}")
                    return
                }

                is Either.Right -> parserResult.value
            }

            val programResult = parser.parseProgram()
            val program = when (programResult) {
                is Either.Left -> {
                    println("Program parsing failed: ${programResult.value}")
                    return
                }

                is Either.Right -> programResult.value
            }

            val visitor = AstVisitor()
            val symbolTable = visitor.visit(program)

            currentAst = program
            currentSymbolTable = symbolTable

            println("Successfully parsed AST with ${program.statements.size} statements")
        } catch (e: Exception) {
            println("Exception during parsing: ${e.message}")
            e.printStackTrace()
        }
    }

    fun findNodeAtOffset(offset: Int): AstNode? {
        val ast = currentAst ?: return null
        return findNodeRecursive(ast, offset)
    }

    fun findSymbolAtOffset(offset: Int): Symbol? {
        val symbolTable = currentSymbolTable ?: return null

        // First, find the node at the offset
        val node = findNodeAtOffset(offset)

        // If it's an identifier, try to resolve it from the symbol table
        if (node is AstNode.Identifier) {
            return symbolTable.resolve(node.value, offset)
        }

        // Also check if we're in a function name or parameter position
        val ast = currentAst ?: return null
        return findSymbolInAst(ast, offset, symbolTable)
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

    fun getSymbolTable(): SymbolTable? = currentSymbolTable

    fun getAst(): AstNode.Program? = currentAst

    private fun findNodeRecursive(node: AstNode, offset: Int): AstNode? {
        if (offset !in node.range) {
            return null
        }

        // 複合��ードの場合、子ノードを再帰的に探索
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
                if (offset in node.loopCounter.range) return node.loopCounter

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
}
