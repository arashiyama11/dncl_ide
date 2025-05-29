package io.github.arashiyama11.dncl_ide.domain.usecase

import io.github.arashiyama11.dncl_ide.domain.model.CursorPosition
import io.github.arashiyama11.dncl_ide.domain.model.EntryPath
import io.github.arashiyama11.dncl_ide.domain.model.FileContent
import io.github.arashiyama11.dncl_ide.domain.model.FileName
import io.github.arashiyama11.dncl_ide.domain.model.NotebookFile
import io.github.arashiyama11.dncl_ide.domain.notebook.Cell
import io.github.arashiyama11.dncl_ide.domain.notebook.CellType
import io.github.arashiyama11.dncl_ide.domain.notebook.Metadata
import io.github.arashiyama11.dncl_ide.domain.notebook.Notebook
import io.github.arashiyama11.dncl_ide.domain.repository.FileRepository
import kotlinx.serialization.json.Json

class NotebookFileUseCase(private val fileRepository: FileRepository) {

    private val json = Json {
        prettyPrint = true
        ignoreUnknownKeys = true
    }

    fun FileContent.toNotebook(): Notebook = json.decodeFromString(value)

    fun Notebook.toFileContent(): FileContent = FileContent(json.encodeToString(this))

    suspend fun createNotebookFile(
        parentPath: EntryPath,
        fileName: FileName
    ) {
        val notebook = Notebook(
            metadata = Metadata(dnclVersion = "0.0.0"),
            cells = listOf(
                Cell(
                    id = "cell-1",
                    type = CellType.CODE,
                    source = listOf("表示する('Hello, world!')"),
                    executionCount = 0,
                    outputs = emptyList()
                ),
                Cell(
                    id = "cell-2",
                    type = CellType.MARKDOWN,
                    source = listOf("# タイトル", "これはマークダウンセルです。"),
                    executionCount = null,
                    outputs = emptyList()
                )
            )
        )
        val content = notebook.toFileContent()
        fileRepository.saveFile(parentPath + fileName, content, cursorPosition = CursorPosition(0))
    }

    suspend fun saveNotebookFile(
        notebookFile: NotebookFile,
        fileContent: FileContent,
        cursorPosition: CursorPosition
    ) {
        fileRepository.saveFile(notebookFile.path, fileContent, cursorPosition)
    }

    suspend fun getNotebookFileContent(notebookFile: NotebookFile): Notebook {
        return fileRepository.getNotebookFileContent(notebookFile).toNotebook()
    }

    suspend fun modifyNotebookCell(
        notebook: Notebook,
        cellId: String,
        newCell: Cell
    ): Notebook {
        val updatedCells = notebook.cells.map { cell ->
            if (cell.id == cellId) newCell else cell
        }
        return notebook.copy(cells = updatedCells)
    }
}