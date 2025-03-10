package io.github.arashiyama11.domain.usecase

import android.icu.text.CaseMap
import io.github.arashiyama11.domain.model.CursorPosition
import io.github.arashiyama11.domain.model.Entry
import io.github.arashiyama11.domain.model.EntryPath
import io.github.arashiyama11.domain.model.FileContent
import io.github.arashiyama11.domain.model.FileName
import io.github.arashiyama11.domain.model.Folder
import io.github.arashiyama11.domain.model.FolderName
import io.github.arashiyama11.domain.model.ProgramFile
import kotlinx.coroutines.flow.StateFlow

interface IFileUseCase {
    val selectedEntryPath: StateFlow<EntryPath?>

    suspend fun getRootFolder(): Folder
    suspend fun getEntryByPath(entryPath: EntryPath): Entry?
    suspend fun saveFile(
        programFile: ProgramFile, fileContent: FileContent,
        cursorPosition: CursorPosition
    )

    suspend fun selectFile(entryPath: EntryPath)
    suspend fun createFile(parentPath: EntryPath, fileName: FileName)
    suspend fun createFolder(parentPath: EntryPath, folderName: FolderName)
    suspend fun getFileContent(programFile: ProgramFile): FileContent
    suspend fun getCursorPosition(programFile: ProgramFile): CursorPosition
}