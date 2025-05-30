package io.github.arashiyama11.dncl_ide.domain.usecase

import io.github.arashiyama11.dncl_ide.domain.model.CursorPosition
import io.github.arashiyama11.dncl_ide.domain.model.Entry
import io.github.arashiyama11.dncl_ide.domain.model.FileContent
import io.github.arashiyama11.dncl_ide.domain.model.EntryPath
import io.github.arashiyama11.dncl_ide.domain.model.FileName
import io.github.arashiyama11.dncl_ide.domain.model.Folder
import io.github.arashiyama11.dncl_ide.domain.model.FolderName
import io.github.arashiyama11.dncl_ide.domain.model.ProgramFile
import io.github.arashiyama11.dncl_ide.domain.repository.FileRepository

class FileUseCase(private val fileRepository: FileRepository) {
    val selectedEntryPath = fileRepository.selectedEntryPath
    suspend fun getEntryByPath(entryPath: EntryPath): Entry? {
        return fileRepository.getEntryByPath(entryPath)
    }

    suspend fun saveFile(
        programFile: ProgramFile,
        fileContent: FileContent,
        cursorPosition: CursorPosition
    ) =
        fileRepository.saveFile(programFile, fileContent, cursorPosition)

    suspend fun selectFile(entryPath: EntryPath) {
        fileRepository.selectFile(entryPath)
    }

    suspend fun createFile(
        parentPath: EntryPath,
        fileName: FileName
    ) {
        fileRepository.saveFile(
            ProgramFile(
                fileName,
                parentPath + fileName,
            ), FileContent(""), CursorPosition(0)
        )
    }

    suspend fun createFolder(
        parentPath: EntryPath,
        folderName: FolderName
    ) {
        fileRepository.createFolder(parentPath + folderName)
    }

    suspend fun getRootFolder(): Folder {
        return fileRepository.getRootFolder()
    }

    suspend fun getCursorPosition(programFile: ProgramFile): CursorPosition {
        return fileRepository.getCursorPosition(programFile)
    }

    suspend fun getFileContent(programFile: ProgramFile): FileContent {
        return fileRepository.getFileContent(programFile)
    }
}