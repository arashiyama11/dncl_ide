package io.github.arashiyama11.dncl

import io.github.arashiyama11.dncl.evaluator.Evaluator
import io.github.arashiyama11.dncl.lexer.Lexer
import io.github.arashiyama11.dncl.model.BuiltInFunction
import io.github.arashiyama11.dncl.model.DnclObject
import io.github.arashiyama11.dncl.model.SystemCommand
import io.github.arashiyama11.dncl.parser.Parser
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertEquals

class EvaluatorTest {
    private var stdin: DnclObject = DnclObject.Null
    private var stdout = ""
    private var evaluator: Evaluator = Evaluator(
        { fn, arg ->
            when (fn) {
                BuiltInFunction.PRINT -> {
                    arg.joinToString(" ") { it.toString() }.also { stdout += it }
                    stdout += "\n"
                    DnclObject.Null
                }

                BuiltInFunction.LENGTH -> {
                    require(arg.size == 1)
                    when (arg[0]) {
                        is DnclObject.Array -> DnclObject.Int((arg[0] as DnclObject.Array).value.size)
                        else -> DnclObject.TypeError("")
                    }
                }

                BuiltInFunction.DIFF -> {
                    require(arg.size == 1)
                    when (arg[0]) {
                        is DnclObject.String -> {
                            val str = (arg[0] as DnclObject.String).value
                            require(str.length == 1)
                            if (str == " ") DnclObject.Int(-1) else DnclObject.Int(str[0].code - 'a'.code)
                        }

                        else -> DnclObject.TypeError("")
                    }
                }
            }
        },
        {
            when (it) {
                SystemCommand.Input -> {
                    stdin
                }

                is SystemCommand.Unknown -> {
                    DnclObject.Null
                }
            }
        }, 1
    )

    val evaluator0Origin = Evaluator(
        { fn, arg ->
            when (fn) {
                BuiltInFunction.PRINT -> {
                    arg.joinToString(", ") { it.toString() }.also { stdout += it }
                    stdout += "\n"
                    DnclObject.Null
                }

                BuiltInFunction.LENGTH -> {
                    require(arg.size == 1)
                    when (arg[0]) {
                        is DnclObject.Array -> DnclObject.Int((arg[0] as DnclObject.Array).value.size)
                        else -> DnclObject.TypeError("")
                    }
                }

                BuiltInFunction.DIFF -> {
                    require(arg.size == 1)
                    when (arg[0]) {
                        is DnclObject.String -> {
                            val str = (arg[0] as DnclObject.String).value
                            require(str.length == 1)
                            if (str == " ") DnclObject.Int(-1) else DnclObject.Int(str[0].code - 'a'.code)
                        }

                        else -> DnclObject.TypeError("")
                    }
                }
            }
        },
        {
            when (it) {
                SystemCommand.Input -> {
                    stdin
                }

                is SystemCommand.Unknown -> {
                    DnclObject.Null
                }
            }
        }, 0
    )

    @BeforeTest
    fun setUp() {
        stdin = DnclObject.Null
        stdout = ""
    }

    fun testEval(evaluator: Evaluator, program: String, expected: String) {
        evaluator.evalProgram(program.toProgram())
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

    //TODO
    @Test
    fun testSisaku2022_1() {
        //testEval(evaluator0Origin, TestCase.Sisaku2022_1, "2\n")
    }

    @Test
    fun testSisaku2022_2() {
        stdin = DnclObject.Int(62)
        testEval(
            evaluator0Origin, TestCase.Sisaku2022_2, """0～99の数字を入力してください
62, は, 6, 番目にありました
添字,  , 要素
0,  , 3
1,  , 18
2,  , 29
3,  , 33
4,  , 48
5,  , 52
6,  , 62
7,  , 77
8,  , 89
9,  , 97
"""
        )
    }

    private fun String.toProgram() = Parser(Lexer(this)).getOrNull()!!.parseProgram().getOrNull()!!
}
