package io.github.arashiyama11.dncl_ide.repository

import com.russhwolf.settings.Settings
import com.russhwolf.settings.set
import io.github.arashiyama11.dncl_ide.common.AppScope
import io.github.arashiyama11.dncl_ide.domain.model.CursorPosition
import io.github.arashiyama11.dncl_ide.domain.model.Entry
import io.github.arashiyama11.dncl_ide.domain.model.EntryName
import io.github.arashiyama11.dncl_ide.domain.model.EntryPath
import io.github.arashiyama11.dncl_ide.domain.model.FileContent
import io.github.arashiyama11.dncl_ide.domain.model.FileName
import io.github.arashiyama11.dncl_ide.domain.model.Folder
import io.github.arashiyama11.dncl_ide.domain.model.FolderName
import io.github.arashiyama11.dncl_ide.domain.model.NotebookFile
import io.github.arashiyama11.dncl_ide.domain.model.ProgramFile
import io.github.arashiyama11.dncl_ide.domain.repository.FileRepository
import io.github.arashiyama11.dncl_ide.util.RootPathProvider
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlinx.io.buffered
import kotlinx.io.files.Path
import kotlinx.io.files.SystemFileSystem
import kotlinx.io.readString
import kotlinx.io.writeString
import dncl_ide.composeapp.generated.resources.Res
import io.arashiyama11.dncl_ide.generated.DnclLibs
import io.github.arashiyama11.dncl_ide.util.ioDispatcher

class FileRepositoryImpl(rootPathProvider: RootPathProvider, private val appScope: AppScope) :
    FileRepository {
    private val _selectedEntryPath: MutableStateFlow<EntryPath?> = MutableStateFlow(null)
    override val selectedEntryPath: StateFlow<EntryPath?> = _selectedEntryPath

    override val rootPath: EntryPath = rootPathProvider()

    private val stdlibRootPath: EntryPath = rootPath + FolderName("stdlib")

    private val _rootFolder: MutableStateFlow<Folder?> = MutableStateFlow(null)
    override val rootFolder: StateFlow<Folder?> = _rootFolder.asStateFlow()


    private val setting = Settings()

    init {
        DnclLibs.texts
        updateRootFolder()
        appScope.launch(ioDispatcher) {
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

            rootPath.plus(FileName("getting_start.dnclnb")).let {
                if (getEntryByPath(it) == null) {
                    val content =
                        Res.readBytes("files/getting_start_template.dnclnb").decodeToString()
                    saveFile(
                        it,
                        FileContent(content),
                        CursorPosition(0)
                    )
                }
            }
        }
    }


    private fun updateRootFolder() {
        appScope.launch(ioDispatcher) {
            _rootFolder.value = getRootFolder()
        }
    }

    override suspend fun getRootFolder(): Folder = withContext(ioDispatcher) {
        getEntryByPath(rootPath) as Folder
    }

    override suspend fun getEntryByPath(entryPath: EntryPath): Entry? =
        withContext(ioDispatcher) {
            getStdlibEntry(entryPath)?.let { return@withContext it }

            if (!SystemFileSystem.exists(entryPath.toPath())) return@withContext null
            when (SystemFileSystem.metadataOrNull(entryPath.toPath())?.isDirectory) {
                null -> null
                true -> {
                    val entries = SystemFileSystem.list(entryPath.toPath()).map {
                        getEntryByPath(entryPath + FileName(it.name))
                    }
                    val merged = entries.filterNotNull().toMutableList()
                    if (entryPath == rootPath && merged.none { it.path == stdlibRootPath }) {
                        merged += stdlibRootFolder
                    }
                    Folder(
                        name = FolderName(entryPath.value.last().value),
                        path = entryPath,
                        entities = merged
                    )
                }

                false -> {
                    val name = entryPath.value.last().value
                    if (name.endsWith(".dnclnb")) {
                        NotebookFile(
                            name = FileName(name),
                            path = entryPath,
                        )
                    } else ProgramFile(
                        name = FileName(name),
                        path = entryPath,
                    )
                }
            }
        }

    override fun saveFile(
        programFile: ProgramFile,
        fileContent: FileContent,
        cursorPosition: CursorPosition
    ): Job {
        if (programFile.path.isStdlibPath()) {
            return appScope.launch(ioDispatcher) { println("Skip saving stdlib file: read only") }
        }
        return appScope.launch(ioDispatcher) {
            val tmpPath = programFile.path.copy(
                value = programFile.path.value.let { it.dropLast(1) + FileName(it.last().value + ".tmp") }
            ).toPath()
            SystemFileSystem.sink(tmpPath).buffered().use {
                it.writeString(fileContent.value)
            }

            SystemFileSystem.atomicMove(tmpPath, programFile.path.toPath())
            updateRootFolder()
        }
    }

    override fun saveFile(
        entryPath: EntryPath,
        fileContent: FileContent,
        cursorPosition: CursorPosition
    ): Job {
        if (entryPath.isStdlibPath()) {
            return appScope.launch(ioDispatcher) { println("Skip saving stdlib file: read only") }
        }
        return appScope.launch(ioDispatcher) {
            val tmpPath = entryPath.copy(
                value = entryPath.value.let { it.dropLast(1) + FileName(it.last().value + ".tmp") }
            ).toPath()

            SystemFileSystem.sink(tmpPath).buffered().use {
                it.writeString(fileContent.value)
            }



            runCatching {
                SystemFileSystem.atomicMove(tmpPath, entryPath.toPath())
            }.onFailure { println(it) }

            updateRootFolder()
        }
    }

    override suspend fun getNotebookFileContent(notebookFile: NotebookFile): FileContent {
        return withContext(ioDispatcher) {
            val stdlibKey = notebookFile.path.toStdlibKey()
            if (stdlibKey != null) {
                return@withContext FileContent(DnclLibs.texts[stdlibKey] ?: "")
            }
            FileContent(
                SystemFileSystem.source(notebookFile.path.toPath()).buffered()
                    .use { it.readString() }
            )
        }
    }

    override suspend fun createFolder(path: EntryPath) = withContext(ioDispatcher) {
        if (path.isStdlibPath()) return@withContext
        SystemFileSystem.createDirectories(path.toPath()).also { updateRootFolder() }
    }

    override suspend fun deleteEntry(path: EntryPath) {
        withContext(ioDispatcher) {
            if (path.isStdlibPath()) return@withContext
            SystemFileSystem.delete(path.toPath(), false)
            updateRootFolder()
        }
    }

    override suspend fun selectFile(entryPath: EntryPath) = withContext(ioDispatcher) {
        _selectedEntryPath.value = entryPath
        setting[SELECTED_ENTRY_PATH] = entryPath.toString()
    }

    override suspend fun getFileContent(programFile: ProgramFile): FileContent =
        withContext(ioDispatcher) {
            val stdlibKey = programFile.path.toStdlibKey()
            if (stdlibKey != null) {
                return@withContext FileContent(DnclLibs.texts[stdlibKey] ?: "")
            }
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

    private val stdlibRootFolder: Folder by lazy {
        stdlibRootNode.toEntry(stdlibRootPath) as Folder
    }

    private val stdlibRootNode by lazy { buildStdlibTree() }

    private fun buildStdlibTree(): StdlibNode {
        val rootNode = StdlibNode(FolderName("stdlib"))

        DnclLibs.texts.forEach { (rawPath, content) ->
            val parts = rawPath.split('/').filter { it.isNotBlank() }
            var current = rootNode
            parts.forEachIndexed { index, part ->
                val isLeaf = index == parts.lastIndex
                val entryName: EntryName = if (isLeaf) FileName(part) else FolderName(part)
                val next = current.children.getOrPut(part) { StdlibNode(entryName) }
                if (isLeaf) next.content = content
                current = next
            }
        }

        return rootNode
    }

    private fun getStdlibEntry(entryPath: EntryPath): Entry? {
        if (!entryPath.isStdlibPath()) return null

        val relative = entryPath.value.drop(stdlibRootPath.value.size)
        if (relative.isEmpty()) return stdlibRootFolder

        var current = stdlibRootNode
        relative.forEach { name ->
            current = current.children[name.value] ?: return null
        }

        return current.toEntry(entryPath)
    }

    private fun EntryPath.isStdlibPath(): Boolean {
        if (value.size < stdlibRootPath.value.size) return false
        return value.take(stdlibRootPath.value.size) == stdlibRootPath.value
    }

    private fun EntryPath.toStdlibKey(): String? {
        if (!isStdlibPath()) return null
        val relative = value.drop(stdlibRootPath.value.size)
        if (relative.isEmpty()) return null
        return relative.joinToString("/") { it.value }
    }

    private data class StdlibNode(
        val name: EntryName,
        val children: MutableMap<String, StdlibNode> = mutableMapOf(),
        var content: String? = null
    ) {
        fun toEntry(path: EntryPath): Entry {
            return if (content != null) {
                val fileName = name as FileName
                if (fileName.isNotebookFile()) {
                    NotebookFile(fileName, path)
                } else {
                    ProgramFile(fileName, path)
                }
            } else {
                val folderName = name as FolderName
                val entities = children.values
                    .sortedBy { it.name.value }
                    .map { it.toEntry(path + it.name) }
                Folder(folderName, path, entities)
            }
        }
    }

    companion object {
        const val DEFAULT_PROGRAM_FILE_NAME = "index"

        const val SELECTED_ENTRY_PATH = "selectedEntryPath"
    }
}
