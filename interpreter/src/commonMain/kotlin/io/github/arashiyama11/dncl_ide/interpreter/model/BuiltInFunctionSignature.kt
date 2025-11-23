package io.github.arashiyama11.dncl_ide.interpreter.model

data class BuiltInFunctionSignature(
    val name: String,
    val params: List<String>,
    val range: IntRange? = null,
    val filePath: String? = null
)
