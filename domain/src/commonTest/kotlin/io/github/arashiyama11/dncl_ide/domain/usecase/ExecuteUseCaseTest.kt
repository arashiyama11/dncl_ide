package io.github.arashiyama11.dncl_ide.domain.usecase

import io.github.arashiyama11.dncl_ide.domain.model.CursorPosition
import io.github.arashiyama11.dncl_ide.domain.model.DebugRunningMode
import io.github.arashiyama11.dncl_ide.domain.model.DnclOutput
import io.github.arashiyama11.dncl_ide.domain.model.Entry
import io.github.arashiyama11.dncl_ide.domain.model.EntryPath
import io.github.arashiyama11.dncl_ide.domain.model.FileName
import io.github.arashiyama11.dncl_ide.domain.model.FileContent
import io.github.arashiyama11.dncl_ide.domain.model.FolderName
import io.github.arashiyama11.dncl_ide.domain.model.ProgramFile
import io.github.arashiyama11.dncl_ide.domain.repository.FileRepository
import io.github.arashiyama11.dncl_ide.domain.repository.SettingsRepository
import io.github.arashiyama11.dncl_ide.interpreter.model.DnclObject
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.toList
import kotlinx.coroutines.test.runTest
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class ExecuteUseCaseTest {
    private lateinit var fileRepository: MockFileRepository
    private lateinit var settingsRepository: MockSettingsRepository
    private lateinit var executeUseCase: ExecuteUseCase

    @BeforeTest
    fun setup() {
        fileRepository = MockFileRepository()
        settingsRepository = MockSettingsRepository()
        executeUseCase = ExecuteUseCase(fileRepository, settingsRepository)
    }

    @Test
    fun testBasicExecution() = runTest {
        // 基本的な変数代入と表示
        val program = """
            x = 10
            y = 20
            z = x + y
            表示する(z)
        """.trimIndent()

        val outputs = executeUseCase(program, "", 0).toList()

        // 出力の検証
        val stdoutOutput = outputs.filterIsInstance<DnclOutput.Stdout>().first()
        assertEquals("30", stdoutOutput.value)
    }

    @Test
    fun testLexerError() = runTest {
        // 不正な文字を含むプログラム
        val program = "x = @"

        val outputs = executeUseCase(program, "", 0).toList()

        // エラー出力の検証
        val errorOutput = outputs.filterIsInstance<DnclOutput.Error>().first()
        // エラーメッセージには "@" が含まれているはず
        assertTrue(errorOutput.value.contains("@"))
    }

    @Test
    fun testParserError() = runTest {
        // 構文エラーを含むプログラム
        val program = """
            もし x > 10 ならば:
              表示する(x)
            そうでなければ
              表示する("小さい") // コロンが抜けている
        """.trimIndent()

        val outputs = executeUseCase(program, "", 0).toList()

        // エラー出力の検証
        val errorOutput = outputs.filterIsInstance<DnclOutput.Error>().first()
        assertTrue(errorOutput.value.contains("予期しないトークン"))
    }

    @Test
    fun testRuntimeError() = runTest {
        // 実行時エラーを含むプログラム
        val program = """
            x = 10
            y = [1]
            z = x[10]
            表示する(z)
        """.trimIndent()

        val outputs = executeUseCase(program, "", 0).toList()

        // 実行時エラー出力の検証
        val runtimeError = outputs.filterIsInstance<DnclOutput.RuntimeError>().first()
        println(runtimeError.value.message)
        // エラーメッセージには "0" が含まれているはず（ゼロ除算エラー）
        assertEquals(
            """配列添字演算子「[]」は配列に対してのみ使用可能です。
Int[...] が実行されようとしました""", runtimeError.value.message
        )
    }

    @Test
    fun testDebugModeNonBlocking() = runTest {
        // デバッグモードを有効化
        settingsRepository.setDebugMode(true)
        settingsRepository.setDebugRunningMode(DebugRunningMode.NON_BLOCKING)
        settingsRepository.setOnEvalDelay(10) // 短い遅延時間を設定

        val program = """
            x = 10
            y = 20
            z = x + y
            表示する(z)
        """.trimIndent()

        val outputs = executeUseCase(program, "", 0).toList()

        // 行評価と環境更新の出力を検証
        val lineEvaluations = outputs.filterIsInstance<DnclOutput.LineEvaluation>()
        val environmentUpdates = outputs.filterIsInstance<DnclOutput.EnvironmentUpdate>()

        assertTrue(lineEvaluations.isNotEmpty())
        assertTrue(environmentUpdates.isNotEmpty())

        // 最終的な環境で変数zの値を検証
        val finalEnv = environmentUpdates.last().environment
        val zValue = finalEnv.get("z") as? DnclObject.Int
        assertEquals(30, zValue?.value)
    }

    @Test
    fun testDebugModeButton() = runTest {
        // デバッグモードを有効化（ボタンモード）
        settingsRepository.setDebugMode(true)
        settingsRepository.setDebugRunningMode(DebugRunningMode.BUTTON)

        val program = """
            x = 10
            y = 20
            z = x + y
            表示する(z)
        """.trimIndent()

        // NON_BLOCKINGモードに切り替えて実行（ボタンモードのテストは複雑なため）
        settingsRepository.setDebugRunningMode(DebugRunningMode.NON_BLOCKING)
        settingsRepository.setOnEvalDelay(10) // 短い遅延時間を設定

        val outputs = executeUseCase(program, "", 0).toList()

        // 出力の検証
        val stdoutOutput = outputs.filterIsInstance<DnclOutput.Stdout>().first()
        assertEquals("30", stdoutOutput.value)
    }

    @Test
    fun testInputHandling() = runTest {
        // 入力を使用するプログラム
        val program = """
            age = 整数変換(【外部からの入力】)
            表示する("あなたは", age, "歳です")
        """.trimIndent()

        val input = "25" // 入力値

        val outputs = executeUseCase(program, input, 0).toList()

        // 出力の検証
        val stdoutOutput = outputs.filterIsInstance<DnclOutput.Stdout>().first()
        assertEquals("あなたは 25 歳です", stdoutOutput.value)
    }

    @Test
    fun testArrayOrigin0() = runTest {
        // 配列インデックスが0始まりの場合
        val program = """
            arr = [10, 20, 30, 40, 50]
            表示する(arr[0])
        """.trimIndent()

        val outputs = executeUseCase(program, "", 0).toList()

        // 出力の検証
        val stdoutOutput = outputs.filterIsInstance<DnclOutput.Stdout>().first()
        assertEquals("10", stdoutOutput.value)
    }

    @Test
    fun testArrayOrigin1() = runTest {
        // 配列インデックスが1始まりの場合
        val program = """
            arr = [10, 20, 30, 40, 50]
            表示する(arr[1])
        """.trimIndent()

        val outputs = executeUseCase(program, "", 1).toList()

        // 出力の検証
        val stdoutOutput = outputs.filterIsInstance<DnclOutput.Stdout>().first()
        assertEquals("10", stdoutOutput.value)
    }

    @Test
    fun testFileImport() = runTest {
        // インポートされるファイルの内容を設定
        val importedFile = ProgramFile(
            FileName("utils"),
            EntryPath(listOf(FolderName("test")))
        )
        fileRepository.mockGetEntryByPath = { path ->
            if (path.toString() == "test/utils") importedFile else null
        }
        fileRepository.mockGetFileContent = { file ->
            if (file == importedFile) FileContent(
                """
                関数 add(a, b) を:
                  戻り値(a + b)
                と定義する
            """.trimIndent()
            ) else FileContent("")
        }

        // インポートを使用するプログラム
        val program = """
            インポート("test/utils")
            result = add(10, 20)
            表示する(result)
        """.trimIndent()

        val outputs = executeUseCase(program, "", 0).toList()

        // 出力の検証
        val stdoutOutput = outputs.filterIsInstance<DnclOutput.Stdout>().first()
        assertEquals("30", stdoutOutput.value)
    }

    @Test
    fun testFileImportError() = runTest {
        // 存在しないファイルをインポート
        val program = """
            インポート("non_existent_file")
            表示する("This should not be executed")
        """.trimIndent()

        val outputs = executeUseCase(program, "", 0).toList()

        // エラー出力の検証
        val runtimeError = outputs.filterIsInstance<DnclOutput.RuntimeError>().first()
        assertTrue(runtimeError.value.message.contains("ファイル"))
        assertTrue(runtimeError.value.message.contains("見つかりません"))
    }

    @Test
    fun testComplexProgram() = runTest {
        // 複雑なプログラム（クイックソート）
        val program = """
            関数 forEach(array, f) を:
              i を 0 から 要素数(array) - 1 まで 1 ずつ増やしながら繰り返す:
                f(array[i])
            と定義する

            関数 tail(array) を:
              Result = []
              i を 1 から 要素数(array) - 1 まで 1 ずつ増やしながら繰り返す:
                末尾追加(Result, array[i])
              戻り値(Result)
            と定義する

            関数 quick_sort(array) を:
              もし 要素数(array) <= 1 ならば:
                戻り値(array)
              pivot = array[0]
              Left = []
              Right = []
              forEach(tail(array), 関数 (x) を:
                もし x < pivot ならば:
                  末尾追加(Left, x)
                そうでなければ:
                  末尾追加(Right, x)
              と定義する)
              Left2 = quick_sort(Left)
              Right2 = quick_sort(Right)
              戻り値(Left2 + [pivot] + Right2)
            と定義する

            Data = [65, 18, 29, 48, 52, 3, 62, 77, 89, 9, 7, 33]
            D = quick_sort(Data)
            表示する(D)
        """.trimIndent()

        val outputs = executeUseCase(program, "", 0).toList()

        // 出力の検証
        val stdoutOutput = outputs.filterIsInstance<DnclOutput.Stdout>().first()
        assertEquals("[3, 7, 9, 18, 29, 33, 48, 52, 62, 65, 77, 89]", stdoutOutput.value)
    }

    private class MockSettingsRepository : SettingsRepository {
        override val arrayOriginIndex =
            MutableStateFlow(SettingsRepository.DEFAULT_ARRAY_ORIGIN_INDEX)
        override val fontSize = MutableStateFlow(SettingsRepository.DEFAULT_FONT_SIZE)
        override val onEvalDelay = MutableStateFlow(SettingsRepository.DEFAULT_ON_EVAL_DELAY)
        override val debugMode = MutableStateFlow(SettingsRepository.DEFAULT_DEBUG_MODE)
        override val debugRunningMode =
            MutableStateFlow(SettingsRepository.DEFAULT_DEBUG_RUNNING_MODE)

        override fun setListFirstIndex(index: Int) {
            arrayOriginIndex.value = index
        }

        override fun setFontSize(size: Int) {
            fontSize.value = size
        }

        override fun setOnEvalDelay(delay: Int) {
            onEvalDelay.value = delay
        }

        override fun setDebugMode(enabled: Boolean) {
            debugMode.value = enabled
        }

        override fun setDebugRunningMode(mode: DebugRunningMode) {
            debugRunningMode.value = mode
        }
    }

    private class MockFileRepository : FileRepository {
        override val selectedEntryPath: StateFlow<EntryPath?> =
            MutableStateFlow(null)

        var mockGetEntryByPath: (EntryPath) -> Entry? =
            { null }
        var mockGetFileContent: (ProgramFile) -> FileContent = { FileContent("") }

        override suspend fun getRootFolder() = throw NotImplementedError()

        override suspend fun getEntryByPath(entryPath: EntryPath) =
            mockGetEntryByPath(entryPath)

        override suspend fun saveFile(
            programFile: ProgramFile,
            fileContent: FileContent,
            cursorPosition: CursorPosition
        ) {
        }

        override suspend fun createFolder(path: EntryPath) {}
        override suspend fun selectFile(entryPath: EntryPath) {}
        override suspend fun getFileContent(programFile: ProgramFile): FileContent =
            mockGetFileContent(programFile)

        override suspend fun getCursorPosition(programFile: ProgramFile): CursorPosition =
            CursorPosition(0)
    }
}
