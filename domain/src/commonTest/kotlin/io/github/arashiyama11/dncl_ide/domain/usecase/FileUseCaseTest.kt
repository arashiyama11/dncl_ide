package io.github.arashiyama11.dncl_ide.domain.usecase

import io.github.arashiyama11.dncl_ide.domain.model.CursorPosition
import io.github.arashiyama11.dncl_ide.domain.model.EntryPath
import io.github.arashiyama11.dncl_ide.domain.model.FileContent
import io.github.arashiyama11.dncl_ide.domain.model.FileName
import io.github.arashiyama11.dncl_ide.domain.model.Folder
import io.github.arashiyama11.dncl_ide.domain.model.FolderName
import io.github.arashiyama11.dncl_ide.domain.model.ProgramFile
import io.github.arashiyama11.dncl_ide.domain.repository.FileRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.test.runTest
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertNull

class FileUseCaseTest {
    private lateinit var fileRepository: MockFileRepository
    private lateinit var fileUseCase: FileUseCase

    @BeforeTest
    fun setup() {
        fileRepository = MockFileRepository()
        fileUseCase = FileUseCase(fileRepository)
    }

    @Test
    fun `getEntryByPathはリポジトリから正しいエントリーを取得すること`() = runTest {
        val entryPath = EntryPath(listOf(FolderName("root"), FileName("test.txt")))
        val expectedFile = ProgramFile(FileName("test.txt"), entryPath)
        fileRepository.mockGetEntryByPath = { expectedFile }
        val result = fileUseCase.getEntryByPath(entryPath)
        assertEquals(expectedFile, result)
    }

    @Test
    fun `getEntryByPathは存在しないパスに対してnullを返すこと`() = runTest {
        fileRepository.mockGetEntryByPath = { null }
        val entryPath = EntryPath(listOf(FolderName("nonexistent"), FileName("test.txt")))
        val result = fileUseCase.getEntryByPath(entryPath)
        assertNull(result)
    }

    @Test
    fun `saveFileはリポジトリのsaveFileを正しく呼び出すこと`() = runTest {
        val entryPath = EntryPath(listOf(FolderName("root"), FileName("test.txt")))
        val programFile = ProgramFile(FileName("test.txt"), entryPath)
        val fileContent = FileContent("テストコンテンツ")
        val cursorPosition = CursorPosition(5)
        var calledWithFile: ProgramFile? = null
        var calledWithContent: FileContent? = null
        var calledWithPosition: CursorPosition? = null
        fileRepository.mockSaveFile = { file, content, position ->
            calledWithFile = file
            calledWithContent = content
            calledWithPosition = position
        }
        fileUseCase.saveFile(programFile, fileContent, cursorPosition)
        assertEquals(programFile, calledWithFile)
        assertEquals(fileContent, calledWithContent)
        assertEquals(cursorPosition, calledWithPosition)
    }

    @Test
    fun `selectFileはリポジトリのselectFileを正しく呼び出すこと`() = runTest {
        val entryPath = EntryPath(listOf(FolderName("root"), FileName("test.txt")))
        var calledWithPath: EntryPath? = null
        fileRepository.mockSelectFile = { path ->
            calledWithPath = path
        }
        fileUseCase.selectFile(entryPath)
        assertEquals(entryPath, calledWithPath)
    }

    @Test
    fun `createFileは正しくファイルを作成すること`() = runTest {
        val parentPath = EntryPath(listOf(FolderName("root")))
        val fileName = FileName("newfile.txt")
        val expectedPath = EntryPath(listOf(FolderName("root"), fileName))
        var savedFile: ProgramFile? = null
        var savedContent: FileContent? = null
        var savedPosition: CursorPosition? = null
        fileRepository.mockSaveFile = { file, content, position ->
            savedFile = file
            savedContent = content
            savedPosition = position
        }
        fileUseCase.createFile(parentPath, fileName)
        assertNotNull(savedFile)
        assertEquals(fileName, savedFile?.name)
        assertEquals(expectedPath, savedFile?.path)
        assertEquals("", savedContent?.value)
        assertEquals(0, savedPosition?.value)
    }

    @Test
    fun `createFolderはリポジトリのcreateFolder関数を正しく呼び出すこと`() = runTest {
        val parentPath = EntryPath(listOf(FolderName("root")))
        val folderName = FolderName("newfolder")
        val expectedPath = EntryPath(listOf(FolderName("root"), folderName))
        var calledWithPath: EntryPath? = null
        fileRepository.mockCreateFolder = { path ->
            calledWithPath = path
        }
        fileUseCase.createFolder(parentPath, folderName)
        assertEquals(expectedPath, calledWithPath)
    }

    @Test
    fun `getRootFolderはリポジトリから正しくルートフォルダを取得すること`() = runTest {
        val rootFolder = Folder(
            FolderName("root"),
            EntryPath(listOf(FolderName("root"))),
            emptyList()
        )
        fileRepository.mockGetRootFolder = { rootFolder }
        val result = fileUseCase.getRootFolder()
        assertEquals(rootFolder, result)
    }

    @Test
    fun `getCursorPositionはリポジトリから正しいカーソル位置を取得すること`() = runTest {
        val entryPath = EntryPath(listOf(FolderName("root"), FileName("test.txt")))
        val programFile = ProgramFile(FileName("test.txt"), entryPath)
        val expectedPosition = CursorPosition(10)
        fileRepository.mockGetCursorPosition = { expectedPosition }
        val result = fileUseCase.getCursorPosition(programFile)
        assertEquals(expectedPosition, result)
    }

    @Test
    fun `getFileContentはリポジトリから正しいファイル内容を取得すること`() = runTest {
        val entryPath = EntryPath(listOf(FolderName("root"), FileName("test.txt")))
        val programFile = ProgramFile(FileName("test.txt"), entryPath)
        val expectedContent = FileContent("テストコンテンツ")
        fileRepository.mockGetFileContent = { expectedContent }
        val result = fileUseCase.getFileContent(programFile)
        assertEquals(expectedContent, result)
    }

    @Test
    fun `selectedEntryPathはリポジトリから取得されること`() = runTest {
        val entryPath = EntryPath(listOf(FolderName("root"), FileName("selected.txt")))
        fileRepository.selectedEntryPathFlow.value = entryPath
        val result = fileUseCase.selectedEntryPath.value
        assertEquals(entryPath, result)
    }

    private class MockFileRepository : FileRepository {
        val selectedEntryPathFlow = MutableStateFlow<EntryPath?>(null)
        override val selectedEntryPath: StateFlow<EntryPath?> = selectedEntryPathFlow
        var mockGetRootFolder: () -> Folder = {
            Folder(FolderName("root"), EntryPath(listOf(FolderName("root"))), emptyList())
        }
        var mockGetEntryByPath: (EntryPath) -> io.github.arashiyama11.dncl_ide.domain.model.Entry? =
            { null }
        var mockSaveFile: (ProgramFile, FileContent, CursorPosition) -> Unit = { _, _, _ -> }
        var mockCreateFolder: (EntryPath) -> Unit = { }
        var mockSelectFile: (EntryPath) -> Unit = { selectedEntryPathFlow.value = it }
        var mockGetFileContent: (ProgramFile) -> FileContent = { FileContent("") }
        var mockGetCursorPosition: (ProgramFile) -> CursorPosition = { CursorPosition(0) }
        override suspend fun getRootFolder(): Folder = mockGetRootFolder()
        override suspend fun getEntryByPath(entryPath: EntryPath) = mockGetEntryByPath(entryPath)
        override suspend fun saveFile(
            programFile: ProgramFile,
            fileContent: FileContent,
            cursorPosition: CursorPosition
        ) = mockSaveFile(programFile, fileContent, cursorPosition)

        override suspend fun createFolder(path: EntryPath) = mockCreateFolder(path)
        override suspend fun selectFile(entryPath: EntryPath) = mockSelectFile(entryPath)
        override suspend fun getFileContent(programFile: ProgramFile): FileContent =
            mockGetFileContent(programFile)

        override suspend fun getCursorPosition(programFile: ProgramFile): CursorPosition =
            mockGetCursorPosition(programFile)
    }
}
