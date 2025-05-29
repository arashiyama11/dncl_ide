package io.github.arashiyama11.dncl_ide.domain.notebook

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class Notebook(
    val metadata: Metadata,
    val cells: List<Cell>
)

@Serializable
data class Metadata(
    @SerialName("dncl_version")
    val dnclVersion: String,
    val kernelspec: KernelSpec? = null
)

@Serializable
data class KernelSpec(
    val name: String,
    val language: String,
    val version: String? = null
)

@Serializable
data class Cell(
    val id: String,
    val type: CellType,
    val source: List<String>,
    @SerialName("execution_count")
    val executionCount: Int? = null,
    val outputs: List<Output>? = null
)

@Serializable
enum class CellType {
    @SerialName("code")
    CODE,
    @SerialName("markdown")
    MARKDOWN
}

@Serializable
data class Output(
    @SerialName("output_type")
    val outputType: String,
    val name: String? = null,
    val text: List<String>? = null,
    val ename: String? = null,
    val evalue: String? = null,
    val traceback: List<String>? = null
)
