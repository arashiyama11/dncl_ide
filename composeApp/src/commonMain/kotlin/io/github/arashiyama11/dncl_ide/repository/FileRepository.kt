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
import io.github.arashiyama11.dncl_ide.domain.repository.IFileRepository
import io.github.arashiyama11.dncl_ide.util.IRootPathProvider
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.IO
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import kotlinx.io.buffered
import kotlinx.io.files.Path
import kotlinx.io.files.SystemFileSystem
import kotlinx.io.readString
import kotlinx.io.writeString

class FileRepository(iRootPathProvider: IRootPathProvider) : IFileRepository {
    override val selectedEntryPath: StateFlow<EntryPath?> = MutableStateFlow(null)

    private val rootPath: EntryPath = iRootPathProvider()

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
                (selectedEntryPath as MutableStateFlow).value =
                    EntryPath.fromString(selectedEntryPathString)
            }


            println("Root folder:")
            println(getRootFolder())
        }
    }

    override suspend fun getRootFolder(): Folder {
        return getEntryByPath(rootPath) as Folder
    }

    override suspend fun getEntryByPath(entryPath: EntryPath): Entry? {
        if (!SystemFileSystem.exists(entryPath.toPath())) return null
        return when (SystemFileSystem.metadataOrNull(entryPath.toPath())?.isDirectory) {
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
    ) {
        SystemFileSystem.sink(programFile.path.toPath()).buffered().use {
            it.writeString(fileContent.value)
        }
    }

    override suspend fun createFolder(path: EntryPath) {
        SystemFileSystem.createDirectories(path.toPath())
    }

    override suspend fun selectFile(entryPath: EntryPath) {
        (selectedEntryPath as MutableStateFlow).value = entryPath
        setting[SELECTED_ENTRY_PATH] = entryPath.toString()
    }

    override suspend fun getFileContent(programFile: ProgramFile): FileContent {
        return FileContent(
            SystemFileSystem.source(programFile.path.toPath()).buffered().readString()
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