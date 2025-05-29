package io.github.arashiyama11.dncl_ide.domain.model

sealed interface EntryName {
    val value: String
    fun isNotebookFile(): Boolean = value.endsWith(".dnclnb")
}

data class FolderName(override val value: String) : EntryName

data class FileName(override val value: String) : EntryName