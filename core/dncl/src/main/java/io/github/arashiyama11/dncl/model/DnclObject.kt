package io.github.arashiyama11.dncl.model

sealed interface DnclObject {
    data class Int(val value: kotlin.Int) : DnclObject {
        override fun toString() = value.toString()
    }

    data class Float(val value: kotlin.Float) : DnclObject {
        override fun toString() = value.toString()
    }

    data class String(val value: kotlin.String) : DnclObject {
        override fun toString() = value
    }

    data class Boolean(val value: kotlin.Boolean) : DnclObject {
        override fun toString() = value.toString()
    }

    data class Array(val value: MutableList<DnclObject>) : DnclObject {
        override fun toString() = value.joinToString(", ", "[", "]")
    }

    data class Function(
        val parameters: List<String>,
        val body: AstNode.Program,
        val env: Environment
    ) : DnclObject {
        override fun toString() = "<fn $parameters>"
    }

    data class BuiltInFunction(val identifier: io.github.arashiyama11.dncl.model.BuiltInFunction) :
        DnclObject {
        override fun toString() = identifier.name
    }

    data object Null : DnclObject {
        override fun toString() = "null"
    }

    data class ReturnValue(val value: DnclObject) : DnclObject {
        override fun toString() = value.toString()
    }

    sealed class Error(open val message: kotlin.String) : DnclObject

    data class TypeError(override val message: kotlin.String) : Error(message) {
        constructor(
            expected: kotlin.String,
            actual: kotlin.String
        ) : this("expected: $expected, actual: $actual")
    }

    data class UndefinedError(override val message: kotlin.String) : Error(message)
}