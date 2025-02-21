package io.github.arashiyama11.data.repository

import io.github.arashiyama11.model.FileName
import io.github.arashiyama11.model.ProgramFile
import kotlinx.coroutines.flow.StateFlow

interface IFileRepository {
    val selectedFileName: StateFlow<FileName?>
    suspend fun getAllFileNames(): List<FileName>?
    suspend fun getFileByName(fileName: FileName): ProgramFile?
    suspend fun saveFile(programFile: ProgramFile)
    suspend fun selectFile(fileName: FileName)
}