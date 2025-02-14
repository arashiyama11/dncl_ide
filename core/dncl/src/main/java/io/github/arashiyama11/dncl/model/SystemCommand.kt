package io.github.arashiyama11.dncl.model

sealed interface SystemCommand {
    val command: String

    data object Input : SystemCommand {
        override val command: String = "外部からの入力"
    }

    data class Unknown(override val command: String) : SystemCommand

    companion object {
        fun from(command: String): SystemCommand = when (command) {
            "外部からの入力" -> Input
            else -> Unknown(command)
        }
    }
}