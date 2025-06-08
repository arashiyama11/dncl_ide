package io.github.arashiyama11.dncl_ide.domain.usecase

import io.github.arashiyama11.dncl_ide.domain.repository.FileRepository
import io.github.arashiyama11.dncl_ide.domain.model.*
import io.github.arashiyama11.dncl_ide.domain.notebook.*
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.test.runTest
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull

class NotebookFileUseCaseTest {
    private lateinit var useCase: NotebookFileUseCase

    private class FakeFileRepository() : FileRepository {
        override val rootFolder: StateFlow<Folder?> = MutableStateFlow(null)
        override val rootPath: EntryPath = EntryPath(listOf(FolderName("root")))
        override val selectedEntryPath: StateFlow<EntryPath?> = MutableStateFlow(null)
        override suspend fun getRootFolder(): Folder = throw NotImplementedError()
        override suspend fun getEntryByPath(entryPath: EntryPath): Entry? =
            throw NotImplementedError()

        override fun saveFile(
            programFile: ProgramFile,
            fileContent: FileContent,
            cursorPosition: CursorPosition
        ) = throw NotImplementedError()

        override fun saveFile(
            entryPath: EntryPath,
            fileContent: FileContent,
            cursorPosition: CursorPosition
        ) = throw NotImplementedError()

        override suspend fun getNotebookFileContent(notebookFile: NotebookFile): FileContent =
            throw NotImplementedError()

        override suspend fun createFolder(path: EntryPath) = throw NotImplementedError()
        override suspend fun selectFile(entryPath: EntryPath) = throw NotImplementedError()
        override suspend fun getFileContent(programFile: ProgramFile): FileContent =
            throw NotImplementedError()

        override suspend fun getCursorPosition(programFile: ProgramFile): CursorPosition =
            throw NotImplementedError()
    }

    @BeforeTest
    fun setup() {
        useCase = NotebookFileUseCase(FakeFileRepository())
    }

    @Test
    fun testToNotebookAndBack() {
        val original = Notebook(
            metadata = Metadata(
                dnclVersion = "1.2.3",
                kernelspec = KernelSpec(name = "k", language = "lang", version = null)
            ),
            cells = listOf(
                Cell(
                    id = "1",
                    type = CellType.CODE,
                    source = listOf("print('a')"),
                    executionCount = 1,
                    outputs = listOf(
                        Output(
                            outputType = "stream",
                            name = "o",
                            text = listOf("t"),
                            ename = null,
                            evalue = null,
                            traceback = null
                        )
                    )
                ),
                Cell(
                    id = "2",
                    type = CellType.MARKDOWN,
                    source = listOf("md"),
                    executionCount = null,
                    outputs = null
                )
            )
        )
        val content = with(useCase) { original.toFileContent() }
        val decoded = with(useCase) { content.toNotebook() }
        assertEquals(original, decoded)
    }

    @Test
    fun testCreateCellDefaults() {
        val codeCell = useCase.createCell(
            id = "c",
            type = CellType.CODE,
            source = listOf("s")
        )
        assertEquals("c", codeCell.id)
        assertEquals(CellType.CODE, codeCell.type)
        assertEquals(listOf("s"), codeCell.source)
        assertEquals(0, codeCell.executionCount)
        assertEquals(emptyList<Output>(), codeCell.outputs)

        val mdCell = useCase.createCell(
            id = "m",
            type = CellType.MARKDOWN,
            source = listOf("m")
        )
        assertEquals("m", mdCell.id)
        assertEquals(CellType.MARKDOWN, mdCell.type)
        assertEquals(listOf("m"), mdCell.source)
        assertNull(mdCell.executionCount)
        assertNull(mdCell.outputs)
    }

    @Test
    fun testModifyNotebookCell() = runTest {
        val cell1 = useCase.createCell(
            id = "c1",
            type = CellType.CODE,
            source = listOf("1")
        )
        val cell2 = useCase.createCell(
            id = "c2",
            type = CellType.CODE,
            source = listOf("2")
        )
        val notebook = Notebook(
            metadata = Metadata(dnclVersion = "v"),
            cells = listOf(cell1, cell2)
        )
        val newCell2 = useCase.createCell(
            id = "c2",
            type = CellType.CODE,
            source = listOf("updated"),
            executionCount = 5,
            outputs = listOf(
                Output(
                    outputType = "t",
                    name = null,
                    text = null,
                    ename = null,
                    evalue = null,
                    traceback = null
                )
            )
        )
        val modified = useCase.modifyNotebookCell(notebook, "c2", newCell2)
        assertEquals(2, modified.cells.size)
        assertEquals(cell1, modified.cells[0])
        assertEquals(newCell2, modified.cells[1])
    }
} 