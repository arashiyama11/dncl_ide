package io.github.arashiyama11.dncl_ide.interpreter.model


sealed interface AstNode {
    val literal: String
    val range: IntRange
    val filePath: String?
    override fun toString(): String

    sealed interface Statement : AstNode
    sealed interface Expression : AstNode
    sealed interface Assignable : Expression

    data class PrefixExpression(
        val operator: PrefixExpressionToken,
        val right: Expression,
        override val range: IntRange,
        override val filePath: String? = right.filePath ?: operator.filePath
    ) : Expression {
        override val literal: String
            get() = "(${operator.literal}${right.literal})"

        override fun toString() = literal
    }

    data class InfixExpression(
        val left: Expression,
        val operator: InfixExpressionToken,
        val right: Expression,
        override val filePath: String? = left.filePath ?: operator.filePath ?: right.filePath
    ) : Expression {
        override val literal: String
            get() = "(${left.literal} ${operator.literal} ${right.literal})"
        override val range: IntRange
            get() = left.range.first..right.range.last
    }

    data class IndexExpression(
        val left: Expression,
        val right: Expression,
        override val filePath: String? = left.filePath ?: right.filePath
    ) : Expression, Assignable {
        override val literal: String
            get() = "${left.literal}[${right.literal}]"

        override val range: IntRange
            get() = left.range.first..right.range.last
    }

    data class Program(
        val statements: List<Statement>,
        override val filePath: String? = statements.firstOrNull()?.filePath
    ) : AstNode {
        override val literal: String
            get() = statements.joinToString(separator = "\n") { it.literal }
        override val range: IntRange
            get() = statements.firstOrNull()?.range?.first?.let { first ->
                statements.lastOrNull()?.range?.last?.let { last ->
                    first..last
                } ?: first..first
            } ?: 0..0
    }

    data class AssignStatement(
        val assignments: List<Pair<Assignable, Expression>>,
        override val filePath: String? = assignments.firstOrNull()?.first?.filePath
    ) : Statement {
        override val literal: String
            get() = assignments.joinToString(separator = ", ") { "${it.first.literal} = ${it.second.literal}" }

        override val range: IntRange
            get() = assignments.firstOrNull()?.first?.range?.first?.let { first ->
                assignments.lastOrNull()?.second?.range?.last?.let { last ->
                    first..last
                } ?: first..first
            } ?: 0..0
    }


    data class ExpressionStatement(
        val expression: Expression,
        override val filePath: String? = expression.filePath
    ) : Statement {
        override val literal: String
            get() = expression.literal

        override val range: IntRange
            get() = expression.range
    }

    data class IfStatement(
        val condition: Expression,
        val consequence: BlockStatement,
        val alternative: BlockStatement?,
        override val filePath: String? = condition.filePath ?: consequence.filePath ?: alternative?.filePath
    ) : Statement {
        override val literal: String
            get() = "if ${condition.literal} ${consequence.literal}${alternative?.literal?.let { " else $it" } ?: ""}"
        override val range: IntRange
            get() = condition.range.first..(alternative?.range?.last ?: consequence.range.last)
    }


    data class ForStatement(
        val loopCounter: Identifier,
        val start: Expression,
        val end: Expression,
        val step: Expression,
        val stepType: StepType,
        val block: BlockStatement,
        override val filePath: String? = loopCounter.filePath ?: block.filePath
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
        val block: BlockStatement,
        override val filePath: String? = condition.filePath ?: block.filePath
    ) : Statement {
        override val literal: String
            get() = "while ${condition.literal} ${block.literal}"

        override val range: IntRange
            get() = condition.range.first..block.range.last
    }

    data class WhileExpression(
        val condition: Expression,
        val block: BlockStatement,
        override val filePath: String? = condition.filePath ?: block.filePath
    ) : Expression {
        override val literal: String
            get() = "while ${condition.literal} ${block.literal}"

        override val range: IntRange
            get() = condition.range.first..block.range.last

        fun toStatement() = WhileStatement(condition, block)
    }

    data class FunctionStatement(
        val name: Token,
        val parameters: List<Token>,
        val block: BlockStatement,
        override val range: IntRange,
        override val filePath: String? = name.filePath ?: parameters.firstOrNull()?.filePath
    ) : Statement {
        override val literal: String
            get() = "function ${name.literal}(${parameters.joinToString(separator = ", ") { it.literal }}) ${block.literal}"

        /*override val range: IntRange
            get() = block.range.first - 1..block.range.last + 1*/
    }

    data class BlockStatement(
        val statements: List<Statement>,
        override val filePath: String? = statements.firstOrNull()?.filePath
    ) : Statement {
        override val literal: String
            get() = "{\n${statements.joinToString(separator = "\n") { it.literal }}\n}"
        override val range: IntRange
            get() = statements.firstOrNull()?.range?.first?.let { first ->
                statements.lastOrNull()?.range?.last?.let { last ->
                    first..last
                } ?: first..first
            } ?: 0..0
    }

    data class Identifier(
        val value: String,
        override val range: IntRange,
        override val filePath: String? = null
    ) : Expression,
        Assignable {
        override val literal: String
            get() = value
    }

    data class IntLiteral(
        val value: Int,
        override val range: IntRange,
        override val filePath: String? = null
    ) : Expression {
        override val literal: String
            get() = value.toString()
    }

    data class FloatLiteral(
        val value: Float,
        override val range: IntRange,
        override val filePath: String? = null
    ) : Expression {
        override val literal: String
            get() = value.toString()
    }

    data class StringLiteral(
        val value: String,
        override val range: IntRange,
        override val filePath: String? = null
    ) : Expression {
        override val literal: String
            get() = "\"$value\""
    }

    data class ArrayLiteral(
        val elements: List<Expression>,
        override val range: IntRange,
        override val filePath: String? = elements.firstOrNull()?.filePath
    ) :
        Expression {
        override val literal: String
            get() = "[${elements.joinToString(separator = ", ") { it.literal }}]"
    }

    data class SystemLiteral(
        val value: String,
        override val range: IntRange,
        override val filePath: String? = null
    ) : Expression {
        override val literal: String
            get() = value
    }

    data class FunctionLiteral(
        val parameters: List<Token>,
        val body: BlockStatement,
        override val filePath: String? = body.filePath ?: parameters.firstOrNull()?.filePath
    ) : Expression {
        override val literal: String
            get() = "function(${parameters.joinToString(separator = ", ") { it.literal }}) ${body.literal}"

        override val range: IntRange
            get() = body.range
    }

    data class CallExpression(
        val function: Expression,
        val arguments: List<Expression>,
        override val filePath: String? = function.filePath
    ) : Expression {
        override val literal: String
            get() = "${function.literal}(${arguments.joinToString(separator = ", ") { it.literal }})"

        override val range: IntRange
            get() = function.range.first..(arguments.lastOrNull()?.range?.last?.plus(2)
                ?: (function.range.last + 3))
    }

    data class BooleanLiteral(
        val value: Boolean,
        override val range: IntRange,
        override val filePath: String? = null
    ) : Expression {
        override val literal: String
            get() = if (value) "真" else "偽"
    }
}
