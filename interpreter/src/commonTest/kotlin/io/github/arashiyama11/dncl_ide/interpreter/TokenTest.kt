package io.github.arashiyama11.dncl_ide.interpreter

import io.github.arashiyama11.dncl_ide.interpreter.model.Token
import kotlin.test.Test
import kotlin.test.assertEquals

class TokenTest {

    @Test
    fun testTokenCreation() {
        val dummyRange = IntRange(0, 0)
        val tokens = listOf<Pair<Token, String>>(
            Token.EOF(dummyRange) to "EOF",
            Token.Identifier("myVar", dummyRange) to "myVar",
            Token.Int("123", dummyRange) to "123",
            Token.Float("12.34", dummyRange) to "12.34",
            Token.String("hello", dummyRange) to "hello",
            Token.Assign(dummyRange) to "=",
            Token.Plus(dummyRange) to "+",
            Token.Minus(dummyRange) to "-",
            Token.Times(dummyRange) to "*",
            Token.Divide(dummyRange) to "/",
            Token.DivideInt(dummyRange) to "//",
            Token.Modulo(dummyRange) to "%",
            Token.Bang(dummyRange) to "!",
            Token.Equal(dummyRange) to "==",
            Token.NotEqual(dummyRange) to "≠",
            Token.LessThan(dummyRange) to "<",
            Token.LessThanOrEqual(dummyRange) to "≦",
            Token.GreaterThan(dummyRange) to ">",
            Token.GreaterThanOrEqual(dummyRange) to "≧",
            Token.Comma(dummyRange) to ",",
            Token.Colon(dummyRange) to ":",
            Token.ParenOpen(dummyRange) to "(",
            Token.ParenClose(dummyRange) to ")",
            Token.BraceOpen(dummyRange) to "{",
            Token.BraceClose(dummyRange) to "}",
            Token.BracketOpen(dummyRange) to "[",
            Token.BracketClose(dummyRange) to "]",
            Token.LenticularOpen(dummyRange) to "【",
            Token.LenticularClose(dummyRange) to "】",
            Token.If(dummyRange) to "もし",
            Token.Else(dummyRange) to "そうでなければ",
            Token.Elif(dummyRange) to "そうでなくもし",
            Token.Then(dummyRange) to "ならば",
            Token.While(dummyRange) to "の間繰り返す",
            Token.Function(dummyRange) to "関数",
            Token.Define(dummyRange) to "と定義する",
            Token.And(dummyRange) to "AND",
            Token.Or(dummyRange) to "OR",
            Token.Wo(dummyRange) to "を",
            Token.Kara(dummyRange) to "から",
            Token.Made(dummyRange) to "まで",
            Token.UpTo(dummyRange) to "ずつ増やしながら繰り返す",
            Token.DownTo(dummyRange) to "ずつ減らしながら繰り返す",
            Token.NewLine(dummyRange) to "NEW_LINE",
            Token.Indent(0, dummyRange) to "Indent(0)",
            Token.Comment("# comment", dummyRange) to "# comment",
            Token.Japanese("日本語の識別子", dummyRange) to "日本語の識別子"
        )

        tokens.forEach { (token, expectedLiteral) ->
            assertEquals(
                expectedLiteral,
                token.literal,
                "Expected literal '${'$'}expectedLiteral' for token ${'$'}{token::class.simpleName}, but got '${'$'}{token.literal}'"
            )
        }
    }
}