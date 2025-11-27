package io.github.arashiyama11.dncl_ide.interpreter

import io.github.arashiyama11.dncl_ide.interpreter.lexer.Lexer
import io.github.arashiyama11.dncl_ide.interpreter.parser.Parser
import io.github.arashiyama11.dncl_ide.interpreter.preprocessor.preProcess
import kotlinx.coroutines.flow.toList
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.fail


class ParserTest {
    @Test
    fun test() = runTest {
        val input = """
もし Akibi[buin] < Akibi[tantou] ならば:
    tantou = buin
表示する("次の工芸品の担当は部員", tantou, "です")"""
        println(
            Parser(preProcess(Lexer(input), resolveLib = { "" }).toList()).getOrNull()!!
                .parseProgram()
        )
    }

    @Test
    fun testExpression() = runTest {
        val input = """
-a+b
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
        val parser = Parser(preProcess(Lexer(input), resolveLib = { "" }).toList()).getOrNull()!!
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

    @Test
    fun testFunction() = runTest {
        val input = TestCase.MaisuFunction
        val parser = Parser(preProcess(Lexer(input), resolveLib = { "" }).toList()).getOrNull()!!
        val prog = parser.parseProgram()
        if (prog.isLeft()) fail(prog.leftOrNull()?.explain(input))
        assertEquals(
            """function 枚数(kingaku) {
Kouka = [1, 5, 10, 50, 100]
maisu = 0, nokori = kingaku
for i in 4..0 DECREMENT by IntLiteral(value=1, range=91..91, filePath=null) {
maisu = (maisu + (nokori // Kouka[i]))
nokori = (nokori % Kouka[i])
}
戻り値(maisu)
}""", prog.getOrNull()!!.literal
        )
    }

    @Test
    fun testexam2025_0() {
        testParser(
            TestCase.exam2025_0, """Akibi = [5, 3, 4]
buinsu = 3
tantou = 1
for buin in 2..buinsu INCREMENT by IntLiteral(value=1, range=62..62, filePath=null) {
if (Akibi[buin] < Akibi[tantou]) {
tantou = buin
}
}
表示する("次の工芸品の担当は部員", tantou, "です")"""
        )
    }

    @Test
    fun testexam2025_1() {
        testParser(
            TestCase.exam2025_1, """Nissu = [4, 1, 3, 1, 3, 4, 2, 4, 3]
kougeihinsu = 9
Akibi = [1, 1, 1]
buinsu = 3
for kougeihin in 1..kougeihinsu INCREMENT by IntLiteral(value=1, range=113..113, filePath=null) {
tantou = 1
for buin in 2..buinsu INCREMENT by IntLiteral(value=1, range=166..166, filePath=null) {
if (Akibi[buin] < Akibi[tantou]) {
tantou = buin
}
}
表示する("工芸品", kougeihin, " … ", "部員", tantou, "：", Akibi[tantou], "日目～", ((Akibi[tantou] + Nissu[kougeihin]) - 1), "日目")
Akibi[tantou] = (Akibi[tantou] + Nissu[kougeihin])
}"""
        )
    }

    @Test
    fun testSisaku2022() {
        testParser(
            TestCase.Sisaku2022,
            """Angoubun = ["p", "y", "e", "b", " ", "c", "m", "y", "b", "o", " ", "k", "x", "n", " ", "c", "o", "f", "o", "x", " ", "i", "o", "k", "b", "c", " ", "k", "q", "y", " "]
Hindo = [0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0]
for i in 0..(要素数(Angoubun) - 1) INCREMENT by IntLiteral(value=1, range=266..266, filePath=null) {
bangou = 差分(Angoubun[i])
if (bangou ≠ (-1)) {
Hindo[bangou] = (Hindo[bangou] + 1)
}
}
表示する(Hindo)"""
        )
    }

    @Test
    fun testSisaku2022_0() {
        testParser(
            TestCase.Sisaku2022_0, """Kouka = [1, 5, 10, 50, 100]
kingaku = 46
maisu = 0, nokori = kingaku
for i in 4..0 DECREMENT by IntLiteral(value=1, range=79..79, filePath=null) {
maisu = (maisu + (nokori // Kouka[i]))
nokori = (nokori % Kouka[i])
}
表示する(maisu)"""
        )
    }

    @Test
    fun testSisaku2022_1() {
        testParser(
            TestCase.Sisaku2022_1, """kakaku = 46
min_maisu = 100
for tsuri in 0..99 INCREMENT by IntLiteral(value=1, range=47..47, filePath=null) {
shiharai = (kakaku + tsuri)
maisu = (枚数(shiharai) + 枚数(tsuri))
if (maisu < min_maisu) {
min_maisu = maisu
}
}
表示する(min_maisu)"""
        )
    }

    @Test
    fun testSisaku2022_2() {
        testParser(
            TestCase.Sisaku2022_2, """Data = [3, 18, 29, 33, 48, 52, 62, 77, 89, 97]
kazu = 要素数(Data)
表示する("0～99の数字を入力してください")
atai = 整数変換(外部からの入力)
hidari = 0, migi = (kazu - 1)
owari = 0
while ((hidari ≦ migi) AND (owari == 0)) {
aida = ((hidari + migi) // 2)
if (Data[aida] == atai) {
表示する(atai, "は", aida, "番目にありました")
owari = 1
} else {
if (Data[aida] < atai) {
hidari = (aida + 1)
} else {
migi = (aida - 1)
}
}
}
if (owari == 0) {
表示する(atai, "は見つかりませんでした")
}
表示する("添字", " ", "要素")
for i in 0..(kazu - 1) INCREMENT by IntLiteral(value=1, range=480..480, filePath=null) {
表示する(i, " ", Data[i])
}"""
        )
    }


    private fun testParser(input: String, expected: String) = runTest {
        val parser = Parser(preProcess(Lexer(input), resolveLib = { "" }).toList()).getOrNull()!!
        val prog = parser.parseProgram()
        if (prog.isLeft()) fail(prog.leftOrNull()?.message)
        assertEquals(expected, prog.getOrNull()!!.literal)
    }
}
