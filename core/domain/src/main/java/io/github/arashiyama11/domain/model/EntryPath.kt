package io.github.arashiyama11.domain.model

data class EntryPath(
    val value: List<EntryName>,
) {
    override fun toString(): String {
        return value.joinToString("/") { it.value }
    }

    operator fun plus(entryName: EntryName): EntryPath = EntryPath(value + entryName)
}