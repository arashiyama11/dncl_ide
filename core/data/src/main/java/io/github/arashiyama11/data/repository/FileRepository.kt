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
import io.github.arashiyama11.domain.model.CursorPosition
import io.github.arashiyama11.domain.model.Entry
import io.github.arashiyama11.domain.model.FileContent
import io.github.arashiyama11.domain.model.EntryPath
import io.github.arashiyama11.domain.model.FileName
import io.github.arashiyama11.domain.model.Folder
import io.github.arashiyama11.domain.model.FolderName
import io.github.arashiyama11.domain.model.ProgramFile
import io.github.arashiyama11.domain.repository.IFileRepository
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.launch
import org.koin.core.annotation.Single
import java.io.File

@Single(binds = [IFileRepository::class])
class FileRepository(
    private val context: Context,
    private val fileDao: FileDao
) : IFileRepository {
    override val selectedEntryPath: MutableStateFlow<EntryPath?> = MutableStateFlow(null)
    private val programFilesDir: File = File(context.filesDir, PROGRAM_FILES_DIR).apply {
        if (!exists()) {
            mkdirs()
        }
    }

    private val rootPath = EntryPath(
        listOf(
            FolderName(PROGRAM_FILES_DIR)
        )
    )

    init {
        CoroutineScope(Dispatchers.IO).launch {
            if (programFilesDir.listFiles().isNullOrEmpty()) {
                selectedEntryPath.value = rootPath + FileName(DEFAULT_FILE_NAME)
                saveFile(
                    ProgramFile(
                        FileName(DEFAULT_FILE_NAME),
                        selectedEntryPath.value!!,
                    ), FileContent(""),
                    CursorPosition(0)
                )
            } else {
                val id =
                    context.dataStore.data.firstOrNull()?.get(PreferencesKeys.SELECTED_FILE_ID) ?: 0
                fileDao.getFileById(id)?.let {
                    val p = it.path.split("/")
                    selectedEntryPath.value = EntryPath(
                        p.dropLast(1).map { FolderName(it) } + FileName(p.last())
                    )
                }
            }
        }
    }

    override suspend fun getEntryByPath(entryPath: EntryPath): Entry? {
        val folder = entryPath.toFile()
        return if (folder.exists()) {
            if (folder.isDirectory) {
                getFolderByPath(entryPath)
            } else {
                getFileByPath(entryPath)
            }
        } else {
            null
        }
    }

    suspend fun getFolderByPath(entryPath: EntryPath): Folder {
        Log.d("FileRepository", "getFolderByPath: $entryPath")
        return Folder(
            FolderName(entryPath.value.last().value),
            entryPath,
            entryPath.toFile().listFiles()?.mapNotNull {
                val path =
                    entryPath + if (it.isDirectory) FolderName(it.name) else FileName(it.name)
                getEntryByPath(path)
            }.orEmpty()
        )
    }

    fun getFileByPath(entryPath: EntryPath): ProgramFile {
        Log.d("FileRepository", "getFileByPath: $entryPath")
        return ProgramFile(
            FileName(entryPath.value.last().value),
            entryPath,
        )
    }

    override suspend fun getRootFolder(): Folder {
        return getFolderByPath(rootPath)
    }

    override suspend fun saveFile(
        programFile: ProgramFile,
        fileContent: FileContent,
        cursorPosition: CursorPosition
    ) {
        fileDao.getFileByPath(programFile.path.toString())?.let {
            fileDao.insertFile(
                it.copy(cursorPosition = cursorPosition.value)
            )
        } ?: fileDao.insertFile(
            FileEntity(
                path = programFile.path.toString(),
                cursorPosition = cursorPosition.value
            )
        )

        programFile.path.toFile().apply {
            if (!exists()) {
                createNewFile()
            }
        }.writeText(fileContent.value)
    }

    override suspend fun createFolder(path: EntryPath) {
        path.toFile().mkdirs()
    }

    override suspend fun selectFile(entryPath: EntryPath) {
        selectedEntryPath.value = entryPath

        fileDao.getFileByPath(entryPath.toString())?.let {
            context.dataStore.edit { preferences ->
                preferences[PreferencesKeys.SELECTED_FILE_ID] = it.id
            }
        } ?: Log.e("FileRepository", "selectFile: file not found")
    }

    override suspend fun getFileContent(programFile: ProgramFile): FileContent {
        return FileContent(programFile.path.toFile().readText())
    }

    override suspend fun getCursorPosition(programFile: ProgramFile): CursorPosition {
        return CursorPosition(
            fileDao.getFileByPath(programFile.path.toString())?.cursorPosition ?: 0
        )
    }

    private fun EntryPath.toFile(): File {
        return context.filesDir.resolve(this.toString())
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