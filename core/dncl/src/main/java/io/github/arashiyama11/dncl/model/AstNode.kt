package io.github.arashiyama11.dncl.model


sealed interface AstNode {
    val literal: String
    override fun toString(): String

    sealed interface Statement : AstNode
    sealed interface Expression : AstNode
    data class PrefixExpression(
        val operator: PrefixExpressionToken,
        val right: Expression
    ) : Expression {
        override val literal: String
            get() = "(${operator.literal}${right.literal})"

        override fun toString() = literal
    }

    data class InfixExpression(
        val left: Expression,
        val operator: InfixExpressionToken,
        val right: Expression
    ) : Expression {
        override val literal: String
            get() = "(${left.literal} ${operator.literal} ${right.literal})"
    }

    data class IndexExpression(
        val left: Expression,
        val right: Expression,
    ) : Expression, Assignable {
        override val literal: String
            get() = "${left.literal}[${right.literal}]"
    }

    data class Program(val statements: List<Statement>) : AstNode {
        override val literal: String
            get() = statements.joinToString(separator = "\n") { it.literal }
    }

    data class AssignStatement(val assignments: List<Pair<Assignable, Expression>>) :
        Statement {
        override val literal: String
            get() = assignments.joinToString(separator = ", ") { "${it.first.literal} = ${it.second.literal}" }
    }

    sealed interface Assignable : Expression

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

    data class WhileStatement(
        val condition: Expression,
        val block: BlockStatement
    ) : Statement {
        override val literal: String
            get() = "while ${condition.literal} ${block.literal}"
    }

    data class WhileExpression(
        val condition: Expression,
        val block: BlockStatement
    ) : Expression {
        override val literal: String
            get() = "while ${condition.literal} ${block.literal}"

        fun toStatement() = WhileStatement(condition, block)
    }

    data class BlockStatement(val statements: List<Statement>) : Statement {
        override val literal: String
            get() = "{\n${statements.joinToString(separator = "\n") { it.literal }}\n}"
    }

    data class Identifier(val value: String) : Expression, Assignable {
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