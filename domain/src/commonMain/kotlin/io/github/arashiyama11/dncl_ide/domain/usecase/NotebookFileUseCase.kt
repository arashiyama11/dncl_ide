package io.github.arashiyama11.dncl_ide.domain.usecase

import arrow.core.getOrElse
import io.github.arashiyama11.dncl_ide.domain.model.CursorPosition
import io.github.arashiyama11.dncl_ide.domain.model.DnclOutput
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
import io.github.arashiyama11.dncl_ide.interpreter.evaluator.CallBuiltInFunctionScope
import io.github.arashiyama11.dncl_ide.interpreter.model.DnclObject
import kotlinx.coroutines.withTimeoutOrNull
import io.github.arashiyama11.dncl_ide.domain.model.FolderName
import io.github.arashiyama11.dncl_ide.domain.model.ProgramFile
import io.github.arashiyama11.dncl_ide.interpreter.model.explain
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ensureActive
import kotlinx.coroutines.withContext
import kotlin.collections.plus

class NotebookFileUseCase(private val fileRepository: FileRepository) {

    private val json = Json {
        prettyPrint = true
        ignoreUnknownKeys = true
    }

    fun FileContent.toNotebook(): Notebook = Json.decodeFromString(value)

    fun Notebook.toFileContent(): FileContent = FileContent(json.encodeToString(this))

    suspend fun executeCell(notebook: Notebook, cellId: String, env: Environment): Output? =
        withContext(
            Dispatchers.Default
        ) {
            val cell = notebook.cells.find { it.id == cellId }
                ?: throw IllegalArgumentException("Cell with id $cellId not found in the notebook.")

            if (cell.type != CellType.CODE) {
                throw IllegalArgumentException("Only code cells can be executed.")
            }

            val code = cell.source.joinToString("\n")

            val output = run {

                val program =
                    Parser(Lexer(code)).getOrElse { return@run DnclOutput.Error(it.explain(code)) }
                        .parseProgram()
                        .getOrElse { return@run DnclOutput.Error(it.explain(code)) }
                var i = 0
                val evaluator = EvaluatorFactory.create(
                    Channel(), 0, null, { _, _ ->
                        if (i++ > 1000000) {
                            coroutineContext.ensureActive()
                            i = 0
                        }
                    }
                )
                evaluator.evalProgram(program, env).fold(
                    ifLeft = {
                        DnclOutput.Error(it.explain(code))
                    }
                ) {
                    when (it) {
                        is DnclObject.Error -> {
                            DnclOutput.RuntimeError(it)
                        }

                        is DnclObject.Nothing -> {
                            null
                        }

                        else -> {
                            DnclOutput.Stdout(it.toString())
                        }
                    }
                }
            }

            when (output) {
                is DnclOutput.Stdout -> {
                    Output(
                        outputType = "stream",
                        name = "stdout",
                        text = listOf(output.value)
                    )
                }

                is DnclOutput.Error -> {
                    Output(
                        outputType = "error",
                        text = listOf(output.value),
                        evalue = output.value,
                        ename = output::class.simpleName,
                    )
                }

                is DnclOutput.RuntimeError -> {
                    Output(
                        outputType = "error",
                        text = listOf(output.value.explain(code)),
                        evalue = output.value.explain(code),
                        ename = "RuntimeError",
                    )
                }

                else -> null
            }
        }

    fun createNotebookFile(
        parentPath: EntryPath,
        fileName: FileName
    ) {
        val notebook = Notebook(
            metadata = Metadata(dnclVersion = "0.0.0"),
            cells = listOf(
                Cell(
                    id = "cell-1",
                    type = CellType.CODE,
                    source = listOf(""""表示する("Hello, world!")"""),
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

    fun saveNotebookFile(
        notebookFile: NotebookFile,
        fileContent: FileContent,
        cursorPosition: CursorPosition
    ) {
        fileRepository.saveFile(notebookFile.path, fileContent, cursorPosition)
    }

    suspend fun getNotebook(notebookFile: NotebookFile): Notebook {
        return fileRepository.getNotebookFileContent(notebookFile).toNotebook()
    }

    fun modifyNotebookCell(
        notebook: Notebook,
        cellId: String,
        newCell: Cell
    ): Notebook {
        val updatedCells = notebook.cells.map { cell ->
            if (cell.id == cellId) newCell else cell
        }
        return notebook.copy(cells = updatedCells)
    }

    fun modifyNotebookOutput(
        notebook: Notebook,
        cellId: String,
        newOutputs: List<Output>
    ): Notebook {
        val updatedCells = notebook.cells.map { cell ->
            if (cell.id == cellId) {
                cell.copy(outputs = newOutputs)
            } else {
                cell
            }
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

    /**
     * セルを更新し、ファイルに保存する
     * @param notebookFile 対象のノートブックファイル情報
     * @param notebook 現在のNotebookオブジェクト
     * @param cellId 更新対象セルID
     * @param updateBlock 古いCellから新しいCellを生成するラムダ
     * @return 更新後のNotebookオブジェクト
     */
    fun updateCellAndSave(
        notebookFile: NotebookFile,
        notebook: Notebook,
        cellId: String,
        updateBlock: (Cell) -> Cell
    ): Notebook {
        // セルの新規作成
        val oldCell = notebook.cells.firstOrNull { it.id == cellId }
            ?: throw IllegalArgumentException("Cell with id $cellId not found in the notebook.")
        val newCell = updateBlock(oldCell)
        // ノートブックを更新
        val updatedNotebook = modifyNotebookCell(notebook, cellId, newCell)
        // ファイルに保存
        saveNotebookFile(
            notebookFile,
            updatedNotebook.toFileContent(),
            CursorPosition(0)
        )
        return updatedNotebook
    }

    /**
     * 新しいセルを指定位置に挿入し、ファイルに保存する
     */
    fun insertCellAndSave(
        notebookFile: NotebookFile,
        notebook: Notebook,
        newCell: Cell,
        afterCellId: String? = null
    ): Notebook {
        val cells = notebook.cells.toMutableList()
        val insertIndex = afterCellId?.let { id ->
            val idx = cells.indexOfFirst { it.id == id }
            if (idx != -1) idx + 1 else cells.size
        } ?: 0
        if (insertIndex in 0..cells.size) {
            cells.add(insertIndex, newCell)
        } else {
            cells.add(newCell)
        }
        val updatedNotebook = notebook.copy(cells = cells)
        saveNotebookFile(
            notebookFile,
            updatedNotebook.toFileContent(),
            CursorPosition(0)
        )
        return updatedNotebook
    }

    /**
     * 指定セルを削除し、ファイルに保存する
     */
    fun deleteCellAndSave(
        notebookFile: NotebookFile,
        notebook: Notebook,
        cellId: String
    ): Notebook {
        val updatedCells = notebook.cells.filterNot { it.id == cellId }
        val updatedNotebook = notebook.copy(cells = updatedCells)
        saveNotebookFile(
            notebookFile,
            updatedNotebook.toFileContent(),
            CursorPosition(0)
        )
        return updatedNotebook
    }

    /**
     * セルのタイプを変更してファイルに保存する
     */
    fun changeCellTypeAndSave(
        notebookFile: NotebookFile,
        notebook: Notebook,
        cellId: String,
        newType: CellType
    ): Notebook {
        val cell = notebook.cells.firstOrNull { it.id == cellId }
            ?: throw IllegalArgumentException("Cell with id $cellId not found in the notebook.")
        val updatedCell = createCell(
            id = cellId,
            type = newType,
            source = cell.source,
            executionCount = if (newType == CellType.CODE) 0 else null,
            outputs = if (newType == CellType.CODE) emptyList() else null
        )
        val updatedNotebook = modifyNotebookCell(notebook, cellId, updatedCell)
        saveNotebookFile(
            notebookFile,
            updatedNotebook.toFileContent(),
            CursorPosition(0)
        )
        return updatedNotebook
    }

    /**
     * 指定セルの出力をクリアしてファイルに保存する
     */
    fun clearCellOutput(
        notebookFile: NotebookFile,
        notebook: Notebook,
        cellId: String
    ): Notebook {
        val updatedCells = notebook.cells.map { cell ->
            if (cell.id == cellId) {
                cell.copy(outputs = emptyList(), executionCount = 0)
            } else {
                cell
            }
        }
        val updatedNotebook = notebook.copy(cells = updatedCells)
        return updatedNotebook
    }


    fun appendOutput(
        notebookFile: NotebookFile,
        notebook: Notebook,
        cellId: String,
        newOutput: Output
    ): Notebook {
        val oldCell = notebook.cells.firstOrNull { it.id == cellId }
            ?: throw IllegalArgumentException("Cell with id $cellId not found in the notebook.")
        val prevOutputs = oldCell.outputs ?: emptyList()
        val mergedOutputs =
            if (prevOutputs.isNotEmpty() && prevOutputs.last().outputType == newOutput.outputType) {
                val last = prevOutputs.last()
                val mergedText = (last.text ?: emptyList()) + (newOutput.text ?: emptyList())
                val mergedOutput = last.copy(text = mergedText)
                prevOutputs.dropLast(1) + mergedOutput
            } else {
                prevOutputs + newOutput
            }
        val updatedCell = oldCell.copy(
            outputs = mergedOutputs,
            executionCount = (oldCell.executionCount ?: 0) + 1
        )
        val updatedNotebook = modifyNotebookCell(notebook, cellId, updatedCell)

        return updatedNotebook
    }

    suspend fun CallBuiltInFunctionScope.importAndExecute(
        notebookFile: NotebookFile,
        importPath: String,
        env: Environment
    ): DnclObject = withContext(Dispatchers.Default) {
        // パスを分割してEntryPathを生成
        val parts = importPath.split("/")
        // ファイル取得 (タイムアウト付き)
        val entry = withTimeoutOrNull(100) {
            fileRepository.getEntryByPath(
                EntryPath(
                    parts.dropLast(1).map { FolderName(it) } + FileName(
                        parts.last()
                    )
                )).apply { if (this != null) return@withTimeoutOrNull this }

            fileRepository.getEntryByPath(
                fileRepository.rootPath + EntryPath(
                    parts.dropLast(1).map { FolderName(it) } + FileName(
                        parts.last()
                    )
                )
            )
        }
        return@withContext if (entry is ProgramFile) {
            val content = fileRepository.getFileContent(entry).value
            val parser = Parser(Lexer(content)).getOrElse { err ->
                return@withContext DnclObject.RuntimeError(
                    err.explain(content),
                    astNode
                )
            }
            val prog = parser.parseProgram().getOrElse { err ->
                return@withContext DnclObject.RuntimeError(
                    err.explain(content),
                    astNode
                )
            }
            // 新規Evaluatorで同じ環境に対して実行
            var i = 0
            val evaluator = EvaluatorFactory.create(
                Channel(), 0, null, { _, _ ->
                    if (i++ > 1000000) {
                        coroutineContext.ensureActive()
                        i = 0
                    }
                }
            )
            evaluator.evalProgram(prog, env).fold(
                ifLeft = { lhs ->
                    DnclObject.RuntimeError(
                        lhs.message.orEmpty(),
                        astNode
                    )
                },
                ifRight = {
                    DnclObject.Null(astNode)
                }
            )
        } else {
            DnclObject.RuntimeError(
                "ファイル:$importPath が見つかりません",
                astNode
            )
        }
    }
}
