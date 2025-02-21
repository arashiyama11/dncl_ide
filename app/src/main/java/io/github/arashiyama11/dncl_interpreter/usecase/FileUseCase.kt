package io.github.arashiyama11.dncl_interpreter.usecase

import io.github.arashiyama11.model.CursorPosition
import io.github.arashiyama11.model.FileContent
import io.github.arashiyama11.model.FileName
import io.github.arashiyama11.model.ProgramFile
import io.github.arashiyama11.data.repository.IFileRepository
import org.koin.core.annotation.Single

@Single(binds = [IFileUseCase::class])
class FileUseCase(private val fileRepository: IFileRepository) :
    IFileUseCase {
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