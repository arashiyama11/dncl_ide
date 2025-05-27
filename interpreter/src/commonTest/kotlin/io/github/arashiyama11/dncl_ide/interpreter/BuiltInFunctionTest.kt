package io.github.arashiyama11.dncl_ide.interpreter

import io.github.arashiyama11.dncl_ide.interpreter.lexer.Lexer
import io.github.arashiyama11.dncl_ide.interpreter.evaluator.Evaluator
import io.github.arashiyama11.dncl_ide.interpreter.evaluator.EvaluatorFactory
import io.github.arashiyama11.dncl_ide.interpreter.model.DnclObject
import io.github.arashiyama11.dncl_ide.interpreter.model.Environment
import io.github.arashiyama11.dncl_ide.interpreter.parser.Parser
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.channels.ReceiveChannel
import kotlinx.coroutines.runBlocking
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue
import kotlin.test.fail
import kotlin.time.Duration
import kotlin.time.TimeSource

class BuiltInFunctionTest {
    private var stdout = ""
    private var clearCalled = false
    private lateinit var evaluator: Evaluator

    private val builtInEnv = runBlocking {
        EvaluatorFactory.createBuiltInFunctionEnvironment(
            onStdout = {
                stdout += "$it\n"
            },
            onClear = {
                clearCalled = true
            },
            onImport = { DnclObject.Null(astNode) },
        )
    }

    @BeforeTest
    fun setUp() {
        stdout = ""
        clearCalled = false
        val inputChannel = Channel<String>(capacity = 1)
        inputChannel.trySend("")
        evaluator =
            EvaluatorFactory.create(
                inputChannel = inputChannel,
                arrayOrigin = 0,
            ) { astNode, _ ->
                DnclObject.Null(astNode)
            }
    }

    private fun String.toProgram() = Parser(Lexer(this)).getOrNull()!!.parseProgram().getOrNull()!!

    private fun testEval(program: String, expected: String) {
        runBlocking {
            evaluator.evalProgram(program.toProgram(), Environment(builtInEnv)).leftOrNull()
                ?.let { fail(it.toString()) }
        }
        assertEquals(expected, stdout)
    }

    private fun evalAndGetResult(program: String): DnclObject {
        return runBlocking {
            val result = evaluator.evalProgram(program.toProgram(), Environment(builtInEnv))
            result.leftOrNull()?.let { fail(it.toString()) }
            result.getOrNull() ?: fail("Evaluation failed")
        }
    }

    // Tests for original built-in functions
    @Test
    fun testPrint() {
        testEval("表示する(\"Hello, World!\")", "Hello, World!\n")
    }

    @Test
    fun testLength() {
        val result = evalAndGetResult("要素数([1, 2, 3, 4, 5])")
        assertTrue(result is DnclObject.Int)
        assertEquals(5, result.value)
    }

    @Test
    fun testDiff() {
        val result = evalAndGetResult("差分(\"a\")")
        assertTrue(result is DnclObject.Int)
        assertEquals(0, result.value) // 'a' - 'a' = 0
    }

    @Test
    fun testConcat() {
        val result = evalAndGetResult("連結([1, 2], [3, 4])")
        assertTrue(result is DnclObject.Array)
        assertEquals(4, result.value.size)
        assertEquals("1", result.value[0].toString())
        assertEquals("2", result.value[1].toString())
        assertEquals("3", result.value[2].toString())
        assertEquals("4", result.value[3].toString())
    }

    @Test
    fun testPush() {
        testEval(
            """
            arr = [1, 2, 3]
            末尾追加(arr, 4)
            表示する(arr)
        """.trimIndent(), "[1, 2, 3, 4]\n"
        )
    }

    @Test
    fun testPop() {
        testEval(
            """
            arr = [1, 2, 3]
            val = 末尾削除(arr)
            表示する(arr)
            表示する(val)
        """.trimIndent(), "[1, 2]\n3\n"
        )
    }

    @Test
    fun testShift() {
        testEval(
            """
            arr = [1, 2, 3]
            val = 先頭削除(arr)
            表示する(arr)
            表示する(val)
        """.trimIndent(), "[2, 3]\n1\n"
        )
    }

    @Test
    fun testUnshift() {
        testEval(
            """
            arr = [1, 2, 3]
            先頭追加(arr, 0)
            表示する(arr)
        """.trimIndent(), "[0, 1, 2, 3]\n"
        )
    }

    @Test
    fun testInt() {
        val result = evalAndGetResult("整数変換(\"123\")")
        assertTrue(result is DnclObject.Int)
        assertEquals(123, result.value)
    }

    @Test
    fun testFloat() {
        val result = evalAndGetResult("浮動小数点変換(\"123.45\")")
        assertTrue(result is DnclObject.Float)
        assertEquals(123.45f, result.value)
    }

    @Test
    fun testString() {
        val result = evalAndGetResult("文字列変換(123)")
        assertTrue(result is DnclObject.String)
        assertEquals("123", result.value)
    }

    @Test
    fun testCharCode() {
        val result = evalAndGetResult("文字コード(\"A\")")
        assertTrue(result is DnclObject.Int)
        assertEquals(65, result.value)
    }

    @Test
    fun testFromCharCode() {
        val result = evalAndGetResult("コードから文字(65)")
        assertTrue(result is DnclObject.String)
        assertEquals("A", result.value)
    }

    // Tests for new built-in functions
    // Array operation functions
    @Test
    fun testSlice() {
        val result = evalAndGetResult("部分配列([1, 2, 3, 4, 5], 1, 4)")
        assertTrue(result is DnclObject.Array)
        assertEquals(3, result.value.size)
        assertEquals("2", result.value[0].toString())
        assertEquals("3", result.value[1].toString())
        assertEquals("4", result.value[2].toString())
    }

    @Test
    fun testJoin() {
        val result = evalAndGetResult("配列結合([\"Hello\", \"World\"], \", \")")
        assertTrue(result is DnclObject.String)
        assertEquals("Hello, World", result.value)
    }

    @Test
    fun testSort() {
        val result = evalAndGetResult("並べ替え([3, 1, 4, 1, 5, 9, 2, 6, 5])")
        assertTrue(result is DnclObject.Array)
        assertEquals(9, result.value.size)
        assertEquals("1", result.value[0].toString())
        assertEquals("1", result.value[1].toString())
        assertEquals("2", result.value[2].toString())
        assertEquals("3", result.value[3].toString())
        assertEquals("4", result.value[4].toString())
        assertEquals("5", result.value[5].toString())
        assertEquals("5", result.value[6].toString())
        assertEquals("6", result.value[7].toString())
        assertEquals("9", result.value[8].toString())
    }

    @Test
    fun testReverse() {
        val result = evalAndGetResult("逆順([1, 2, 3, 4, 5])")
        assertTrue(result is DnclObject.Array)
        assertEquals(5, result.value.size)
        assertEquals("5", result.value[0].toString())
        assertEquals("4", result.value[1].toString())
        assertEquals("3", result.value[2].toString())
        assertEquals("2", result.value[3].toString())
        assertEquals("1", result.value[4].toString())
    }

    @Test
    fun testFind() {
        val result = evalAndGetResult("検索([\"apple\", \"banana\", \"cherry\"], \"banana\")")
        assertTrue(result is DnclObject.Int)
        assertEquals(1, result.value)
    }

    // String operation functions
    @Test
    fun testSubstring() {
        val result = evalAndGetResult("部分文字列(\"Hello, World!\", 7, 12)")
        assertTrue(result is DnclObject.String)
        assertEquals("World", result.value)
    }

    @Test
    fun testSplit() {
        val result = evalAndGetResult("分割(\"apple,banana,cherry\", \",\")")
        assertTrue(result is DnclObject.Array)
        assertEquals(3, result.value.size)
        assertEquals("apple", (result.value[0] as DnclObject.String).value)
        assertEquals("banana", (result.value[1] as DnclObject.String).value)
        assertEquals("cherry", (result.value[2] as DnclObject.String).value)
    }

    @Test
    fun testTrim() {
        val result = evalAndGetResult("空白除去(\"  Hello, World!  \")")
        assertTrue(result is DnclObject.String)
        assertEquals("Hello, World!", result.value)
    }

    @Test
    fun testReplace() {
        val result = evalAndGetResult("置換(\"Hello, World!\", \"World\", \"DNCL\")")
        assertTrue(result is DnclObject.String)
        assertEquals("Hello, DNCL!", result.value)
    }

    // Math functions
    @Test
    fun testRound() {
        val result = evalAndGetResult("四捨五入(3.7)")
        assertTrue(result is DnclObject.Int)
        assertEquals(4, result.value)
    }

    @Test
    fun testFloor() {
        val result = evalAndGetResult("切り捨て(3.7)")
        assertTrue(result is DnclObject.Int)
        assertEquals(3, result.value)
    }

    @Test
    fun testCeil() {
        val result = evalAndGetResult("切り上げ(3.2)")
        assertTrue(result is DnclObject.Int)
        assertEquals(4, result.value)
    }

    @Test
    fun testRandom() {
        val result = evalAndGetResult("乱数()")
        assertTrue(result is DnclObject.Float)
        assertTrue(result.value >= 0f && result.value < 1f)
    }

    @Test
    fun testMax() {
        val result = evalAndGetResult("最大値(3, 7, 2, 5)")
        assertTrue(result is DnclObject.Int)
        assertEquals(7, result.value)
    }

    @Test
    fun testMin() {
        val result = evalAndGetResult("最小値(3, 7, 2, 5)")
        assertTrue(result is DnclObject.Int)
        assertEquals(2, result.value)
    }

    // Type checking functions
    @Test
    fun testIsInt() {
        val result = evalAndGetResult("整数判定(42)")
        assertTrue(result is DnclObject.Boolean)
        assertEquals(true, result.value)
    }

    @Test
    fun testIsFloat() {
        val result = evalAndGetResult("浮動小数点判定(3.14)")
        assertTrue(result is DnclObject.Boolean)
        assertEquals(true, result.value)
    }

    @Test
    fun testIsString() {
        val result = evalAndGetResult("文字列判定(\"Hello\")")
        assertTrue(result is DnclObject.Boolean)
        assertEquals(true, result.value)
    }

    @Test
    fun testIsArray() {
        val result = evalAndGetResult("配列判定([1, 2, 3])")
        assertTrue(result is DnclObject.Boolean)
        assertEquals(true, result.value)
    }

    @Test
    fun testIsBoolean() {
        val result = evalAndGetResult("真偽値判定(1 == 1)")
        assertTrue(result is DnclObject.Boolean)
        assertEquals(true, result.value)
    }

    // System functions
    @Test
    fun testClear() {
        val result = evalAndGetResult("出力消去()")
        assertTrue(result is DnclObject.Null)
        assertTrue(clearCalled, "onClear function should be called")
    }

    @Test
    fun testSleep() {
        val timeSource = TimeSource.Monotonic
        val start = timeSource.markNow()

        val result = evalAndGetResult("待機(100)")

        val elapsed = start.elapsedNow()
        assertTrue(result is DnclObject.Null)
        assertTrue(
            elapsed >= Duration.parse("100ms"),
            "Sleep should pause execution for at least 100ms"
        )
    }
}
