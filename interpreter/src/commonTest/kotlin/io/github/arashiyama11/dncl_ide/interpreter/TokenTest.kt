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
            Token.Comma(dummyRange) to "COMMA",
            Token.Colon(dummyRange) to "COLON",
            Token.ParenOpen(dummyRange) to "(",
            Token.ParenClose(dummyRange) to ")",
            Token.BraceOpen(dummyRange) to "{",
            Token.BraceClose(dummyRange) to "}",
            Token.BracketOpen(dummyRange) to "[",
            Token.BracketClose(dummyRange) to "]",
            Token.LenticularOpen(dummyRange) to "【",
            Token.LenticularClose(dummyRange) to "】",
            Token.If(dummyRange) to "IF",
            Token.Else(dummyRange) to "ELSE",
            Token.Elif(dummyRange) to "ELIF",
            Token.Then(dummyRange) to "THEN",
            Token.While(dummyRange) to "WHILE",
            Token.Function(dummyRange) to "FUNCTION",
            Token.Define(dummyRange) to "DEFINE",
            Token.And(dummyRange) to "AND",
            Token.Or(dummyRange) to "OR",
            Token.Wo(dummyRange) to "WO",
            Token.Kara(dummyRange) to "KARA",
            Token.Made(dummyRange) to "MADE",
            Token.UpTo(dummyRange) to "UpTo",
            Token.DownTo(dummyRange) to "DownTo",
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