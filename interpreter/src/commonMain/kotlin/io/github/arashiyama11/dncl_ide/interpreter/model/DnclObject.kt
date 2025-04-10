package io.github.arashiyama11.dncl_ide.interpreter.model

sealed interface DnclObject {
    val astNode: AstNode
    fun hash(): kotlin.Int

    data class Int(val value: kotlin.Int, override val astNode: AstNode) : DnclObject {
        override fun toString() = value.toString()
        override fun hash() = value.hashCode()
    }

    data class Float(val value: kotlin.Float, override val astNode: AstNode) : DnclObject {
        override fun toString() = value.toString()
        override fun hash() = value.hashCode()
    }

    data class String(val value: kotlin.String, override val astNode: AstNode) : DnclObject {
        override fun toString() = value
        override fun hash() = value.hashCode()
    }

    data class Boolean(val value: kotlin.Boolean, override val astNode: AstNode) : DnclObject {
        override fun toString() = value.toString()
        override fun hash() = value.hashCode()
    }

    data class Array(val value: MutableList<DnclObject>, override val astNode: AstNode) :
        DnclObject {
        override fun toString() = value.joinToString(", ", "[", "]")
        override fun hash() = value.hashCode()
    }

    data class Function(
        val parameters: List<kotlin.String>,
        val body: AstNode.BlockStatement,
        val env: Environment, override val astNode: AstNode
    ) : DnclObject {
        override fun toString() = "<fn $parameters>"
        override fun hash() =
            parameters.hashCode() + 31 * body.hashCode() + 31 * 31 * env.hashCode()
    }

    data class BuiltInFunction(
        val identifier: io.github.arashiyama11.dncl_ide.interpreter.model.BuiltInFunction,
        override val astNode: AstNode
    ) :
        DnclObject {
        override fun toString() = identifier.name

        override fun hash() = identifier.hashCode()
    }

    data class Null(override val astNode: AstNode) : DnclObject {
        override fun toString() = "null"

        override fun hash() = 0
    }

    data class ReturnValue(val value: DnclObject, override val astNode: AstNode) : DnclObject {
        override fun toString() = value.toString()

        override fun hash() = value.hashCode()

    }

    sealed class Error(open val message: kotlin.String, override val astNode: AstNode) :
        DnclObject {
        override fun toString() = message

        override fun hash() = message.hashCode()
    }

    data class RuntimeError(override val message: kotlin.String, override val astNode: AstNode) :
        Error(message, astNode)

    data class ArgumentSizeError(
        override val message: kotlin.String,
        override val astNode: AstNode
    ) :
        Error(message, astNode)

    data class TypeError(override val message: kotlin.String, override val astNode: AstNode) :
        Error(message, astNode) {
        constructor(
            expected: kotlin.String,
            actual: kotlin.String,
            astNode: AstNode,
        ) : this("expected: $expected, actual: $actual", astNode)

    }

    data class UndefinedError(override val message: kotlin.String, override val astNode: AstNode) :
        Error(message, astNode)

    data class IndexOutOfRangeError(
        val index: kotlin.Int,
        val length: kotlin.Int,
        override val astNode: AstNode
    ) : Error("index: $index, length: $length", astNode)
}