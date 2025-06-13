package io.github.arashiyama11.dncl_ide.domain.usecase

import io.github.arashiyama11.dncl_ide.domain.model.CursorPosition
import io.github.arashiyama11.dncl_ide.domain.model.EntryPath
import io.github.arashiyama11.dncl_ide.domain.model.FileContent
import io.github.arashiyama11.dncl_ide.domain.model.FileName
import io.github.arashiyama11.dncl_ide.domain.model.Folder
import io.github.arashiyama11.dncl_ide.domain.model.FolderName
import io.github.arashiyama11.dncl_ide.domain.model.NotebookFile
import io.github.arashiyama11.dncl_ide.domain.model.ProgramFile
import io.github.arashiyama11.dncl_ide.domain.model.ValidationError
import io.github.arashiyama11.dncl_ide.domain.repository.FileRepository
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.test.runTest
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class FileNameValidationUseCaseTest {
    private lateinit var fileRepository: MockFileRepository
    private lateinit var fileNameValidationUseCase: FileNameValidationUseCase

    @BeforeTest
    fun setup() {
        fileRepository = MockFileRepository()
        fileNameValidationUseCase = FileNameValidationUseCase(fileRepository)
    }

    @Test
    fun `空のファイル名の場合はエラーを返す`() = runTest {
        val entryPath = EntryPath(listOf(FolderName("root"), FileName("")))
        val result = fileNameValidationUseCase(entryPath)
        assertTrue(result.isLeft())
        assertEquals(ValidationError.FileNameEmpty, result.leftOrNull())
    }

    @Test
    fun `禁止文字を含む場合はエラーを返す`() = runTest {
        val entryPath = EntryPath(listOf(FolderName("root"), FileName("file*name")))
        val result = fileNameValidationUseCase(entryPath)
        assertTrue(result.isLeft())
        assertTrue(result.leftOrNull() is ValidationError.ForbiddenChar)
    }

    @Test
    fun `最大長を超えるファイル名の場合はエラーを返す`() = runTest {
        val longName = "a".repeat(FileNameValidationUseCase.MAX_FILE_NAME_LENGTH + 1)
        val entryPath = EntryPath(listOf(FolderName("root"), FileName(longName)))
        val result = fileNameValidationUseCase(entryPath)
        assertTrue(result.isLeft())
        assertEquals(ValidationError.FileNameTooLong, result.leftOrNull())
    }

    @Test
    fun `予約名の場合はエラーを返す`() = runTest {
        val entryPath = EntryPath(listOf(FolderName("root"), FileName("CON")))
        val result = fileNameValidationUseCase(entryPath)
        assertTrue(result.isLeft())
        assertTrue(result.leftOrNull() is ValidationError.ReservedName)
    }

    @Test
    fun `親フォルダが存在しない場合はエラーを返す`() = runTest {
        val entryPath = EntryPath(listOf(FolderName("nonexistent"), FileName("test.txt")))
        fileRepository.mockGetEntryByPath = { null }
        val result = fileNameValidationUseCase(entryPath)
        assertTrue(result.isLeft())
        assertEquals(ValidationError.FolderNotFound, result.leftOrNull())
    }

    @Test
    fun `同名のファイルが既に存在する場合はエラーを返す`() = runTest {
        val folderName = FolderName("root")
        val fileName = FileName("existing.txt")
        val entryPath = EntryPath(listOf(folderName, fileName))
        val folder = Folder(
            folderName,
            EntryPath(listOf(folderName)),
            listOf(ProgramFile(fileName, entryPath))
        )
        fileRepository.mockGetEntryByPath = { folder }
        val result = fileNameValidationUseCase(entryPath)
        assertTrue(result.isLeft())
        assertTrue(result.leftOrNull() is ValidationError.AlreadyExists)
    }

    @Test
    fun `有効なファイル名の場合は成功を返す`() = runTest {
        val folderName = FolderName("root")
        val fileName = FileName("valid.txt")
        val entryPath = EntryPath(listOf(folderName, fileName))
        val folder = Folder(
            folderName,
            EntryPath(listOf(folderName)),
            emptyList()
        )
        fileRepository.mockGetEntryByPath = { folder }
        val result = fileNameValidationUseCase(entryPath)
        assertTrue(result.isRight())
        assertEquals(Unit, result.getOrNull())
    }
}

class MockFileRepository : FileRepository {
    override val rootFolder: StateFlow<Folder?>
        get() = MutableStateFlow(null)
    override val rootPath: EntryPath = EntryPath(listOf(FolderName("root")))
    override val selectedEntryPath: StateFlow<EntryPath?> = MutableStateFlow(null)
    var mockGetEntryByPath: (EntryPath) -> io.github.arashiyama11.dncl_ide.domain.model.Entry? =
        { null }

    override suspend fun getRootFolder() =
        Folder(FolderName("root"), EntryPath(listOf(FolderName("root"))), emptyList())

    override suspend fun getEntryByPath(entryPath: EntryPath) = mockGetEntryByPath(entryPath)
    override fun saveFile(
        programFile: ProgramFile,
        fileContent: io.github.arashiyama11.dncl_ide.domain.model.FileContent,
        cursorPosition: io.github.arashiyama11.dncl_ide.domain.model.CursorPosition
    ): Job {
        return Job()
    }

    override fun saveFile(
        entryPath: EntryPath,
        fileContent: FileContent,
        cursorPosition: CursorPosition
    ): Job {
        return Job()
    }

    override suspend fun getNotebookFileContent(notebookFile: NotebookFile): FileContent {
        return FileContent("")
    }

    override suspend fun createFolder(path: EntryPath) {}
    override suspend fun selectFile(entryPath: EntryPath) {}
    override suspend fun getFileContent(programFile: ProgramFile) =
        io.github.arashiyama11.dncl_ide.domain.model.FileContent("")

    override suspend fun getCursorPosition(programFile: ProgramFile) =
        io.github.arashiyama11.dncl_ide.domain.model.CursorPosition(0)
} 