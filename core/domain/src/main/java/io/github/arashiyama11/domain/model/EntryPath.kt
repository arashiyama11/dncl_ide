package io.github.arashiyama11.domain.model

data class EntryPath(
    val value: List<EntryName>,
) {
    override fun toString(): String {
        return value.joinToString("/") { it.value }
    }

    fun parent(): EntryPath? {
        return if (value.size > 1) {
            EntryPath(value.dropLast(1))
        } else {
            null
        }
    }

    operator fun plus(entryName: EntryName): EntryPath = EntryPath(value + entryName)
}