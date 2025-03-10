package io.github.arashiyama11.domain.model

sealed interface EntryName {
    val value: String
}

data class FolderName(override val value: String) : EntryName

data class FileName(override val value: String) : EntryName