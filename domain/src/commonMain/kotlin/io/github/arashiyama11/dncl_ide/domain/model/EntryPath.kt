package io.github.arashiyama11.dncl_ide.domain.model

data class EntryPath(
    val value: List<EntryName>,
) {

    val isAbsolute: Boolean
        get() = value.firstOrNull()?.value?.isBlank() == true

    override fun toString(): String {
        return value.joinToString("/") { it.value }
    }

    fun isNotebookFile(): Boolean {
        return value.lastOrNull()?.isNotebookFile() ?: false
    }

    fun parent(): EntryPath? {
        return if (value.size > 1) {
            EntryPath(value.dropLast(1))
        } else {
            null
        }
    }

    operator fun plus(entryName: EntryName): EntryPath = EntryPath(value + entryName)

    operator fun plus(entryPath: EntryPath): EntryPath = EntryPath(value + entryPath.value)

    override operator fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other is EntryPath) {
            return value == other.value
        }

        return false
    }

    companion object {
        fun fromString(path: String): EntryPath {
            return EntryPath(path.split("/").map { FolderName(it) })
        }
    }

    override fun hashCode(): Int {
        var result = value.hashCode()
        result = 31 * result + isAbsolute.hashCode()
        return result
    }
}