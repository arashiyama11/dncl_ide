package io.github.arashiyama11.dncl_ide.domain.repository

import io.github.arashiyama11.dncl_ide.domain.model.CursorPosition
import io.github.arashiyama11.dncl_ide.domain.model.Entry
import io.github.arashiyama11.dncl_ide.domain.model.EntryPath
import io.github.arashiyama11.dncl_ide.domain.model.FileContent
import io.github.arashiyama11.dncl_ide.domain.model.Folder
import io.github.arashiyama11.dncl_ide.domain.model.ProgramFile
import kotlinx.coroutines.flow.StateFlow

interface FileRepository {
    val selectedEntryPath: StateFlow<EntryPath?>
    suspend fun getRootFolder(): Folder
    suspend fun getEntryByPath(entryPath: EntryPath): Entry?
    suspend fun saveFile(
        programFile: ProgramFile,
        fileContent: FileContent,
        cursorPosition: CursorPosition
    )

    suspend fun createFolder(path: EntryPath)

    suspend fun selectFile(entryPath: EntryPath)
    suspend fun getFileContent(programFile: ProgramFile): FileContent
    suspend fun getCursorPosition(programFile: ProgramFile): CursorPosition
}