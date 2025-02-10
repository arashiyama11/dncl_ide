package io.github.arashiyama11.dncl

import io.github.arashiyama11.dncl.lexer.Lexer
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.fail

class LexerTest {

    @Test
    fun test1() {
        val input = """100
99.999
1
0
0.0
3.13
"こんにちは"
"Hello"
""
"a"
表示する(x)
表示する(x,y,  z)
f(x)
kosu ← 3
tensu = 100
Tokuten[4] ← 100
Tokuten ← {87, 45, 72, 100}
Tokuten = [87, 45, 72, 100]
kosu_gokei ← kosu,tokuten ← kosu×(kosu＋1)
kosu を 1 増やす
# comment
#comment
x ← 【外部からの入力】 # comment
1+2
7-2
+5
-234
3*4
8/2
7÷2
5%2
x==2
x≠2
x>2
x<2
x≧2
x≦2
kosu == 2
kosu != 1234
kosu ≧12かつ kosu≦27
kosu ＞75でない
kosu ＞12かつkosu＜27でない
1 and 3>2
1==2 かつ 3+3>2
2==3 or 1!=1
"""
        var result = ""
        for (token in Lexer(input)) {
            if (token.isRight()) {
                result += token.getOrNull()!!
            } else {
                fail(token.leftOrNull()?.toString())
            }
        }
        assertEquals(
            "Int(literal=100)NewLineFloat(literal=99.999)NewLineInt(literal=1)NewLineInt(literal=0)NewLineFloat(literal=0.0)NewLineFloat(literal=3.13)NewLineString(literal=こんにちは)NewLineString(literal=Hello)NewLineString(literal=)NewLineString(literal=a)NewLineJapanese(literal=表示する)ParenOpenIdentifier(literal=x)ParenCloseNewLineJapanese(literal=表示する)ParenOpenIdentifier(literal=x)CommaIdentifier(literal=y)CommaIdentifier(literal=z)ParenCloseNewLineIdentifier(literal=f)ParenOpenIdentifier(literal=x)ParenCloseNewLineIdentifier(literal=kosu)AssignInt(literal=3)NewLineIdentifier(literal=tensu)AssignInt(literal=100)NewLineIdentifier(literal=Tokuten)BracketOpenInt(literal=4)BracketCloseAssignInt(literal=100)NewLineIdentifier(literal=Tokuten)AssignBraceOpenInt(literal=87)CommaInt(literal=45)CommaInt(literal=72)CommaInt(literal=100)BraceCloseNewLineIdentifier(literal=Tokuten)AssignBracketOpenInt(literal=87)CommaInt(literal=45)CommaInt(literal=72)CommaInt(literal=100)BracketCloseNewLineIdentifier(literal=kosu_gokei)AssignIdentifier(literal=kosu)CommaIdentifier(literal=tokuten)AssignIdentifier(literal=kosu)TimesParenOpenIdentifier(literal=kosu)PlusInt(literal=1)ParenCloseNewLineIdentifier(literal=kosu)WoInt(literal=1)Japanese(literal=増やす)NewLineNewLineNewLineIdentifier(literal=x)AssignLenticularOpenJapanese(literal=外部からの入力)LenticularCloseNewLineInt(literal=1)PlusInt(literal=2)NewLineInt(literal=7)MinusInt(literal=2)NewLinePlusInt(literal=5)NewLineMinusInt(literal=234)NewLineInt(literal=3)TimesInt(literal=4)NewLineInt(literal=8)DivideInt(literal=2)NewLineInt(literal=7)DivideIntInt(literal=2)NewLineInt(literal=5)ModuloInt(literal=2)NewLineIdentifier(literal=x)EqualInt(literal=2)NewLineIdentifier(literal=x)NotEqualInt(literal=2)NewLineIdentifier(literal=x)GreaterThanInt(literal=2)NewLineIdentifier(literal=x)LessThanInt(literal=2)NewLineIdentifier(literal=x)GreaterThanOrEqualInt(literal=2)NewLineIdentifier(literal=x)LessThanOrEqualInt(literal=2)NewLineIdentifier(literal=kosu)EqualInt(literal=2)NewLineIdentifier(literal=kosu)NotEqualInt(literal=1234)NewLineIdentifier(literal=kosu)GreaterThanOrEqualInt(literal=12)AndIdentifier(literal=kosu)LessThanOrEqualInt(literal=27)NewLineIdentifier(literal=kosu)GreaterThanInt(literal=75)Japanese(literal=でない)NewLineIdentifier(literal=kosu)GreaterThanInt(literal=12)Japanese(literal=かつkosu)LessThanInt(literal=27)Japanese(literal=でない)NewLineInt(literal=1)AndInt(literal=3)GreaterThanInt(literal=2)NewLineInt(literal=1)EqualInt(literal=2)AndInt(literal=3)PlusInt(literal=3)GreaterThanInt(literal=2)NewLineInt(literal=2)EqualInt(literal=3)OrInt(literal=1)NotEqualInt(literal=1)EOF",
            result
        )
        println(result)
    }

    @Test
    fun test2025_1() {
        val input = TestCase.exam2025_0
        var result = ""
        for (token in Lexer(input)) {
            if (token.isRight()) {
                result += token.getOrNull()!!
            } else {
                fail(token.leftOrNull()?.message)
            }
        }
        assertEquals(
            "Identifier(literal=Akibi)AssignBracketOpenInt(literal=5)CommaInt(literal=3)CommaInt(literal=4)BracketCloseNewLineIdentifier(literal=buinsu)AssignInt(literal=3)NewLineIdentifier(literal=tantou)AssignInt(literal=1)NewLineIdentifier(literal=buin)WoInt(literal=2)KaraIdentifier(literal=buinsu)MadeInt(literal=1)UpToColonNewLineIndent(depth=2, literal=Indent(2))IfIdentifier(literal=Akibi)BracketOpenIdentifier(literal=buin)BracketCloseLessThanIdentifier(literal=Akibi)BracketOpenIdentifier(literal=tantou)BracketCloseThenColonNewLineIndent(depth=4, literal=Indent(4))Identifier(literal=tantou)AssignIdentifier(literal=buin)NewLineJapanese(literal=表示する)ParenOpenString(literal=次の工芸品の担当は部員)CommaIdentifier(literal=tantou)CommaString(literal=です)ParenCloseEOF",
            result
        )
    }

    @Test
    fun test2025_2() {
        val input = TestCase.exam2025_1
        var result = ""
        for (token in Lexer(input)) {
            if (token.isRight()) {
                result += token.getOrNull()!!
            } else {
                fail(token.leftOrNull()?.message)
            }
        }
        assertEquals(
            "Identifier(literal=Nissu)AssignBracketOpenInt(literal=4)CommaInt(literal=1)CommaInt(literal=3)CommaInt(literal=1)CommaInt(literal=3)CommaInt(literal=4)CommaInt(literal=2)CommaInt(literal=4)CommaInt(literal=3)BracketCloseNewLineIdentifier(literal=kougeihinsu)AssignInt(literal=9)NewLineIdentifier(literal=Akibi)AssignBracketOpenInt(literal=1)CommaInt(literal=1)CommaInt(literal=1)BracketCloseNewLineIdentifier(literal=buinsu)AssignInt(literal=3)NewLineIdentifier(literal=kougeihin)WoInt(literal=1)KaraIdentifier(literal=kougeihinsu)MadeInt(literal=1)UpToColonNewLineIndent(depth=2, literal=Indent(2))Identifier(literal=tantou)AssignInt(literal=1)NewLineIndent(depth=2, literal=Indent(2))Identifier(literal=buin)WoInt(literal=2)KaraIdentifier(literal=buinsu)MadeInt(literal=1)UpToColonNewLineIndent(depth=2, literal=Indent(2))IfIdentifier(literal=Akibi)BracketOpenIdentifier(literal=buin)BracketCloseLessThanIdentifier(literal=Akibi)BracketOpenIdentifier(literal=tantou)BracketCloseThenColonNewLineIndent(depth=2, literal=Indent(2))Identifier(literal=tantou)AssignIdentifier(literal=buin)NewLineIndent(depth=2, literal=Indent(2))Japanese(literal=表示する)ParenOpenString(literal=工芸品)CommaIdentifier(literal=kougeihin)CommaString(literal= … )CommaNewLineIndent(depth=16, literal=Indent(16))String(literal=部員)CommaIdentifier(literal=tantou)CommaString(literal=：)CommaNewLineIndent(depth=16, literal=Indent(16))Identifier(literal=Akibi)BracketOpenIdentifier(literal=tantou)BracketCloseCommaString(literal=日目～)CommaNewLineIndent(depth=16, literal=Indent(16))Identifier(literal=Akibi)BracketOpenIdentifier(literal=tantou)BracketClosePlusIdentifier(literal=Nissu)BracketOpenIdentifier(literal=kougeihin)BracketCloseMinusInt(literal=1)CommaString(literal=日目)ParenCloseNewLineIndent(depth=2, literal=Indent(2))Identifier(literal=Akibi)BracketOpenIdentifier(literal=tantou)BracketCloseAssignIdentifier(literal=Akibi)BracketOpenIdentifier(literal=tantou)BracketClosePlusIdentifier(literal=Nissu)BracketOpenIdentifier(literal=kougeihin)BracketCloseEOF",
            result
        )
    }

    @Test
    fun testSisaku_0() {
        val input = TestCase.Sisaku2022
        var result = ""
        for (token in Lexer(input)) {
            if (token.isRight()) {
                result += token.getOrNull()!!
            } else {
                fail(token.leftOrNull()?.message)
            }
        }
        assertEquals(
            "Identifier(literal=Angoubun)AssignBracketOpenString(literal=p)CommaString(literal=y)CommaString(literal=e)CommaString(literal=b)CommaString(literal= )CommaString(literal=c)CommaString(literal=m)CommaString(literal=y)CommaString(literal=b)CommaString(literal=o)CommaString(literal= )CommaString(literal=k)CommaString(literal=x)CommaString(literal=n)CommaString(literal= )CommaString(literal=c)CommaString(literal=o)CommaString(literal=f)CommaString(literal=o)CommaString(literal=x)CommaString(literal= )CommaString(literal=i)CommaString(literal=o)CommaString(literal=k)CommaString(literal=b)CommaString(literal=c)CommaString(literal= )CommaString(literal=k)CommaString(literal=q)CommaString(literal=y)CommaString(literal= )BracketCloseNewLineNewLineIdentifier(literal=i)WoInt(literal=0)KaraInt(literal=1)MadeInt(literal=1)UpToColonNewLineIndent(depth=2, literal=Indent(2))Identifier(literal=bangou)AssignJapanese(literal=差分)ParenOpenIdentifier(literal=Angoubun)BracketOpenIdentifier(literal=i)BracketCloseParenCloseNewLineIndent(depth=2, literal=Indent(2))IfIdentifier(literal=bangou)NotEqualMinusInt(literal=1)ThenColonNewLineIndent(depth=4, literal=Indent(4))Identifier(literal=Hindo)BracketOpenIdentifier(literal=bangou)BracketCloseAssignIdentifier(literal=Hindo)BracketOpenIdentifier(literal=bangou)BracketClosePlusInt(literal=1)NewLineJapanese(literal=表示する)ParenOpenIdentifier(literal=Hindo)ParenCloseEOF",
            result
        )
    }

    @Test
    fun testSisaku_2022_0() {
        val input = TestCase.Sisaku2022_0
        var result = ""
        for (token in Lexer(input)) {
            if (token.isRight()) {
                result += token.getOrNull()!!
            } else {
                fail(token.leftOrNull()?.message)
            }
        }
        assertEquals(
            "Identifier(literal=Kouka)AssignBracketOpenInt(literal=1)CommaInt(literal=5)CommaInt(literal=10)CommaInt(literal=50)CommaInt(literal=100)BracketCloseNewLineIdentifier(literal=kingaku)AssignInt(literal=46)NewLineIdentifier(literal=maisu)AssignInt(literal=0)CommaIdentifier(literal=nokori)AssignIdentifier(literal=kingaku)NewLineIdentifier(literal=i)WoInt(literal=4)KaraInt(literal=0)MadeInt(literal=1)DownToColonNewLineIndent(depth=2, literal=Indent(2))Identifier(literal=maisu)AssignIdentifier(literal=maisu)PlusIdentifier(literal=nokori)DivideIntIdentifier(literal=Kouka)BracketOpenIdentifier(literal=i)BracketCloseNewLineIndent(depth=2, literal=Indent(2))Identifier(literal=nokori)AssignIdentifier(literal=nokori)ModuloIdentifier(literal=Kouka)BracketOpenIdentifier(literal=i)BracketCloseNewLineJapanese(literal=表示する)ParenOpenIdentifier(literal=maisu)ParenCloseEOF",
            result
        )
    }

    @Test
    fun testSisaku_2022_1() {
        val input = TestCase.Sisaku2022_1
        var result = ""
        for (token in Lexer(input)) {
            if (token.isRight()) {
                result += token.getOrNull()!!
            } else {
                fail(token.leftOrNull()?.message)
            }
        }
        assertEquals(
            "Identifier(literal=kakaku)AssignInt(literal=46)NewLineIdentifier(literal=min_maisu)AssignInt(literal=100)NewLineIdentifier(literal=tsuri)WoInt(literal=0)KaraInt(literal=99)MadeInt(literal=1)UpToColonNewLineIndent(depth=2, literal=Indent(2))Identifier(literal=shiharai)AssignIdentifier(literal=kakaku)PlusIdentifier(literal=tsuri)NewLineIndent(depth=2, literal=Indent(2))Identifier(literal=maisu)AssignJapanese(literal=枚数)ParenOpenIdentifier(literal=shiharai)ParenClosePlusJapanese(literal=枚数)ParenOpenIdentifier(literal=tsuri)ParenCloseNewLineIndent(depth=4, literal=Indent(4))IfIdentifier(literal=maisu)LessThanIdentifier(literal=min_maisu)ThenColonNewLineIndent(depth=6, literal=Indent(6))Identifier(literal=min_maisu)AssignIdentifier(literal=maisu)NewLineJapanese(literal=表示する)ParenOpenIdentifier(literal=min_maisu)ParenCloseEOF",
            result
        )
    }

    @Test
    fun testSisaku_2022_2() {
        val input = TestCase.Sisaku2022_2
        var result = ""
        for (token in Lexer(input)) {
            if (token.isRight()) {
                result += token.getOrNull()!!
            } else {
                fail(token.leftOrNull()?.message)
            }
        }
        assertEquals(
            "Identifier(literal=Data)AssignBracketOpenInt(literal=3)CommaInt(literal=18)CommaInt(literal=29)CommaInt(literal=33)CommaInt(literal=48)CommaInt(literal=52)CommaInt(literal=62)CommaInt(literal=77)CommaInt(literal=89)CommaInt(literal=97)BracketCloseNewLineIdentifier(literal=kazu)AssignJapanese(literal=要素数)ParenOpenIdentifier(literal=Data)ParenCloseNewLineJapanese(literal=表示する)ParenOpenString(literal=0～99の数字を入力してください)ParenCloseNewLineNewLineIdentifier(literal=hidari)AssignInt(literal=0)CommaIdentifier(literal=migi)AssignIdentifier(literal=kazu)MinusInt(literal=1)NewLineIdentifier(literal=owari)AssignInt(literal=0)NewLineIdentifier(literal=hidari)LessThanOrEqualIdentifier(literal=migi)AndIdentifier(literal=owari)EqualInt(literal=0)WhileColonNewLineIndent(depth=2, literal=Indent(2))Identifier(literal=aida)AssignParenOpenIdentifier(literal=hidari)PlusIdentifier(literal=migi)ParenCloseDivideIntInt(literal=2)NewLineIndent(depth=2, literal=Indent(2))IfIdentifier(literal=Data)BracketOpenIdentifier(literal=aida)BracketCloseEqualIdentifier(literal=atai)ThenColonNewLineIndent(depth=4, literal=Indent(4))Japanese(literal=表示する)ParenOpenIdentifier(literal=atai)CommaString(literal=は)CommaIdentifier(literal=aida)CommaString(literal=番目にありました)ParenCloseNewLineIndent(depth=4, literal=Indent(4))Identifier(literal=owari)AssignInt(literal=1)NewLineIndent(depth=2, literal=Indent(2))ElifIdentifier(literal=Data)BracketOpenIdentifier(literal=aida)BracketCloseLessThanIdentifier(literal=atai)ThenColonNewLineIndent(depth=4, literal=Indent(4))Identifier(literal=hidari)AssignIdentifier(literal=aida)PlusInt(literal=1)NewLineIndent(depth=2, literal=Indent(2))ElseColonNewLineIndent(depth=4, literal=Indent(4))Identifier(literal=migi)AssignIdentifier(literal=aida)MinusInt(literal=1)NewLineIfIdentifier(literal=owari)EqualInt(literal=0)ThenColonNewLineIndent(depth=2, literal=Indent(2))Japanese(literal=表示する)ParenOpenIdentifier(literal=atai)CommaString(literal=は見つかりませんでした)ParenCloseNewLineJapanese(literal=表示する)ParenOpenString(literal=添字)CommaString(literal= )CommaString(literal=要素)ParenCloseNewLineNewLineEOF",
            result
        )
    }
}