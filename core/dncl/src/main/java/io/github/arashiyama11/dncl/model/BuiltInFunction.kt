package io.github.arashiyama11.dncl.model

enum class BuiltInFunction(private val identifier: String) {
    PRINT("表示する"), LENGTH("要素数"), DIFF("差分");

    companion object {
        fun from(identifier: String): BuiltInFunction? =
            entries.find { it.identifier == identifier }
    }
}