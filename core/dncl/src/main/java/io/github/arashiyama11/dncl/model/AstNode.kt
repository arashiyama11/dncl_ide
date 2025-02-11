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

    sealed interface InfixExpression : Expression {
        val left: Expression
        val operator: Token
        val right: Expression
        override val literal: String
            get() = "(${left.literal} ${operator.literal} ${right.literal})"

        companion object {
            operator fun invoke(
                left: Expression,
                operator: Token,
                right: Expression
            ): InfixExpression = InfixExpressionImpl(left, operator, right)
        }
    }

    private data class InfixExpressionImpl(
        override val left: Expression,
        override val operator: Token,
        override val right: Expression
    ) : InfixExpression

    data class IndexExpression(
        override val left: Expression,
        override val right: Expression,
        override val operator: Token
    ) : InfixExpression {
        override val literal: String
            get() = "(${left.literal}[${right.literal}])"
    }

    data class Program(val statements: List<Statement>) : AstNode {
        override val literal: String
            get() = statements.joinToString(separator = "\n") { it.literal }
    }

    data class AssignStatement(val name: Token.Identifier, val value: Expression) : Statement {
        override val literal: String
            get() = "${name.literal} = ${value.literal}"
    }

    data class IndexAssignStatement(
        val name: Token.Identifier,
        val index: Expression,
        val value: Expression
    ) : Statement {
        override val literal: String
            get() = "${name.literal}[${index.literal}] = ${value.literal}"
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
            get() = "if ${condition.literal} ${consequence.literal}${alternative?.literal?.let { " else $it" } ?: ""}"
    }

    data class ForStatement(
        val loopCounter: Token.Identifier,
        val start: Expression,
        val end: Expression,
        val step: Expression,
        val stepType: StepType,
        val block: BlockStatement
    ) : Statement {
        override val literal: String
            get() = "for ${loopCounter.literal} in $start..$end $stepType by $step ${block.literal}"

        companion object {
            enum class StepType {
                INCREMENT,
                DECREMENT
            }
        }
    }

    data class WhileExpression(
        val condition: Expression,
        val block: BlockStatement
    ) : Expression {
        override val literal: String
            get() = "while ${condition.literal} ${block.literal}"
    }

    data class BlockStatement(val statements: List<Statement>) : Statement {
        override val literal: String
            get() = "{\n${statements.joinToString(separator = "\n") { it.literal }}\n}"
    }

    data class Identifier(val value: String) : Expression {
        override val literal: String
            get() = value
    }

    data class IntLiteral(val value: Int) : Expression {
        override val literal: String
            get() = value.toString()
    }

    data class FloatLiteral(val value: Float) : Expression {
        override val literal: String
            get() = value.toString()
    }

    data class StringLiteral(val value: String) : Expression {
        override val literal: String
            get() = "\"$value\""
    }

    data class ArrayLiteral(val elements: List<Expression>) : Expression {
        override val literal: String
            get() = "[${elements.joinToString(separator = ", ") { it.literal }}]"
    }

    data class SystemLiteral(val value: String) : Expression {
        override val literal: String
            get() = value
    }

    data class CallExpression(
        val function: Expression,
        val arguments: List<Expression>
    ) : Expression {
        override val literal: String
            get() = "${function.literal}(${arguments.joinToString(separator = ", ") { it.literal }})"
    }
}