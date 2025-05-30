package io.github.arashiyama11.dncl_ide.domain.usecase

import arrow.core.getOrElse
import io.github.arashiyama11.dncl_ide.domain.model.CursorPosition
import io.github.arashiyama11.dncl_ide.domain.model.EntryPath
import io.github.arashiyama11.dncl_ide.domain.model.FileContent
import io.github.arashiyama11.dncl_ide.domain.model.FileName
import io.github.arashiyama11.dncl_ide.domain.model.NotebookFile
import io.github.arashiyama11.dncl_ide.domain.notebook.Cell
import io.github.arashiyama11.dncl_ide.domain.notebook.CellType
import io.github.arashiyama11.dncl_ide.domain.notebook.Metadata
import io.github.arashiyama11.dncl_ide.domain.notebook.Notebook
import io.github.arashiyama11.dncl_ide.domain.notebook.Output
import io.github.arashiyama11.dncl_ide.domain.repository.FileRepository
import io.github.arashiyama11.dncl_ide.interpreter.evaluator.EvaluatorFactory
import io.github.arashiyama11.dncl_ide.interpreter.lexer.Lexer
import io.github.arashiyama11.dncl_ide.interpreter.model.Environment
import io.github.arashiyama11.dncl_ide.interpreter.parser.Parser
import kotlinx.coroutines.channels.Channel
import kotlinx.serialization.json.Json

class NotebookFileUseCase(private val fileRepository: FileRepository) {

    private val json = Json {
        prettyPrint = true
        ignoreUnknownKeys = true
    }

    fun FileContent.toNotebook(): Notebook = json.decodeFromString(value)

    fun Notebook.toFileContent(): FileContent = FileContent(json.encodeToString(this))

    suspend fun executeCell(notebook: Notebook, cellId: String, env: Environment) {
        val cell = notebook.cells.find { it.id == cellId }
            ?: throw IllegalArgumentException("Cell with id $cellId not found in the notebook.")

        if (cell.type != CellType.CODE) {
            throw IllegalArgumentException("Only code cells can be executed.")
        }

        val program =
            Parser(Lexer(cell.source.joinToString("\n"))).getOrElse { return }.parseProgram()
                .getOrElse { return }
        val evaluator = EvaluatorFactory.create(
            Channel(), 0, null, null
        )
        evaluator.evalProgram(program, env)
    }

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

    suspend fun getNotebook(notebookFile: NotebookFile): Notebook {
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

    /**
     * 新しいセルを作成する
     *
     * @param id セルの一意識別子
     * @param type セルの種類 (CODE または MARKDOWN)
     * @param source セルのソースコード（行ごとのリスト）
     * @param executionCount 実行回数（コードセルの場合のみ使用）
     * @param outputs セルの出力（コードセルの場合のみ使用）
     * @return 新しいCellオブジェクト
     */
    fun createCell(
        id: String,
        type: CellType,
        source: List<String>,
        executionCount: Int? = if (type == CellType.CODE) 0 else null,
        outputs: List<Output>? = if (type == CellType.CODE) emptyList() else null
    ): Cell {
        return Cell(
            id = id,
            type = type,
            source = source,
            executionCount = executionCount,
            outputs = outputs
        )
    }

    /**
     * ノートブックファイルからノートブックコンテンツを取得する（getNotebookのエイリアス）
     */
    suspend fun getNotebookFileContent(notebookFile: NotebookFile): Notebook {
        return getNotebook(notebookFile)
    }
}
