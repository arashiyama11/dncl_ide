package io.github.arashiyama11.dncl_ide.domain.model


sealed interface Entry {
    val name: EntryName
    val path: EntryPath
}

data class Folder(
    override val name: FolderName,
    override val path: EntryPath,
    val entities: List<Entry>,
) : Entry

data class ProgramFile(
    override val name: FileName,
    override val path: EntryPath,
) : Entry