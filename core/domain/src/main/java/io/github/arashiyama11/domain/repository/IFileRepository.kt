package io.github.arashiyama11.domain.repository

import io.github.arashiyama11.domain.model.CursorPosition
import io.github.arashiyama11.domain.model.Entry
import io.github.arashiyama11.domain.model.EntryPath
import io.github.arashiyama11.domain.model.FileContent
import io.github.arashiyama11.domain.model.Folder
import io.github.arashiyama11.domain.model.ProgramFile
import kotlinx.coroutines.flow.StateFlow

interface IFileRepository {
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