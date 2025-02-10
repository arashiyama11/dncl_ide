package io.github.arashiyama11.dncl.model

sealed interface AstNode {
    val literal: String
    override fun toString(): String

    sealed interface Statement : AstNode
    sealed interface Expression : AstNode
    data class PrefixExpression(
        val operator: Token,
        val right: Expression
    ) : Expression {
        override val literal: String
            get() = "(${operator.literal}${right.literal})"

        override fun toString() = literal
    }

    data class InfixExpression(
        val left: Expression,
        val operator: Token,
        val right: Expression
    ) : Expression {

        override val literal: String
            get() = "(${left.literal} ${operator.literal} ${right.literal})"
    }

    data class Program(val statements: List<Statement>) : AstNode {
        override val literal: String
            get() = statements.joinToString(separator = "\n") { it.literal }
    }

    data class AssignStatement(val name: Token.Identifier, val value: Expression) : Statement {
        override val literal: String
            get() = "${name.literal} = ${value.literal};"
    }

    data class ExpressionStatement(val expression: Expression) : Statement {
        override val literal: String
            get() = expression.literal
    }

    data class IfStatement(
        val condition: Expression,
        val consequence: BlockStatement,
        val alternative: BlockStatement?
    ) : Statement {
        override val literal: String
            get() = "if (${condition.literal}) ${consequence.literal}${alternative?.literal ?: ""}"
    }

    data class BlockStatement(val statements: List<Statement>) : Statement {
        override val literal: String
            get() = "{\n${statements.joinToString(separator = "\n") { it.literal }}\n}"
    }

    data class Identifier(val value: String) : Expression {
        override val literal: String
            get() = value
    }

    data class Boolean(val value: kotlin.Boolean) : Expression {
        override val literal: String
            get() = value.toString()
    }

    data class IntLiteral(val value: kotlin.Int) : Expression {
        override val literal: String
            get() = value.toString()
    }

    data class FloatLiteral(val value: kotlin.Float) : Expression {
        override val literal: String
            get() = value.toString()
    }


    data class ArrayLiteral(val elements: List<Expression>) : Expression {
        override val literal: String
            get() = "[${elements.joinToString(separator = ", ") { it.literal }}]"
    }
}