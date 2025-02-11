package io.github.arashiyama11.dncl

import io.github.arashiyama11.dncl.lexer.Lexer
import io.github.arashiyama11.dncl.model.AstNode
import io.github.arashiyama11.dncl.parser.Parser
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.fail


class ParserTest {
    @Test
    fun test() {
        val input = """Akibi = [5, 3, 4]
buinsu = 3
tantou = 1
buin を 2 から buinsu まで 1 ずつ増やしながら繰り返す:
  もし Akibi[buin] < Akibi[tantou] ならば:
    tantou = buin
表示する("次の工芸品の担当は部員", tantou, "です")"""
        val parser = Parser(Lexer(input)).getOrNull()!!
        val prog = parser.parseProgram()
        println(prog.getOrNull()?.literal)
        println(prog.leftOrNull()?.explain(input))
        for (stmt in prog.getOrNull()!!.statements) {
            println(stmt::class.java)
            if (stmt is AstNode.ExpressionStatement) {
                if (stmt.expression is AstNode.WhileExpression) {
                    println(stmt.expression.literal)
                }
            }
        }
    }

    @Test
    fun testExpression() {
        val input = """-a+b
!-a
a + b + c
a + b - c
a * b * c
a * b / c
a + b / c
a + b * c
a * 3 + c
a + b * c + d / e - f
3 + 4
5 + 5
4 / 2 * 1
5 // 2 + 3
5 ÷ 2 + 3 * 4
5 > 4 == 3 < 4
5 < 4 != 3 > 4
3 + 4 * 5 == 3 * 1 + 4 * 5
"""
        val parser = Parser(Lexer(input)).getOrNull()!!
        val prog = parser.parseProgram()
        if (prog.isLeft()) fail(prog.leftOrNull()?.message)
        println(prog.getOrNull()!!.literal)
        assertEquals(
            """((-a) + b)
(!(-a))
((a + b) + c)
((a + b) - c)
((a * b) * c)
((a * b) / c)
(a + (b / c))
(a + (b * c))
((a * 3) + c)
(((a + (b * c)) + (d / e)) - f)
(3 + 4)
(5 + 5)
((4 / 2) * 1)
((5 // 2) + 3)
((5 // 2) + (3 * 4))
((5 > 4) == (3 < 4))
((5 < 4) ≠ (3 > 4))
((3 + (4 * 5)) == ((3 * 1) + (4 * 5)))""", prog.getOrNull()!!.literal
        )
    }

    @Test//TODO: add more test cases
    fun testAll() {
        for ((i, program) in TestCase.all.withIndex()) {
            val parser = Parser(Lexer(program)).getOrNull()!!
            val prog = parser.parseProgram()
            if (prog.isLeft()) fail("fail test case $i ${prog.leftOrNull()?.message}")
            println(prog.getOrNull()!!.literal)

        }
    }
}