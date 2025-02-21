package io.github.arashiyama11.data.repository

import android.content.Context
import android.util.Log
import io.github.arashiyama11.data.FileDao
import io.github.arashiyama11.data.entity.FileEntity
import io.github.arashiyama11.data.entity.SelectedFileEntity
import io.github.arashiyama11.model.CursorPosition
import io.github.arashiyama11.model.FileContent
import io.github.arashiyama11.model.FileName
import io.github.arashiyama11.model.ProgramFile
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.koin.core.annotation.Single
import java.io.File

@Single(binds = [IFileRepository::class])
class FileRepository(
    context: Context, private val fileDao:
    FileDao
) : IFileRepository {
    override val selectedFileName: MutableStateFlow<FileName?> = MutableStateFlow(null)
    private val programFilesDir: File = File(context.filesDir, PROGRAM_FILES_DIR).apply {
        if (!exists()) {
            mkdirs()
        }
    }

    init {
        if (programFilesDir.listFiles().isNullOrEmpty()) {
            CoroutineScope(Dispatchers.IO).launch {
                selectedFileName.value = FileName(DEFAULT_FILE_NAME)
                saveFile(
                    ProgramFile(
                        FileName(DEFAULT_FILE_NAME),
                        FileContent(""),
                        CursorPosition(0)
                    )
                )
            }
        }

        CoroutineScope(Dispatchers.IO).launch {
            fileDao.getSelectedFile()?.let {
                selectedFileName.value =
                    FileName(fileDao.getFileById(it.fileId)?.name ?: DEFAULT_FILE_NAME)
            }
        }
    }


    override suspend fun getAllFileNames(): List<FileName>? {
        return withContext(Dispatchers.IO) {
            programFilesDir.listFiles()?.map {
                FileName(it.name)
            }
        }
    }

    override suspend fun getFileByName(fileName: FileName): ProgramFile? {
        return withContext(Dispatchers.IO) {
            val file = File(programFilesDir, fileName.value)
            if (!file.exists()) {
                return@withContext null
            }
            fileDao.getFileByName(fileName.value)?.let {
                ProgramFile(
                    fileName,
                    FileContent(file.readText()),
                    CursorPosition(it.cursorPosition)
                )
            }
        }
    }

    override suspend fun saveFile(programFile: ProgramFile) {
        withContext(Dispatchers.IO) {
            fileDao.getFileByName(programFile.name.value)?.let {
                fileDao.insertFile(
                    it.copy(cursorPosition = programFile.cursorPosition.value)
                )
            } ?: fileDao.insertFile(
                FileEntity(
                    name = programFile.name.value,
                    cursorPosition = programFile.cursorPosition.value
                )
            )

            fileDao.getFileByName(programFile.name.value)?.let {
                fileDao.insertSelectedFile(SelectedFileEntity(it.id))
            }


            val file = File(programFilesDir, programFile.name.value)
            file.writeText(programFile.content.value)
        }
    }

    override suspend fun selectFile(fileName: FileName) {
        selectedFileName.value = fileName
        withContext(Dispatchers.IO) {
            fileDao.getFileByName(fileName.value)?.let {
                fileDao.insertSelectedFile(SelectedFileEntity(it.id))
            } ?: Log.e("FileRepository", "selectFile: file not found")
        }
    }

    companion object {
        const val PROGRAM_FILES_DIR = "programs"
        const val DEFAULT_FILE_NAME = "index"
    }
}