package io.github.arashiyama11.domain.usecase

import io.github.arashiyama11.domain.model.CursorPosition
import io.github.arashiyama11.domain.model.Entry
import io.github.arashiyama11.domain.model.FileContent
import io.github.arashiyama11.domain.model.EntryPath
import io.github.arashiyama11.domain.model.FileName
import io.github.arashiyama11.domain.model.Folder
import io.github.arashiyama11.domain.model.FolderName
import io.github.arashiyama11.domain.model.ProgramFile
import io.github.arashiyama11.domain.repository.IFileRepository
import org.koin.core.annotation.Single

@Single
class FileUseCase(private val fileRepository: IFileRepository) {
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