package io.github.arashiyama11.dncl_ide.repository

import com.russhwolf.settings.Settings
import com.russhwolf.settings.set
import io.github.arashiyama11.dncl_ide.domain.model.CursorPosition
import io.github.arashiyama11.dncl_ide.domain.model.Entry
import io.github.arashiyama11.dncl_ide.domain.model.EntryPath
import io.github.arashiyama11.dncl_ide.domain.model.FileContent
import io.github.arashiyama11.dncl_ide.domain.model.FileName
import io.github.arashiyama11.dncl_ide.domain.model.Folder
import io.github.arashiyama11.dncl_ide.domain.model.FolderName
import io.github.arashiyama11.dncl_ide.domain.model.ProgramFile
import io.github.arashiyama11.dncl_ide.domain.repository.FileRepository
import io.github.arashiyama11.dncl_ide.util.RootPathProvider
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.IO
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlinx.io.buffered
import kotlinx.io.files.Path
import kotlinx.io.files.SystemFileSystem
import kotlinx.io.readString
import kotlinx.io.writeString

class FileRepositoryImpl(rootPathProvider: RootPathProvider) : FileRepository {
    private val _selectedEntryPath: MutableStateFlow<EntryPath?> = MutableStateFlow(null)
    override val selectedEntryPath: StateFlow<EntryPath?> = _selectedEntryPath

    private val rootPath: EntryPath = rootPathProvider()

    private val setting = Settings()

    init {
        CoroutineScope(Dispatchers.IO).launch {
            val root = getEntryByPath(rootPath)
            if (root == null) {
                createFolder(rootPath)
            }
            rootPath.plus(FileName(DEFAULT_PROGRAM_FILE_NAME)).let {
                if (getEntryByPath(it) == null) {
                    saveFile(
                        ProgramFile(
                            name = FileName(DEFAULT_PROGRAM_FILE_NAME),
                            path = it
                        ),
                        FileContent(""),
                        CursorPosition(0)
                    )
                }
                val selectedEntryPathString =
                    setting.getString(SELECTED_ENTRY_PATH, it.toString())
                _selectedEntryPath.value =
                    EntryPath.fromString(selectedEntryPathString)
            }


            println("Root folder:")
            println(getRootFolder())
        }
    }

    override suspend fun getRootFolder(): Folder = withContext(Dispatchers.IO) {
        getEntryByPath(rootPath) as Folder
    }

    override suspend fun getEntryByPath(entryPath: EntryPath): Entry? =
        withContext(Dispatchers.IO) {
            if (!SystemFileSystem.exists(entryPath.toPath())) return@withContext null
            when (SystemFileSystem.metadataOrNull(entryPath.toPath())?.isDirectory) {
                null -> null
                true -> {
                    val entries = SystemFileSystem.list(entryPath.toPath()).map {
                        getEntryByPath(entryPath + FileName(it.name))
                    }
                    Folder(
                        name = FolderName(entryPath.value.last().value),
                        path = entryPath,
                        entities = entries.filterNotNull()
                    )
                }

                false -> {
                    ProgramFile(
                        name = FileName(entryPath.value.last().value),
                        path = entryPath,
                    )
                }
            }
        }

    override suspend fun saveFile(
        programFile: ProgramFile,
        fileContent: FileContent,
        cursorPosition: CursorPosition
    ) = withContext(Dispatchers.IO) {
        SystemFileSystem.sink(programFile.path.toPath()).buffered().use {
            it.writeString(fileContent.value)
        }
    }

    override suspend fun createFolder(path: EntryPath) = withContext(Dispatchers.IO) {
        SystemFileSystem.createDirectories(path.toPath())
    }

    override suspend fun selectFile(entryPath: EntryPath) = withContext(Dispatchers.IO) {
        _selectedEntryPath.value = entryPath
        setting[SELECTED_ENTRY_PATH] = entryPath.toString()
    }

    override suspend fun getFileContent(programFile: ProgramFile): FileContent =
        withContext(Dispatchers.IO) {
            FileContent(
                SystemFileSystem.source(programFile.path.toPath()).buffered()
                    .use { it.readString() }
            )
        }

    override suspend fun getCursorPosition(programFile: ProgramFile): CursorPosition {
        return CursorPosition(0)
    }

    private fun EntryPath.toPath(): Path {
        return Path(toString())
    }

    companion object {
        const val DEFAULT_PROGRAM_FILE_NAME = "index"

        const val SELECTED_ENTRY_PATH = "selectedEntryPath"
    }
}

