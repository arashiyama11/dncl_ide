package io.github.arashiyama11.dncl_ide.ui.model

import androidx.compose.runtime.Immutable
import kotlinx.collections.immutable.ImmutableList
import kotlinx.collections.immutable.toImmutableList
import io.github.arashiyama11.dncl_ide.domain.notebook.Notebook
import io.github.arashiyama11.dncl_ide.domain.notebook.Cell
import io.github.arashiyama11.dncl_ide.domain.notebook.CellType
import io.github.arashiyama11.dncl_ide.domain.notebook.Output
import io.github.arashiyama11.dncl_ide.domain.notebook.Metadata
import io.github.arashiyama11.dncl_ide.domain.notebook.KernelSpec

@Immutable
data class NotebookUiModel(
    val metadata: MetadataUiModel,
    val cells: ImmutableList<CellUiModel>
)

@Immutable
data class MetadataUiModel(
    val dnclVersion: String,
    val kernelspec: KernelSpecUiModel? = null
)

@Immutable
data class KernelSpecUiModel(
    val name: String,
    val language: String,
    val version: String? = null
)

@Immutable
data class CellUiModel(
    val id: String,
    val type: CellType,
    val source: ImmutableList<String>,
    val executionCount: Int? = null,
    val outputs: ImmutableList<OutputUiModel>? = null
)

@Immutable
data class OutputUiModel(
    val outputType: String,
    val name: String? = null,
    val text: ImmutableList<String>? = null,
    val ename: String? = null,
    val evalue: String? = null,
    val traceback: ImmutableList<String>? = null
)

// Extension functions to convert from domain models to UI models
fun Notebook.toUiModel(): NotebookUiModel = NotebookUiModel(
    metadata = metadata.toUiModel(),
    cells = cells.map { it.toUiModel() }.toImmutableList()
)

fun Metadata.toUiModel(): MetadataUiModel = MetadataUiModel(
    dnclVersion = dnclVersion,
    kernelspec = kernelspec?.toUiModel()
)

fun KernelSpec.toUiModel(): KernelSpecUiModel = KernelSpecUiModel(
    name = name,
    language = language,
    version = version
)

fun Cell.toUiModel(): CellUiModel = CellUiModel(
    id = id,
    type = type,
    source = source.toImmutableList(),
    executionCount = executionCount,
    outputs = outputs?.map { it.toUiModel() }?.toImmutableList()
)

fun Output.toUiModel(): OutputUiModel = OutputUiModel(
    outputType = outputType,
    name = name,
    text = text?.toImmutableList(),
    ename = ename,
    evalue = evalue,
    traceback = traceback?.toImmutableList()
)

// Extension functions to convert from UI models back to domain models
fun NotebookUiModel.toDomainModel(): Notebook = Notebook(
    metadata = metadata.toDomainModel(),
    cells = cells.map { it.toDomainModel() }
)

fun MetadataUiModel.toDomainModel(): Metadata = Metadata(
    dnclVersion = dnclVersion,
    kernelspec = kernelspec?.toDomainModel()
)

fun KernelSpecUiModel.toDomainModel(): KernelSpec = KernelSpec(
    name = name,
    language = language,
    version = version
)

fun CellUiModel.toDomainModel(): Cell = Cell(
    id = id,
    type = type,
    source = source.toList(),
    executionCount = executionCount,
    outputs = outputs?.map { it.toDomainModel() }
)

fun OutputUiModel.toDomainModel(): Output = Output(
    outputType = outputType,
    name = name,
    text = text?.toList(),
    ename = ename,
    evalue = evalue,
    traceback = traceback?.toList()
)
