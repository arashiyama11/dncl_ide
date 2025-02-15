package io.github.arashiyama11.dncl.model

sealed interface SystemCommand {
    val command: String
    val astNode: AstNode.SystemLiteral

    data class Input(override val astNode: AstNode.SystemLiteral) : SystemCommand {
        override val command: String = "外部からの入力"
    }

    data class Unknown(override val command: String, override val astNode: AstNode.SystemLiteral) :
        SystemCommand

    companion object {
        fun from(node: AstNode.SystemLiteral): SystemCommand = when (node.value) {
            "外部からの入力" -> Input(node)
            else -> Unknown(node.value, node)
        }
    }
}