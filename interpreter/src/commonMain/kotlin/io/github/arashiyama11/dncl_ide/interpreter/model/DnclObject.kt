package io.github.arashiyama11.dncl_ide.interpreter.model

import io.github.arashiyama11.dncl_ide.interpreter.api.VirtualFileHandle
import io.github.arashiyama11.dncl_ide.interpreter.evaluator.CallBuiltInFunctionScope


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
        override fun toString() = if (value) "真" else "偽"
        override fun hash() = value.hashCode()
    }

    data class Array(val value: MutableList<DnclObject>, override val astNode: AstNode) :
        DnclObject {
        override fun toString() = value.toMutableList().joinToString(", ", "[", "]")
        override fun hash() = value.hashCode()
    }

    data class File(
        val handle: VirtualFileHandle,
        override val astNode: AstNode
    ) : DnclObject {
        override fun toString() = "<ファイル ${handle.path}>"
        override fun hash() = handle.hashCode()
    }

    data class Function(
        val name: kotlin.String?,
        val parameters: List<kotlin.String>,
        val body: AstNode.BlockStatement,
        val env: Environment, override val astNode: AstNode
    ) : DnclObject {
        override fun toString() = "<関数 ${name ?: "anonymous"}(${parameters.joinToString(", ")})>"
        override fun hash() =
            parameters.hashCode() + 31 * body.hashCode() + 31 * 31 * env.hashCode()
    }

    data class BuiltInFunction(
        val identifier: AllBuiltInFunction,
        override val astNode: AstNode,
        val execute: suspend CallBuiltInFunctionScope.() -> DnclObject
    ) : DnclObject {
        override fun toString() = "<組込関数 ${identifier.name}>"

        override fun hash() = identifier.hashCode()
    }

    data class Null(override val astNode: AstNode) : DnclObject {
        override fun toString() = "null"

        override fun hash() = 0
    }

    data class Nothing(override val astNode: AstNode) : DnclObject {
        override fun toString() = "Nothing"
        override fun hash() = -1
    }

    data class ReturnValue(val value: DnclObject, override val astNode: AstNode) : DnclObject {
        override fun toString() = value.toString()

        override fun hash() = value.hashCode()

    }

    sealed class Error(
        final override val message: kotlin.String,
        node: AstNode,
    ) : DnclObject, DnclError {

        final override val astNode: AstNode = node
        final override val errorRange: IntRange = node.range
        final override val filePath: kotlin.String? = node.filePath

        override fun toString() = message
        override fun hash() = message.hashCode()

        final override fun explain(program: kotlin.String): kotlin.String {
            return explainError(program, message, errorRange, filePath)
        }
    }


    class RuntimeError(message: kotlin.String, astNode: AstNode) :
        Error(message, astNode)

    class ArgumentSizeError(
        message: kotlin.String,
        astNode: AstNode
    ) :
        Error(message, astNode)

    class TypeError(message: kotlin.String, astNode: AstNode) :
        Error(message, astNode)

    class UndefinedError(message: kotlin.String, astNode: AstNode) :
        Error(message, astNode)

    class CannotAssignNothingError(
        message: kotlin.String,
        astNode: AstNode
    ) : Error(message, astNode)

    class IndexOutOfRangeError(
        index: kotlin.Int,
        length: kotlin.Int,
        astNode: AstNode
    ) : Error(
        "配列の範囲外アクセスがされました。\n配列の長さ:${length} \nインデックス:$index",
        astNode
    )
}
