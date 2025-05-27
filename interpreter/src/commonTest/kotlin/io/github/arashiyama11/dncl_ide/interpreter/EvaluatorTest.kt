package io.github.arashiyama11.dncl_ide.interpreter

import io.github.arashiyama11.dncl_ide.interpreter.lexer.Lexer
import io.github.arashiyama11.dncl_ide.interpreter.evaluator.Evaluator
import io.github.arashiyama11.dncl_ide.interpreter.evaluator.EvaluatorFactory
import io.github.arashiyama11.dncl_ide.interpreter.model.AstNode
import io.github.arashiyama11.dncl_ide.interpreter.model.DnclObject
import io.github.arashiyama11.dncl_ide.interpreter.model.Environment
import io.github.arashiyama11.dncl_ide.interpreter.parser.Parser
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.channels.ReceiveChannel
import kotlinx.coroutines.runBlocking
import kotlin.math.max
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.fail

class EvaluatorTest {
    private val nullObj = DnclObject.Null(AstNode.SystemLiteral("", 0..0))
    private var stdin: DnclObject = nullObj
    private var stdout = ""
    private lateinit var evaluator: Evaluator
    private lateinit var evaluator0Origin: Evaluator
    private val builtInEnv = runBlocking {
        EvaluatorFactory.createBuiltInFunctionEnvironment(
            onStdout = {
                stdout += "$it\n"
            },
            onClear = {
                stdout = ""
            },
            onImport = { DnclObject.Null(astNode) },
        )
    }

    @BeforeTest
    fun setUp() {
        stdin = nullObj
        stdout = ""
        val inputChannel = Channel<String>(capacity = 1)
        inputChannel.trySend("62")
        evaluator =
            EvaluatorFactory.create(inputChannel, 1) { astNode, _ -> DnclObject.Null(astNode) }

        val inputChannel0 = Channel<String>(capacity = 1)
        inputChannel0.trySend("62")
        evaluator0Origin =
            EvaluatorFactory.create(inputChannel0, 0) { astNode, _ -> DnclObject.Null(astNode) }
    }

    @Test
    fun test() {
        var program = """
関数 forEach(array,f) を:
  i を 0 から 要素数(array)-1 まで 1 ずつ増やしながら繰り返す:
    f(array[i])
と定義する

関数 tail(array) を:
  Result = []
  i を 1 から 要素数(array)-1 まで 1 ずつ増やしながら繰り返す:
    末尾追加(Result,array[i])
  戻り値(Result)
と定義する

関数 quick_sort(array) を:
  もし 要素数(array) <= 1 ならば:
    戻り値(array)
  pivot = array[0]
  Left = []
  Right = []
  forEach(tail(array),関数 (x) を:
    もし x < pivot ならば:
      末尾追加(Left,x)
    そうでなければ:
      末尾追加(Right,x)
  と定義する)
  Right2 = quick_sort(Right)
  Left2 = quick_sort(Left)
  戻り値(Left2 + [pivot] + Right2)
と定義する

Data = [65, 18, 29, 48, 52, 3, 62, 77, 89, 9, 7, 33]
D = quick_sort(Data)
表示する(D)
"""
        val env = Environment(builtInEnv)
        val a = runBlocking {
            evaluator0Origin.evalProgram(
                program.toProgram(),
                env
            )
        }

        a.getOrNull()!!.let {
            if (it is DnclObject.Error) println(explain(program, it))
        }
        println(a)
        assertEquals("[3, 7, 9, 18, 29, 33, 48, 52, 62, 65, 77, 89]\n", stdout)
    }


    fun testEval(evaluator: Evaluator, program: String, expected: String) {
        stdout = ""
        runBlocking {
            evaluator.evalProgram(program.toProgram(), Environment(builtInEnv)).leftOrNull()
                ?.let { fail(it.toString()) }
        }
        assertEquals(expected, stdout)
    }

    @Test
    fun testexam2025_0() {
        testEval(evaluator, TestCase.exam2025_0, "次の工芸品の担当は部員 2 です\n")
    }

    @Test
    fun testexam2025_1() {
        testEval(
            evaluator, TestCase.exam2025_1, """工芸品 1  …  部員 1 ： 1 日目～ 4 日目
工芸品 2  …  部員 2 ： 1 日目～ 1 日目
工芸品 3  …  部員 3 ： 1 日目～ 3 日目
工芸品 4  …  部員 2 ： 2 日目～ 2 日目
工芸品 5  …  部員 2 ： 3 日目～ 5 日目
工芸品 6  …  部員 3 ： 4 日目～ 7 日目
工芸品 7  …  部員 1 ： 5 日目～ 6 日目
工芸品 8  …  部員 2 ： 6 日目～ 9 日目
工芸品 9  …  部員 1 ： 7 日目～ 9 日目
"""
        )
    }

    @Test
    fun testSisaku2022() {
        testEval(
            evaluator0Origin,
            TestCase.Sisaku2022,
            "[0, 3, 3, 0, 1, 1, 0, 0, 1, 0, 3, 0, 1, 1, 4, 1, 1, 0, 0, 0, 0, 0, 0, 2, 3, 0]\n"
        )
    }

    @Test
    fun testSisaku2022_0() {

        testEval(evaluator0Origin, TestCase.Sisaku2022_0, "6\n")
    }

    @Test
    fun testSisaku2022_1() {
        val env = Environment(builtInEnv)
        runBlocking {
            evaluator0Origin.evalProgram(TestCase.MaisuFunction.toProgram(), env).leftOrNull()
                ?.let { fail(it.toString()) }

            evaluator0Origin.evalProgram(TestCase.Sisaku2022_1.toProgram(), env).leftOrNull()
                ?.let { fail(it.toString()) }
        }

        assertEquals("3\n", stdout)
    }

    @Test
    fun testSisaku2022_2() {
        stdin = DnclObject.Int(62, nullObj.astNode)
        testEval(
            evaluator0Origin, TestCase.Sisaku2022_2, """0～99の数字を入力してください
62 は 6 番目にありました
添字   要素
0   3
1   18
2   29
3   33
4   48
5   52
6   62
7   77
8   89
9   97
"""
        )
    }

    @Test
    fun testBooleanLiteral() {
        // 日本語リテラル
        testEval(evaluator, "表示する(真)", "真\n")
        testEval(evaluator, "表示する(偽)", "偽\n")
        // 英語リテラル
        testEval(evaluator, "表示する(true)", "真\n")
        testEval(evaluator, "表示する(false)", "偽\n")

        // 変数への代入
        testEval(evaluator, "a = 真\n表示する(a)", "真\n")
        testEval(evaluator, "b = 偽\n表示する(b)", "偽\n")
        testEval(evaluator, "c = true\n表示する(c)", "真\n")
        testEval(evaluator, "d = false\n表示する(d)", "偽\n")

        // 配列に含める
        testEval(
            evaluator,
            "arr = [真, 偽, true, false]\n表示する(arr)",
            "[真, 偽, 真, 偽]\n"
        )

        // 比較演算の結果
        testEval(evaluator, "表示する(1 == 1)", "真\n")
        testEval(evaluator, "表示する(1 == 2)", "偽\n")
        testEval(evaluator, "表示する(真 == true)", "真\n")
        testEval(evaluator, "表示する(偽 == false)", "真\n")
        testEval(evaluator, "表示する(真 == 偽)", "偽\n")
        testEval(evaluator, "表示する(true != false)", "真\n")

        // 関数引数として渡す
        testEval(evaluator, "関数 f(x) を:\n 表示する(x) \nと定義する\nf(真)", "真\n")
        testEval(evaluator, "関数 f(x) を:\n 表示する(x) \nと定義する\nf(偽)", "偽\n")
        testEval(evaluator, "関数 f(x) を:\n 表示する(x) \nと定義する\nf(true)", "真\n")
        testEval(evaluator, "関数 f(x) を:\n 表示する(x) \nと定義する\nf(false)", "偽\n")

        // if文の条件
        testEval(evaluator, "もし 真 ならば:\n 表示する(1) \nそうでなければ:\n 表示する(0)", "1\n")
        testEval(evaluator, "もし 偽 ならば:\n 表示する(1) \nそうでなければ:\n 表示する(0)", "0\n")
        testEval(
            evaluator,
            "もし true ならば:\n 表示する(1) \nそうでなければ:\n 表示する(0)",
            "1\n"
        )
        testEval(
            evaluator,
            "もし false ならば:\n 表示する(1) \nそうでなければ:\n 表示する(0)",
            "0\n"
        )
    }

    @Test
    fun testMultipleInputs() {
        val inputChannel = Channel<String>(capacity = 2)
        inputChannel.trySend("foo")
        inputChannel.trySend("bar")
        val evaluator = EvaluatorFactory.create(inputChannel, 0) { _, _ -> }
        val program = """
a = 【外部からの入力】
b = 【外部からの入力】
表示する(a)
表示する(b)
"""
        val env = Environment(builtInEnv)
        stdout = ""
        runBlocking {
            evaluator.evalProgram(program.toProgram(), env)
        }
        assertEquals("foo\nbar\n", stdout)
    }

    @Test
    fun testInputBranching() {
        val inputChannel = Channel<String>(capacity = 1)
        inputChannel.trySend("yes")
        val evaluator = EvaluatorFactory.create(inputChannel, 0) { _, _ -> }
        val program = """
a = 【外部からの入力】
もし a == "yes" ならば:
  表示する("OK")
そうでなければ:
  表示する("NG")
"""
        val env = Environment(builtInEnv)
        stdout = ""
        runBlocking {
            evaluator.evalProgram(program.toProgram(), env)
        }
        assertEquals("OK\n", stdout)
    }

    @Test
    fun testInputEmptyString() {
        val inputChannel = Channel<String>(capacity = 1)
        inputChannel.trySend("")
        val evaluator = EvaluatorFactory.create(inputChannel, 0) { _, _ -> }
        val program = """
a = 【外部からの入力】
表示する(a)
"""
        val env = Environment(builtInEnv)
        stdout = ""
        runBlocking {
            evaluator.evalProgram(program.toProgram(), env)
        }
        assertEquals("\n", stdout)
    }

    @Test
    fun testInputSpecialCharacters() {
        val inputChannel = Channel<String>(capacity = 1)
        inputChannel.trySend("あいうえお!@#\$%")
        val evaluator = EvaluatorFactory.create(inputChannel, 0) { _, _ -> }
        val program = """
a = 【外部からの入力】
表示する(a)
"""
        val env = Environment(builtInEnv)
        stdout = ""
        runBlocking {
            evaluator.evalProgram(program.toProgram(), env)
        }
        assertEquals("あいうえお!@#\$%\n", stdout)
    }

    @Test
    fun testInputSum() {
        val inputChannel = Channel<String>(capacity = 2)
        inputChannel.trySend("3")
        inputChannel.trySend("5")
        val evaluator = EvaluatorFactory.create(inputChannel, 0) { _, _ -> }
        val program = """
a = 【外部からの入力】
b = 【外部からの入力】
c = 整数変換(a) + 整数変換(b)
表示する(c)
"""
        val env = Environment(builtInEnv)
        stdout = ""
        runBlocking {
            evaluator.evalProgram(program.toProgram(), env)
        }
        assertEquals("8\n", stdout)
    }

    private fun String.toProgram() = Parser(Lexer(this)).getOrNull()!!.parseProgram().getOrNull()!!

    fun explain(program: String, error: DnclObject.Error): String {
        val programLines = program.split("\n")
        error.astNode.range
        val (column, line, spaces) = run {
            var index = 0
            for ((l, str) in programLines.withIndex()) {
                if (index + str.length < error.astNode.range.first) {
                    index += str.length + 1
                } else {
                    val col = error.astNode.range.first - index
                    val sp = str.substring(0, col)
                        .fold(0) { acc, c -> acc + if (isHalfWidth(c)) 1 else 2 }
                    return@run Triple(col, l, sp)
                }
            }
            return@run Triple(0, 0, 0)
        }

        return """line: $line, column: $column
${error.message}
${programLines.subList(max(0, line - 5), line + 1).joinToString("\n")}
${" ".repeat(spaces)}${"^".repeat(max(1, error.astNode.range.last - error.astNode.range.first))}"""
    }


    private fun isHalfWidth(char: Char): Boolean {
        val code = char.code
        return (code in 0x0020..0x007E) || (code in 0xFF61..0xFF9F)
    }
}
