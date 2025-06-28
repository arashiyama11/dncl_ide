package io.github.arashiyama11.dncl_ide.language_server

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
        val lexer = Lexer(code)
        val parser = Parser(lexer).getOrNull() ?: return
        val program = parser.parseProgram().getOrNull() ?: return

        val visitor = AstVisitor()
        val symbolTable = visitor.visit(program)

        currentAst = program
        currentSymbolTable = symbolTable
    }

    fun findNodeAtOffset(offset: Int): AstNode? {
        val ast = currentAst ?: return null
        return findNodeRecursive(ast, offset)
    }

    fun findSymbolAtOffset(offset: Int): Symbol? {
        val node = findNodeAtOffset(offset)
        if (node is AstNode.Identifier) {
            return currentSymbolTable?.resolve(node.value)
        }
        return null
    }

    private fun findNodeRecursive(node: AstNode, offset: Int): AstNode? {
        if (offset !in node.range) {
            return null
        }

        // 複合ノードの場合、子ノードを再帰的に探索
        return when (node) {
            is AstNode.Program -> node.statements.firstNotNullOfOrNull {
                findNodeRecursive(
                    it,
                    offset
                )
            } ?: node

            is AstNode.BlockStatement -> node.statements.firstNotNullOfOrNull {
                findNodeRecursive(
                    it,
                    offset
                )
            } ?: node

            is AstNode.ExpressionStatement -> findNodeRecursive(node.expression, offset) ?: node
            is AstNode.AssignStatement -> {
                node.assignments.firstNotNullOfOrNull { (assignable, expression) ->
                    findNodeRecursive(assignable, offset) ?: findNodeRecursive(expression, offset)
                } ?: node
            }

            is AstNode.FunctionStatement -> {
                findNodeRecursive(node.block, offset) ?: node
            }

            is AstNode.IfStatement -> {
                findNodeRecursive(node.condition, offset)
                    ?: findNodeRecursive(node.consequence, offset)
                    ?: node.alternative?.let { findNodeRecursive(it, offset) }
                    ?: node
            }

            is AstNode.ForStatement -> {
                findNodeRecursive(node.start, offset)
                    ?: findNodeRecursive(node.end, offset)
                    ?: findNodeRecursive(node.step, offset)
                    ?: findNodeRecursive(node.block, offset)
                    ?: node
            }

            is AstNode.WhileStatement -> {
                findNodeRecursive(node.condition, offset) ?: findNodeRecursive(node.block, offset)
                ?: node
            }

            is AstNode.CallExpression -> {
                findNodeRecursive(node.function, offset)
                    ?: node.arguments.firstNotNullOfOrNull { findNodeRecursive(it, offset) }
                    ?: node
            }

            is AstNode.InfixExpression -> {
                findNodeRecursive(node.left, offset) ?: findNodeRecursive(node.right, offset)
                ?: node
            }

            is AstNode.PrefixExpression -> {
                findNodeRecursive(node.right, offset) ?: node
            }

            is AstNode.IndexExpression -> {
                findNodeRecursive(node.left, offset) ?: findNodeRecursive(node.right, offset)
                ?: node
            }

            is AstNode.ArrayLiteral -> {
                node.elements.firstNotNullOfOrNull { findNodeRecursive(it, offset) } ?: node
            }

            is AstNode.FunctionLiteral -> {
                findNodeRecursive(node.body, offset) ?: node
            }

            is AstNode.WhileExpression -> {
                findNodeRecursive(node.condition, offset) ?: findNodeRecursive(node.block, offset)
                ?: node
            }
            // リーフノード
            is AstNode.Identifier,
            is AstNode.BooleanLiteral,
            is AstNode.FloatLiteral,
            is AstNode.IntLiteral,
            is AstNode.StringLiteral,
            is AstNode.SystemLiteral -> node
        }
    }
}
