package io.github.arashiyama11.domain.usecase

import io.github.arashiyama11.domain.model.CursorPosition
import io.github.arashiyama11.domain.model.FileContent
import io.github.arashiyama11.domain.model.FileName
import io.github.arashiyama11.domain.model.ProgramFile
import io.github.arashiyama11.domain.repository.IFileRepository
import org.koin.core.annotation.Single

@Single(binds = [IFileUseCase::class])
internal class FileUseCase(private val fileRepository: IFileRepository) : IFileUseCase {
    override val selectedFileName = fileRepository.selectedFileName

    override suspend fun getAllFileNames() = fileRepository.getAllFileNames()

    override suspend fun getFileByName(fileName: FileName) =
        fileRepository.getFileByName(fileName)

    override suspend fun saveFile(programFile: ProgramFile) =
        fileRepository.saveFile(programFile)

    override suspend fun selectFile(fileName: FileName) {
        fileRepository.selectFile(fileName)
    }

    override suspend fun createFile(fileName: FileName) {
        fileRepository.saveFile(
            ProgramFile(
                fileName,
                FileContent(""),
                CursorPosition(0)
            )
        )
    }
}