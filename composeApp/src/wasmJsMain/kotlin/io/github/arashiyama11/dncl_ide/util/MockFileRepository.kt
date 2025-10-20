package io.github.arashiyama11.dncl_ide.util

import io.github.arashiyama11.dncl_ide.domain.repository.FileRepository
import io.github.arashiyama11.dncl_ide.domain.model.CursorPosition
import io.github.arashiyama11.dncl_ide.domain.model.Entry
import io.github.arashiyama11.dncl_ide.domain.model.EntryPath
import io.github.arashiyama11.dncl_ide.domain.model.FileContent
import io.github.arashiyama11.dncl_ide.domain.model.FileName
import io.github.arashiyama11.dncl_ide.domain.model.Folder
import io.github.arashiyama11.dncl_ide.domain.model.FolderName
import io.github.arashiyama11.dncl_ide.domain.model.NotebookFile
import io.github.arashiyama11.dncl_ide.domain.model.ProgramFile
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow

class MockFileRepository : FileRepository {
    override val rootPath: EntryPath = EntryPath.fromString("/")

    val filename = FileName("main.dncl")
    val path = rootPath + filename


    override val selectedEntryPath: StateFlow<EntryPath?> = MutableStateFlow(path)


    val file: ProgramFile = ProgramFile(
        name = filename,
        path = path
    )

    var fileContent: FileContent = FileContent(
        """
        表示する("Hello DNCL!")
    """.trimIndent()
    )

    override val rootFolder: StateFlow<Folder?> = MutableStateFlow(
        Folder(
            name = FolderName("root"),
            path = rootPath,
            entities = listOf()
        )
    )

    override suspend fun getRootFolder(): Folder {
        return rootFolder.value!!
    }

    override suspend fun getEntryByPath(entryPath: EntryPath): Entry? {
        return file
    }

    override fun saveFile(
        programFile: ProgramFile,
        fileContent: FileContent,
        cursorPosition: CursorPosition
    ): Job {
        this.fileContent = fileContent
        return Job()
    }

    override fun saveFile(
        entryPath: EntryPath,
        fileContent: FileContent,
        cursorPosition: CursorPosition
    ): Job {
        this.fileContent = fileContent
        return Job()
    }

    override suspend fun getNotebookFileContent(notebookFile: NotebookFile): FileContent {
        TODO()
    }


    override suspend fun createFolder(path: EntryPath) {
    }

    override suspend fun deleteEntry(path: EntryPath) {
    }

    override suspend fun selectFile(entryPath: EntryPath) {

    }

    override suspend fun getFileContent(programFile: ProgramFile): FileContent {
        return fileContent
    }

    override suspend fun getCursorPosition(programFile: ProgramFile): CursorPosition {
        return CursorPosition(0)
    }
}