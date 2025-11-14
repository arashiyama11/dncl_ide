package io.github.arashiyama11.dncl_ide.domain.notebook

import androidx.compose.runtime.Immutable
import dncl_ide.domain.BuildConfig
import kotlinx.collections.immutable.ImmutableList
import kotlinx.collections.immutable.toImmutableList
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Immutable
data class Notebook(
    val metadata: Metadata,
    val cells: ImmutableList<Cell>
)

@Immutable
data class Metadata(
    val dnclVersion: String = BuildConfig.DNCL_VERSION,
    val kernelspec: KernelSpec? = null
)

@Immutable
data class KernelSpec(
    val name: String,
    val language: String,
    val version: String? = null
)

@Immutable
data class Cell(
    val id: String,
    val type: CellType,
    val source: ImmutableList<String>,
    val executionCount: Int? = null,
    val outputs: ImmutableList<Output>? = null
)

@Immutable

enum class CellType {
    CODE,
    MARKDOWN;

    fun toSerializable(): SerializableCellType = when (this) {
        CODE -> SerializableCellType.CODE
        MARKDOWN -> SerializableCellType.MARKDOWN
    }
}

@Immutable
data class Output(
    val outputType: String,
    val name: String? = null,
    val text: ImmutableList<String>? = null,
    val ename: String? = null,
    val evalue: String? = null,
    val traceback: ImmutableList<String>? = null
)


// --- Serializable Models (For JSON conversion) ---
@Serializable
data class SerializableNotebook(
    val metadata: SerializableMetadata,
    val cells: List<SerializableCell>
)

@Serializable
data class SerializableMetadata(
    @SerialName("dncl_version")
    val dnclVersion: String,
    val kernelspec: SerializableKernelSpec? = null
)

@Serializable
data class SerializableKernelSpec(
    val name: String,
    val language: String,
    val version: String? = null
)

@Serializable
data class SerializableCell(
    val id: String,
    val type: SerializableCellType,
    val source: List<String>,
    @SerialName("execution_count")
    val executionCount: Int? = null,
    val outputs: List<SerializableOutput>? = null
)

@Serializable
enum class SerializableCellType {
    @SerialName("code")
    CODE,

    @SerialName("markdown")
    MARKDOWN;

    fun toDomain(): CellType = when (this) {
        CODE -> CellType.CODE
        MARKDOWN -> CellType.MARKDOWN
    }
}

@Serializable
data class SerializableOutput(
    @SerialName("output_type")
    val outputType: String,
    val name: String? = null,
    val text: List<String>? = null,
    val ename: String? = null,
    val evalue: String? = null,
    val traceback: List<String>? = null
)


// --- Conversion Functions ---

fun SerializableNotebook.toDomain(): Notebook = Notebook(
    metadata = metadata.toDomain(),
    cells = cells.map { it.toDomain() }.toImmutableList()
)

fun Notebook.toSerializable(): SerializableNotebook = SerializableNotebook(
    metadata = metadata.toSerializable(),
    cells = cells.map { it.toSerializable() }
)

fun SerializableMetadata.toDomain(): Metadata = Metadata(
    dnclVersion = dnclVersion,
    kernelspec = kernelspec?.toDomain()
)

fun Metadata.toSerializable(): SerializableMetadata = SerializableMetadata(
    dnclVersion = dnclVersion,
    kernelspec = kernelspec?.toSerializable()
)

fun SerializableKernelSpec.toDomain(): KernelSpec = KernelSpec(
    name = name,
    language = language,
    version = version
)

fun KernelSpec.toSerializable(): SerializableKernelSpec = SerializableKernelSpec(
    name = name,
    language = language,
    version = version
)

fun SerializableCell.toDomain(): Cell = Cell(
    id = id,
    type = type.toDomain(),
    source = source.toImmutableList(),
    executionCount = executionCount,
    outputs = outputs?.map { it.toDomain() }?.toImmutableList()
)

fun Cell.toSerializable(): SerializableCell = SerializableCell(
    id = id,
    type = type.toSerializable(),
    source = source.toList(),
    executionCount = executionCount,
    outputs = outputs?.map { it.toSerializable() }
)

fun SerializableOutput.toDomain(): Output = Output(
    outputType = outputType,
    name = name,
    text = text?.toImmutableList(),
    ename = ename,
    evalue = evalue,
    traceback = traceback?.toImmutableList()
)

fun Output.toSerializable(): SerializableOutput = SerializableOutput(
    outputType = outputType,
    name = name,
    text = text?.toList(),
    ename = ename,
    evalue = evalue,
    traceback = traceback?.toList()
)
