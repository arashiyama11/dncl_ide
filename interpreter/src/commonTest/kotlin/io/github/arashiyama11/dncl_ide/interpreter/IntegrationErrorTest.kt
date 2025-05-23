package io.github.arashiyama11.dncl_ide.interpreter

import io.github.arashiyama11.dncl_ide.interpreter.evaluator.EvaluatorFactory
import io.github.arashiyama11.dncl_ide.interpreter.lexer.Lexer
import io.github.arashiyama11.dncl_ide.interpreter.model.DnclObject
import io.github.arashiyama11.dncl_ide.interpreter.parser.Parser
import io.github.arashiyama11.dncl_ide.interpreter.model.explain
import kotlinx.coroutines.runBlocking
import kotlin.test.*

class IntegrationErrorTest {
    private fun runTest(program: String, expectedOutput: String) {
        // Try to create Parser instance using the companion object invoke operator
        val output = run {
            val lexer = Lexer(program)

            val parser = Parser(lexer).fold(ifLeft = { return@run it.explain(program) }) { it }
            val ast =
                parser.parseProgram().fold(ifLeft = { return@run it.explain(program) }) { it }

            val evaluator = EvaluatorFactory.create("", 0)
            runBlocking {
                evaluator.evalProgram(ast)
            }.fold(ifLeft = { return@run it.explain(program) }) {
                if (it is DnclObject.Error) {
                    return@run it.explain(program)
                }
                Unit
            }
            fail("Expected to fail, but succeeded")
        }
        assertEquals(expectedOutput, output)
    }

    @Test
    fun testInvalidCharacterError() {
        runTest(
            """a;b""",
            """1行2文字目でエラーが発生しました
「;」は無効な文字です
===============
1| a;b
    ^"""
        )
    }

    @Test
    fun testUnclosedStringError() {
        runTest(
            """
1+1
"これは閉じない文字列
""", """3行1文字目でエラーが発生しました
文字列が閉じていません
===============
1| 
2| 1+1
3| "これは閉じない文字列
   ^"""
        )
    }

    @Test
    fun testMultipleExpressionsInOneLineError() {
        //runTest("""""", """""")
        runTest(
            """let a = 1""", """1行1文字目でエラーが発生しました
一行に複数の式を書けません
===============
1| let a = 1
   ^^^"""
        )
    }

    @Test
    fun testUnclosedParenthesisError() {
        runTest(
            """

1 + ( 2 + 3 """, """3行5文字目でエラーが発生しました
カッコが閉じていません
===============
1| 
2| 
3| 1 + ( 2 + 3 
       ^"""
        )
    }

    @Test
    fun testUnexpectedEndOfExpressionInArrayError() {
        runTest(
            """[1,2,""", """1行6文字目でエラーが発生しました
期待せず式が終了しました
===============
1| [1,2,
        ^"""
        )
    }

    @Test
    fun testUnexpectedTokenInIfConditionError() {
        runTest(
            """もし x が 1""", """1行6文字目でエラーが発生しました
予期しないトークン: が
期待されるトークン: ならば
===============
1| もし x が 1
          ^^"""
        )
    }

    @Test
    fun testArrayIndexOutOfBoundsError() {
        runTest(
            """[1,2,3][123]""", """1行9文字目でエラーが発生しました
配列の範囲外アクセスがされました。
配列の長さ:3 
インデックス:123
===============
1| [1,2,3][123]
           ^^^"""
        )
    }

    @Test
    fun testUndefinedVariableError() {
        runTest(
            "\n\na", """3行1文字目でエラーが発生しました
変数「a」は定義されていません
===============
1| 
2| 
3| a
   ^"""
        )
    }

    @Test
    fun testTypeError() {
        runTest(
            """1 + [] """, """1行1文字目でエラーが発生しました
演算子「+」は整数、小数、文字列、配列の同じ型同士の演算のみ可能です。
Int + Array が実行されようとしました
===============
1| 1 + [] 
   ^^^^^^"""
        )
    }
} 