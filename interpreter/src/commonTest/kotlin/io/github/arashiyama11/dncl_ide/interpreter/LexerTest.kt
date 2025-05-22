package io.github.arashiyama11.dncl_ide.interpreter

import io.github.arashiyama11.dncl_ide.interpreter.lexer.Lexer
import io.github.arashiyama11.dncl_ide.interpreter.model.Token
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.fail

class LexerTest {
    private fun tokenToString(token: Token): String = "${token::class.simpleName}(${token.literal})"

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
                result += tokenToString(token.getOrNull()!!)
            } else {
                fail(token.leftOrNull()?.toString())
            }
        }
        println(result)
        assertEquals(
            "Indent(Indent(0))Int(100)NewLine(NEW_LINE)Indent(Indent(0))Float(99.999)NewLine(NEW_LINE)Indent(Indent(0))Int(1)NewLine(NEW_LINE)Indent(Indent(0))Int(0)NewLine(NEW_LINE)Indent(Indent(0))Float(0.0)NewLine(NEW_LINE)Indent(Indent(0))Float(3.13)NewLine(NEW_LINE)Indent(Indent(0))String(こんにちは)NewLine(NEW_LINE)Indent(Indent(0))String(Hello)NewLine(NEW_LINE)Indent(Indent(0))String()NewLine(NEW_LINE)Indent(Indent(0))String(a)NewLine(NEW_LINE)Indent(Indent(0))Japanese(表示する)ParenOpen(()Identifier(x)ParenClose())NewLine(NEW_LINE)Indent(Indent(0))Japanese(表示する)ParenOpen(()Identifier(x)Comma(,)Identifier(y)Comma(,)Identifier(z)ParenClose())NewLine(NEW_LINE)Indent(Indent(0))Identifier(f)ParenOpen(()Identifier(x)ParenClose())NewLine(NEW_LINE)Indent(Indent(0))Identifier(kosu)Assign(=)Int(3)NewLine(NEW_LINE)Indent(Indent(0))Identifier(tensu)Assign(=)Int(100)NewLine(NEW_LINE)Indent(Indent(0))Identifier(Tokuten)BracketOpen([)Int(4)BracketClose(])Assign(=)Int(100)NewLine(NEW_LINE)Indent(Indent(0))Identifier(Tokuten)Assign(=)BraceOpen({)Int(87)Comma(,)Int(45)Comma(,)Int(72)Comma(,)Int(100)BraceClose(})NewLine(NEW_LINE)Indent(Indent(0))Identifier(Tokuten)Assign(=)BracketOpen([)Int(87)Comma(,)Int(45)Comma(,)Int(72)Comma(,)Int(100)BracketClose(])NewLine(NEW_LINE)Indent(Indent(0))Identifier(kosu_gokei)Assign(=)Identifier(kosu)Comma(,)Identifier(tokuten)Assign(=)Identifier(kosu)Times(*)ParenOpen(()Identifier(kosu)Plus(+)Int(1)ParenClose())NewLine(NEW_LINE)Indent(Indent(0))Identifier(kosu)Wo(を)Int(1)Japanese(増やす)NewLine(NEW_LINE)Indent(Indent(0))Comment(# comment)NewLine(NEW_LINE)Indent(Indent(0))Comment(#comment)NewLine(NEW_LINE)Indent(Indent(0))Identifier(x)Assign(=)LenticularOpen(【)Japanese(外部からの入力)LenticularClose(】)Comment(# comment)NewLine(NEW_LINE)Indent(Indent(0))Int(1)Plus(+)Int(2)NewLine(NEW_LINE)Indent(Indent(0))Int(7)Minus(-)Int(2)NewLine(NEW_LINE)Indent(Indent(0))Plus(+)Int(5)NewLine(NEW_LINE)Indent(Indent(0))Minus(-)Int(234)NewLine(NEW_LINE)Indent(Indent(0))Int(3)Times(*)Int(4)NewLine(NEW_LINE)Indent(Indent(0))Int(8)Divide(/)Int(2)NewLine(NEW_LINE)Indent(Indent(0))Int(7)DivideInt(//)Int(2)NewLine(NEW_LINE)Indent(Indent(0))Int(5)Modulo(%)Int(2)NewLine(NEW_LINE)Indent(Indent(0))Identifier(x)Equal(==)Int(2)NewLine(NEW_LINE)Indent(Indent(0))Identifier(x)NotEqual(≠)Int(2)NewLine(NEW_LINE)Indent(Indent(0))Identifier(x)GreaterThan(>)Int(2)NewLine(NEW_LINE)Indent(Indent(0))Identifier(x)LessThan(<)Int(2)NewLine(NEW_LINE)Indent(Indent(0))Identifier(x)GreaterThanOrEqual(≧)Int(2)NewLine(NEW_LINE)Indent(Indent(0))Identifier(x)LessThanOrEqual(≦)Int(2)NewLine(NEW_LINE)Indent(Indent(0))Identifier(kosu)Equal(==)Int(2)NewLine(NEW_LINE)Indent(Indent(0))Identifier(kosu)NotEqual(≠)Int(1234)NewLine(NEW_LINE)Indent(Indent(0))Identifier(kosu)GreaterThanOrEqual(≧)Int(12)And(AND)Identifier(kosu)LessThanOrEqual(≦)Int(27)NewLine(NEW_LINE)Indent(Indent(0))Identifier(kosu)GreaterThan(>)Int(75)Japanese(でない)NewLine(NEW_LINE)Indent(Indent(0))Identifier(kosu)GreaterThan(>)Int(12)Japanese(かつkosu)LessThan(<)Int(27)Japanese(でない)NewLine(NEW_LINE)Indent(Indent(0))Int(1)And(AND)Int(3)GreaterThan(>)Int(2)NewLine(NEW_LINE)Indent(Indent(0))Int(1)Equal(==)Int(2)And(AND)Int(3)Plus(+)Int(3)GreaterThan(>)Int(2)NewLine(NEW_LINE)Indent(Indent(0))Int(2)Equal(==)Int(3)Or(OR)Int(1)NotEqual(≠)Int(1)EOF(EOF)",
            result
        )
        println(result)
    }

    @Test
    fun testFunction() {
        val input = TestCase.MaisuFunction
        var result = ""
        for (token in Lexer(input)) {
            if (token.isRight()) {
                println(tokenToString(token.getOrNull()!!))
                result += tokenToString(token.getOrNull()!!)
            } else {
                fail(token.leftOrNull()?.message)
            }
        }
        assertEquals(
            "Indent(Indent(0))NewLine(NEW_LINE)Indent(Indent(0))Function(関数)Japanese(枚数)ParenOpen(()Identifier(kingaku)ParenClose())Wo(を)Colon(:)NewLine(NEW_LINE)Indent(Indent(2))Identifier(Kouka)Assign(=)BracketOpen([)Int(1)Comma(,)Int(5)Comma(,)Int(10)Comma(,)Int(50)Comma(,)Int(100)BracketClose(])NewLine(NEW_LINE)Indent(Indent(2))Identifier(maisu)Assign(=)Int(0)Comma(,)Identifier(nokori)Assign(=)Identifier(kingaku)NewLine(NEW_LINE)Indent(Indent(2))Identifier(i)Wo(を)Int(4)Kara(から)Int(0)Made(まで)Int(1)DownTo(ずつ減らしながら繰り返す)Colon(:)NewLine(NEW_LINE)Indent(Indent(4))Identifier(maisu)Assign(=)Identifier(maisu)Plus(+)Identifier(nokori)DivideInt(//)Identifier(Kouka)BracketOpen([)Identifier(i)BracketClose(])NewLine(NEW_LINE)Indent(Indent(4))Identifier(nokori)Assign(=)Identifier(nokori)Modulo(%)Identifier(Kouka)BracketOpen([)Identifier(i)BracketClose(])NewLine(NEW_LINE)Indent(Indent(2))Japanese(戻り値)ParenOpen(()Identifier(maisu)ParenClose())NewLine(NEW_LINE)Indent(Indent(0))Define(と定義する)EOF(EOF)",
            result
        )
    }

    @Test
    fun test2025_1() {
        val input = TestCase.exam2025_0
        var result = ""
        for (token in Lexer(input)) {
            if (token.isRight()) {
                result += tokenToString(token.getOrNull()!!)
            } else {
                fail(token.leftOrNull()?.message)
            }
        }
        assertEquals(
            "Indent(Indent(0))Identifier(Akibi)Assign(=)BracketOpen([)Int(5)Comma(,)Int(3)Comma(,)Int(4)BracketClose(])NewLine(NEW_LINE)Indent(Indent(0))Identifier(buinsu)Assign(=)Int(3)NewLine(NEW_LINE)Indent(Indent(0))Identifier(tantou)Assign(=)Int(1)NewLine(NEW_LINE)Indent(Indent(0))Identifier(buin)Wo(を)Int(2)Kara(から)Identifier(buinsu)Made(まで)Int(1)UpTo(ずつ増やしながら繰り返す)Colon(:)NewLine(NEW_LINE)Indent(Indent(2))If(もし)Identifier(Akibi)BracketOpen([)Identifier(buin)BracketClose(])LessThan(<)Identifier(Akibi)BracketOpen([)Identifier(tantou)BracketClose(])Then(ならば)Colon(:)NewLine(NEW_LINE)Indent(Indent(4))Identifier(tantou)Assign(=)Identifier(buin)NewLine(NEW_LINE)Indent(Indent(0))Japanese(表示する)ParenOpen(()String(次の工芸品の担当は部員)Comma(,)Identifier(tantou)Comma(,)String(です)ParenClose())EOF(EOF)",
            result
        )
    }

    @Test
    fun test2025_2() {
        val input = TestCase.exam2025_1
        var result = ""
        for (token in Lexer(input)) {
            if (token.isRight()) {
                result += tokenToString(token.getOrNull()!!)
            } else {
                fail(token.leftOrNull()?.message)
            }
        }
        assertEquals(
            "Indent(Indent(0))Identifier(Nissu)Assign(=)BracketOpen([)Int(4)Comma(,)Int(1)Comma(,)Int(3)Comma(,)Int(1)Comma(,)Int(3)Comma(,)Int(4)Comma(,)Int(2)Comma(,)Int(4)Comma(,)Int(3)BracketClose(])NewLine(NEW_LINE)Indent(Indent(0))Identifier(kougeihinsu)Assign(=)Int(9)NewLine(NEW_LINE)Indent(Indent(0))Identifier(Akibi)Assign(=)BracketOpen([)Int(1)Comma(,)Int(1)Comma(,)Int(1)BracketClose(])NewLine(NEW_LINE)Indent(Indent(0))Identifier(buinsu)Assign(=)Int(3)NewLine(NEW_LINE)Indent(Indent(0))Identifier(kougeihin)Wo(を)Int(1)Kara(から)Identifier(kougeihinsu)Made(まで)Int(1)UpTo(ずつ増やしながら繰り返す)Colon(:)NewLine(NEW_LINE)Indent(Indent(2))Identifier(tantou)Assign(=)Int(1)NewLine(NEW_LINE)Indent(Indent(2))Identifier(buin)Wo(を)Int(2)Kara(から)Identifier(buinsu)Made(まで)Int(1)UpTo(ずつ増やしながら繰り返す)Colon(:)NewLine(NEW_LINE)Indent(Indent(4))If(もし)Identifier(Akibi)BracketOpen([)Identifier(buin)BracketClose(])LessThan(<)Identifier(Akibi)BracketOpen([)Identifier(tantou)BracketClose(])Then(ならば)Colon(:)NewLine(NEW_LINE)Indent(Indent(6))Identifier(tantou)Assign(=)Identifier(buin)NewLine(NEW_LINE)Indent(Indent(2))Japanese(表示する)ParenOpen(()String(工芸品)Comma(,)Identifier(kougeihin)Comma(,)String( … )Comma(,)NewLine(NEW_LINE)Indent(Indent(16))String(部員)Comma(,)Identifier(tantou)Comma(,)String(：)Comma(,)NewLine(NEW_LINE)Indent(Indent(16))Identifier(Akibi)BracketOpen([)Identifier(tantou)BracketClose(])Comma(,)String(日目～)Comma(,)NewLine(NEW_LINE)Indent(Indent(16))Identifier(Akibi)BracketOpen([)Identifier(tantou)BracketClose(])Plus(+)Identifier(Nissu)BracketOpen([)Identifier(kougeihin)BracketClose(])Minus(-)Int(1)Comma(,)String(日目)ParenClose())NewLine(NEW_LINE)Indent(Indent(2))Identifier(Akibi)BracketOpen([)Identifier(tantou)BracketClose(])Assign(=)Identifier(Akibi)BracketOpen([)Identifier(tantou)BracketClose(])Plus(+)Identifier(Nissu)BracketOpen([)Identifier(kougeihin)BracketClose(])EOF(EOF)",
            result
        )
    }

    @Test
    fun testSisaku_0() {
        val input = TestCase.Sisaku2022
        var result = ""
        for (token in Lexer(input)) {
            if (token.isRight()) {
                result += tokenToString(token.getOrNull()!!)
            } else {
                fail(token.leftOrNull()?.message)
            }
        }
        assertEquals(
            "Indent(Indent(0))Identifier(Angoubun)Assign(=)BracketOpen([)String(p)Comma(,)String(y)Comma(,)String(e)Comma(,)String(b)Comma(,)String( )Comma(,)String(c)Comma(,)String(m)Comma(,)String(y)Comma(,)String(b)Comma(,)String(o)Comma(,)String( )Comma(,)String(k)Comma(,)String(x)Comma(,)String(n)Comma(,)String( )Comma(,)String(c)Comma(,)String(o)Comma(,)String(f)Comma(,)String(o)Comma(,)String(x)Comma(,)String( )Comma(,)String(i)Comma(,)String(o)Comma(,)String(k)Comma(,)String(b)Comma(,)String(c)Comma(,)String( )Comma(,)String(k)Comma(,)String(q)Comma(,)String(y)Comma(,)String( )BracketClose(])NewLine(NEW_LINE)Indent(Indent(0))Identifier(Hindo)Assign(=)BracketOpen([)Int(0)Comma(,)Int(0)Comma(,)Int(0)Comma(,)Int(0)Comma(,)Int(0)Comma(,)Int(0)Comma(,)Int(0)Comma(,)Int(0)Comma(,)Int(0)Comma(,)Int(0)Comma(,)Int(0)Comma(,)Int(0)Comma(,)Int(0)Comma(,)Int(0)Comma(,)Int(0)Comma(,)Int(0)Comma(,)Int(0)Comma(,)Int(0)Comma(,)Int(0)Comma(,)Int(0)Comma(,)Int(0)Comma(,)Int(0)Comma(,)Int(0)Comma(,)Int(0)Comma(,)Int(0)Comma(,)Int(0)BracketClose(])NewLine(NEW_LINE)Indent(Indent(0))Identifier(i)Wo(を)Int(0)Kara(から)Japanese(要素数)ParenOpen(()Identifier(Angoubun)ParenClose())Minus(-)Int(1)Made(まで)Int(1)UpTo(ずつ増やしながら繰り返す)Colon(:)NewLine(NEW_LINE)Indent(Indent(2))Identifier(bangou)Assign(=)Japanese(差分)ParenOpen(()Identifier(Angoubun)BracketOpen([)Identifier(i)BracketClose(])ParenClose())NewLine(NEW_LINE)Indent(Indent(2))If(もし)Identifier(bangou)NotEqual(≠)Minus(-)Int(1)Then(ならば)Colon(:)NewLine(NEW_LINE)Indent(Indent(4))Identifier(Hindo)BracketOpen([)Identifier(bangou)BracketClose(])Assign(=)Identifier(Hindo)BracketOpen([)Identifier(bangou)BracketClose(])Plus(+)Int(1)NewLine(NEW_LINE)Indent(Indent(0))Japanese(表示する)ParenOpen(()Identifier(Hindo)ParenClose())EOF(EOF)",
            result
        )
    }

    @Test
    fun testSisaku_2022_0() {
        val input = TestCase.Sisaku2022_0
        var result = ""
        for (token in Lexer(input)) {
            if (token.isRight()) {
                result += tokenToString(token.getOrNull()!!)
            } else {
                fail(token.leftOrNull()?.message)
            }
        }
        assertEquals(
            "Indent(Indent(0))Identifier(Kouka)Assign(=)BracketOpen([)Int(1)Comma(,)Int(5)Comma(,)Int(10)Comma(,)Int(50)Comma(,)Int(100)BracketClose(])NewLine(NEW_LINE)Indent(Indent(0))Identifier(kingaku)Assign(=)Int(46)NewLine(NEW_LINE)Indent(Indent(0))Identifier(maisu)Assign(=)Int(0)Comma(,)Identifier(nokori)Assign(=)Identifier(kingaku)NewLine(NEW_LINE)Indent(Indent(0))Identifier(i)Wo(を)Int(4)Kara(から)Int(0)Made(まで)Int(1)DownTo(ずつ減らしながら繰り返す)Colon(:)NewLine(NEW_LINE)Indent(Indent(2))Identifier(maisu)Assign(=)Identifier(maisu)Plus(+)Identifier(nokori)DivideInt(//)Identifier(Kouka)BracketOpen([)Identifier(i)BracketClose(])NewLine(NEW_LINE)Indent(Indent(2))Identifier(nokori)Assign(=)Identifier(nokori)Modulo(%)Identifier(Kouka)BracketOpen([)Identifier(i)BracketClose(])NewLine(NEW_LINE)Indent(Indent(0))Japanese(表示する)ParenOpen(()Identifier(maisu)ParenClose())EOF(EOF)",
            result
        )
    }

    @Test
    fun testSisaku_2022_1() {
        val input = TestCase.Sisaku2022_1
        var result = ""
        for (token in Lexer(input)) {
            if (token.isRight()) {
                result += tokenToString(token.getOrNull()!!)
            } else {
                fail(token.leftOrNull()?.message)
            }
        }
        assertEquals(
            "Indent(Indent(0))Identifier(kakaku)Assign(=)Int(46)NewLine(NEW_LINE)Indent(Indent(0))Identifier(min_maisu)Assign(=)Int(100)NewLine(NEW_LINE)Indent(Indent(0))Identifier(tsuri)Wo(を)Int(0)Kara(から)Int(99)Made(まで)Int(1)UpTo(ずつ増やしながら繰り返す)Colon(:)NewLine(NEW_LINE)Indent(Indent(2))Identifier(shiharai)Assign(=)Identifier(kakaku)Plus(+)Identifier(tsuri)NewLine(NEW_LINE)Indent(Indent(2))Identifier(maisu)Assign(=)Japanese(枚数)ParenOpen(()Identifier(shiharai)ParenClose())Plus(+)Japanese(枚数)ParenOpen(()Identifier(tsuri)ParenClose())NewLine(NEW_LINE)Indent(Indent(2))If(もし)Identifier(maisu)LessThan(<)Identifier(min_maisu)Then(ならば)Colon(:)NewLine(NEW_LINE)Indent(Indent(4))Identifier(min_maisu)Assign(=)Identifier(maisu)NewLine(NEW_LINE)Indent(Indent(0))Japanese(表示する)ParenOpen(()Identifier(min_maisu)ParenClose())EOF(EOF)",
            result
        )
    }

    @Test
    fun testSisaku_2022_2() {
        val input = TestCase.Sisaku2022_2
        var result = ""
        for (token in Lexer(input)) {
            if (token.isRight()) {
                result += tokenToString(token.getOrNull()!!)
            } else {
                fail(token.leftOrNull()?.message)
            }
        }
        assertEquals(
            "Indent(Indent(0))Identifier(Data)Assign(=)BracketOpen([)Int(3)Comma(,)Int(18)Comma(,)Int(29)Comma(,)Int(33)Comma(,)Int(48)Comma(,)Int(52)Comma(,)Int(62)Comma(,)Int(77)Comma(,)Int(89)Comma(,)Int(97)BracketClose(])NewLine(NEW_LINE)Indent(Indent(0))Identifier(kazu)Assign(=)Japanese(要素数)ParenOpen(()Identifier(Data)ParenClose())NewLine(NEW_LINE)Indent(Indent(0))Japanese(表示する)ParenOpen(()String(0～99の数字を入力してください)ParenClose())NewLine(NEW_LINE)Indent(Indent(0))Identifier(atai)Assign(=)Japanese(整数変換)ParenOpen(()LenticularOpen(【)Japanese(外部からの入力)LenticularClose(】)ParenClose())NewLine(NEW_LINE)Indent(Indent(0))Identifier(hidari)Assign(=)Int(0)Comma(,)Identifier(migi)Assign(=)Identifier(kazu)Minus(-)Int(1)NewLine(NEW_LINE)Indent(Indent(0))Identifier(owari)Assign(=)Int(0)NewLine(NEW_LINE)Indent(Indent(0))Identifier(hidari)LessThanOrEqual(≦)Identifier(migi)And(AND)Identifier(owari)Equal(==)Int(0)While(の間繰り返す)Colon(:)NewLine(NEW_LINE)Indent(Indent(2))Identifier(aida)Assign(=)ParenOpen(()Identifier(hidari)Plus(+)Identifier(migi)ParenClose())DivideInt(//)Int(2)Comment(# 演算子÷は商の整数値を返す)NewLine(NEW_LINE)Indent(Indent(2))If(もし)Identifier(Data)BracketOpen([)Identifier(aida)BracketClose(])Equal(==)Identifier(atai)Then(ならば)Colon(:)NewLine(NEW_LINE)Indent(Indent(4))Japanese(表示する)ParenOpen(()Identifier(atai)Comma(,)String(は)Comma(,)Identifier(aida)Comma(,)String(番目にありました)ParenClose())NewLine(NEW_LINE)Indent(Indent(4))Identifier(owari)Assign(=)Int(1)NewLine(NEW_LINE)Indent(Indent(2))Elif(そうでなくもし)Identifier(Data)BracketOpen([)Identifier(aida)BracketClose(])LessThan(<)Identifier(atai)Then(ならば)Colon(:)NewLine(NEW_LINE)Indent(Indent(4))Identifier(hidari)Assign(=)Identifier(aida)Plus(+)Int(1)NewLine(NEW_LINE)Indent(Indent(2))Else(そうでなければ)Colon(:)NewLine(NEW_LINE)Indent(Indent(4))Identifier(migi)Assign(=)Identifier(aida)Minus(-)Int(1)NewLine(NEW_LINE)Indent(Indent(0))If(もし)Identifier(owari)Equal(==)Int(0)Then(ならば)Colon(:)NewLine(NEW_LINE)Indent(Indent(2))Japanese(表示する)ParenOpen(()Identifier(atai)Comma(,)String(は見つかりませんでした)ParenClose())NewLine(NEW_LINE)Indent(Indent(0))Japanese(表示する)ParenOpen(()String(添字)Comma(,)String( )Comma(,)String(要素)ParenClose())NewLine(NEW_LINE)Indent(Indent(0))Identifier(i)Wo(を)Int(0)Kara(から)Identifier(kazu)Minus(-)Int(1)Made(まで)Int(1)UpTo(ずつ増やしながら繰り返す)Colon(:)NewLine(NEW_LINE)Indent(Indent(2))Japanese(表示する)ParenOpen(()Identifier(i)Comma(,)String( )Comma(,)Identifier(Data)BracketOpen([)Identifier(i)BracketClose(])ParenClose())EOF(EOF)",
            result
        )
    }
}