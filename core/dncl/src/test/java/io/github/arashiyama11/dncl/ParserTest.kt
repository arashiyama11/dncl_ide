package io.github.arashiyama11.dncl

import io.github.arashiyama11.dncl.lexer.Lexer
import io.github.arashiyama11.dncl.parser.Parser
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.fail


class ParserTest {

    @Test
    fun test() {
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
}