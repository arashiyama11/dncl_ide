package io.github.arashiyama11.dncl_ide.interpreter.model


sealed interface AstNode {
    val literal: String
    val range: IntRange
    override fun toString(): String

    sealed interface Statement : AstNode
    sealed interface Expression : AstNode
    sealed interface Assignable : Expression

    data class PrefixExpression(
        val operator: PrefixExpressionToken,
        val right: Expression,
        override val range: IntRange
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
        override val range: IntRange
            get() = left.range.first..right.range.last
    }

    data class IndexExpression(
        val left: Expression,
        val right: Expression,
    ) : Expression, Assignable {
        override val literal: String
            get() = "${left.literal}[${right.literal}]"

        override val range: IntRange
            get() = left.range.first..right.range.last
    }

    data class Program(val statements: List<Statement>) : AstNode {
        override val literal: String
            get() = statements.joinToString(separator = "\n") { it.literal }
        override val range: IntRange
            get() = statements.firstOrNull()?.range?.first?.let { first ->
                statements.lastOrNull()?.range?.last?.let { last ->
                    first..last
                } ?: first..first
            } ?: 0..0
    }

    data class AssignStatement(val assignments: List<Pair<Assignable, Expression>>) :
        Statement {
        override val literal: String
            get() = assignments.joinToString(separator = ", ") { "${it.first.literal} = ${it.second.literal}" }

        override val range: IntRange
            get() = assignments.firstOrNull()?.first?.range?.first?.let { first ->
                assignments.lastOrNull()?.second?.range?.last?.let { last ->
                    first..last
                } ?: first..first
            } ?: 0..0
    }


    data class ExpressionStatement(val expression: Expression) : Statement {
        override val literal: String
            get() = expression.literal

        override val range: IntRange
            get() = expression.range
    }

    data class IfStatement(
        val condition: Expression,
        val consequence: BlockStatement,
        val alternative: BlockStatement?
    ) : Statement {
        override val literal: String
            get() = "if ${condition.literal} ${consequence.literal}${alternative?.literal?.let { " else $it" } ?: ""}"
        override val range: IntRange
            get() = condition.range.first..(alternative?.range?.last ?: consequence.range.last)
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

        override val range: IntRange
            get() = loopCounter.range.first..block.range.last

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

        override val range: IntRange
            get() = condition.range.first..block.range.last
    }

    data class WhileExpression(
        val condition: Expression,
        val block: BlockStatement
    ) : Expression {
        override val literal: String
            get() = "while ${condition.literal} ${block.literal}"

        override val range: IntRange
            get() = condition.range.first..block.range.last

        fun toStatement() = WhileStatement(condition, block)
    }

    data class FunctionStatement(
        val name: String,
        val parameters: List<String>,
        val block: BlockStatement
    ) : Statement {
        override val literal: String
            get() = "function ${name}(${parameters.joinToString(separator = ", ") { it }}) ${block.literal}"

        override val range: IntRange
            get() = block.range
    }

    data class BlockStatement(val statements: List<Statement>) : Statement {
        override val literal: String
            get() = "{\n${statements.joinToString(separator = "\n") { it.literal }}\n}"
        override val range: IntRange
            get() = statements.firstOrNull()?.range?.first?.let { first ->
                statements.lastOrNull()?.range?.last?.let { last ->
                    first..last
                } ?: first..first
            } ?: 0..0
    }

    data class Identifier(val value: String, override val range: IntRange) : Expression,
        Assignable {
        override val literal: String
            get() = value
    }

    data class IntLiteral(val value: Int, override val range: IntRange) : Expression {
        override val literal: String
            get() = value.toString()
    }

    data class FloatLiteral(val value: Float, override val range: IntRange) : Expression {
        override val literal: String
            get() = value.toString()
    }

    data class StringLiteral(val value: String, override val range: IntRange) : Expression {
        override val literal: String
            get() = "\"$value\""
    }

    data class ArrayLiteral(val elements: List<Expression>, override val range: IntRange) :
        Expression {
        override val literal: String
            get() = "[${elements.joinToString(separator = ", ") { it.literal }}]"
    }

    data class SystemLiteral(val value: String, override val range: IntRange) : Expression {
        override val literal: String
            get() = value
    }

    data class FunctionLiteral(
        val parameters: List<String>,
        val body: BlockStatement
    ) : Expression {
        override val literal: String
            get() = "function(${parameters.joinToString(separator = ", ") { it }}) ${body.literal}"

        override val range: IntRange
            get() = body.range
    }

    data class CallExpression(
        val function: Expression,
        val arguments: List<Expression>
    ) : Expression {
        override val literal: String
            get() = "${function.literal}(${arguments.joinToString(separator = ", ") { it.literal }})"

        override val range: IntRange
            get() = function.range.first..(arguments.lastOrNull()?.range?.last
                ?: function.range.last)
    }
}