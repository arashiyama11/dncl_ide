package io.github.arashiyama11.dncl_interpreter.usecase

import io.github.arashiyama11.model.FileName
import io.github.arashiyama11.model.ProgramFile
import kotlinx.coroutines.flow.StateFlow

interface IFileUseCase {
    val selectedFileName: StateFlow<io.github.arashiyama11.model.FileName?>
    suspend fun getAllFileNames(): List<io.github.arashiyama11.model.FileName>?
    suspend fun getFileByName(fileName: io.github.arashiyama11.model.FileName): io.github.arashiyama11.model.ProgramFile?
    suspend fun saveFile(programFile: io.github.arashiyama11.model.ProgramFile)
    suspend fun selectFile(fileName: io.github.arashiyama11.model.FileName)
    suspend fun createFile(fileName: io.github.arashiyama11.model.FileName)
}