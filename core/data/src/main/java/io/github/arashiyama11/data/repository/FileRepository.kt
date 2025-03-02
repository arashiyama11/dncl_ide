package io.github.arashiyama11.data.repository

import android.content.Context
import android.util.Log
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import io.github.arashiyama11.data.FileDao
import io.github.arashiyama11.data.entity.FileEntity
import io.github.arashiyama11.model.CursorPosition
import io.github.arashiyama11.model.FileContent
import io.github.arashiyama11.model.FileName
import io.github.arashiyama11.model.ProgramFile
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.launch
import java.io.File

class FileRepository(
    private val context: Context,
    private val fileDao: FileDao
) : IFileRepository {
    override val selectedFileName: MutableStateFlow<FileName?> = MutableStateFlow(null)
    private val programFilesDir: File = File(context.filesDir, PROGRAM_FILES_DIR).apply {
        if (!exists()) {
            mkdirs()
        }
    }

    init {
        CoroutineScope(Dispatchers.IO).launch {
            if (programFilesDir.listFiles().isNullOrEmpty()) {
                selectedFileName.value = FileName(DEFAULT_FILE_NAME)
                saveFile(
                    ProgramFile(
                        FileName(DEFAULT_FILE_NAME),
                        FileContent(""),
                        CursorPosition(0)
                    )
                )
            } else {
                val id =
                    context.dataStore.data.firstOrNull()?.get(PreferencesKeys.SELECTED_FILE_ID) ?: 0
                fileDao.getFileById(id)?.name?.let {
                    selectFile(FileName(it))
                }
            }
        }
    }


    override suspend fun getAllFileNames(): List<FileName>? {
        return programFilesDir.listFiles()?.map {
            FileName(it.name)
        }
    }

    override suspend fun getFileByName(fileName: FileName): ProgramFile? {
        val file = File(programFilesDir, fileName.value)
        if (!file.exists()) {
            return null
        }
        return fileDao.getFileByName(fileName.value)?.let {
            ProgramFile(
                fileName,
                FileContent(file.readText()),
                CursorPosition(it.cursorPosition)
            )
        }
    }

    override suspend fun saveFile(programFile: ProgramFile) {
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

        val file = File(programFilesDir, programFile.name.value)
        file.writeText(programFile.content.value)
    }

    override suspend fun selectFile(fileName: FileName) {
        selectedFileName.value = fileName

        fileDao.getFileByName(fileName.value)?.let {
            context.dataStore.edit { preferences ->
                preferences[PreferencesKeys.SELECTED_FILE_ID] = it.id
            }
        } ?: Log.e("FileRepository", "selectFile: file not found")
    }

    companion object {
        const val PROGRAM_FILES_DIR = "programs"
        const val DEFAULT_FILE_NAME = "index"
        private val Context.dataStore: DataStore<Preferences> by preferencesDataStore(name = "settings")

        object PreferencesKeys {
            val SELECTED_FILE_ID = intPreferencesKey("selected_file_id")
        }
    }
}