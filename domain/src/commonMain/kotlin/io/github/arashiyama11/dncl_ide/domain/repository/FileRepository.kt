package io.github.arashiyama11.dncl_ide.domain.repository

import io.github.arashiyama11.dncl_ide.domain.model.CursorPosition
import io.github.arashiyama11.dncl_ide.domain.model.Entry
import io.github.arashiyama11.dncl_ide.domain.model.EntryPath
import io.github.arashiyama11.dncl_ide.domain.model.FileContent
import io.github.arashiyama11.dncl_ide.domain.model.FileName
import io.github.arashiyama11.dncl_ide.domain.model.Folder
import io.github.arashiyama11.dncl_ide.domain.model.FolderName
import io.github.arashiyama11.dncl_ide.domain.model.NotebookFile
import io.github.arashiyama11.dncl_ide.domain.model.ProgramFile
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.StateFlow

interface FileRepository {
    val selectedEntryPath: StateFlow<EntryPath?>
    val rootPath: EntryPath

    val rootFolder: StateFlow<Folder?>
    suspend fun getRootFolder(): Folder
    suspend fun getEntryByPath(entryPath: EntryPath): Entry?
    fun saveFile(
        programFile: ProgramFile,
        fileContent: FileContent,
        cursorPosition: CursorPosition
    ): Job

    fun saveFile(
        entryPath: EntryPath,
        fileContent: FileContent,
        cursorPosition: CursorPosition
    ): Job

    suspend fun getNotebookFileContent(notebookFile: NotebookFile): FileContent


    suspend fun createFolder(path: EntryPath)
    suspend fun deleteEntry(path: EntryPath)

    suspend fun selectFile(entryPath: EntryPath)
    suspend fun getFileContent(programFile: ProgramFile): FileContent
    suspend fun getCursorPosition(programFile: ProgramFile): CursorPosition
}

suspend fun FileRepository.resolveLib(path: String): String {
    val normalized = path.removePrefix("./")
    val entryPath = if (normalized.startsWith("/")) {
        EntryPath.fromString(normalized)
    } else {
        val parts = normalized.split('/')
        val folderParts = parts.dropLast(1).filter { it.isNotBlank() }.map { FolderName(it) }
        val fileName = FileName(parts.last())
        this.rootPath + EntryPath(folderParts + fileName)
    }

    val entry = this.getEntryByPath(entryPath)
        ?: error("ファイル:$path が見つかりません")

    require(entry is ProgramFile) { "ファイル:$path はプログラムファイルではありません" }
    return this.getFileContent(entry).value
}
