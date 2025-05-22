package io.github.arashiyama11.dncl_ide.interpreter

import io.github.arashiyama11.dncl_ide.interpreter.model.LexerError
import io.github.arashiyama11.dncl_ide.interpreter.model.ParserError
import io.github.arashiyama11.dncl_ide.interpreter.model.Token
import io.github.arashiyama11.dncl_ide.interpreter.model.InternalError
import kotlin.test.Test
import kotlin.test.assertEquals

class ErrorExplainTest {

    // --- LexerError Tests ---
    @Test
    fun testLexerError_UnExpectedCharacter_singleLine_halfWidth() {
        val program = "abcde;fghij"
        // Error at 'f'
        val error = LexerError.UnExpectedCharacter('f', 6)
        val expected = """1行7文字目でエラーが発生しました
「f」は無効な文字です
===============
1| abcde;fghij
         ^""".trimIndent()
        assertEquals(expected, error.explain(program).trimIndent())
    }

    @Test
    fun testLexerError_UnExpectedCharacter_singleLine_fullWidth() {
        val program = "あいうえお かきくけこ"
        // Error at 'か'
        val error = LexerError.UnExpectedCharacter('か', 6)
        val expected = """1行7文字目でエラーが発生しました
「か」は無効な文字です
===============
1| あいうえお かきくけこ
              ^
""".trimIndent()
        assertEquals(expected, error.explain(program).trimIndent())
    }

    @Test
    fun testLexerError_UnExpectedCharacter_singleLine_mixedWidth() {
        val program = "abc あいう かきく"
        // Error at 'あ'
        val error = LexerError.UnExpectedCharacter('あ', 4)
        val expected = """1行5文字目でエラーが発生しました
「あ」は無効な文字です
===============
1| abc あいう かきく
       ^
""".trimIndent()
        assertEquals(expected, error.explain(program).trimIndent())
    }

    @Test
    fun testLexerError_UnExpectedEOF() {
        val program = "abcde"
        val error = LexerError.UnExpectedEOF(5, 5..5, "予期しないEOF")
        val expected = """1行6文字目でエラーが発生しました
予期しないEOF
===============
1| abcde
        ^"""
        assertEquals(expected, error.explain(program).trimIndent())
    }

    // --- ParserError Tests ---

    private fun dummyToken(literal: String, range: IntRange): Token {
        return Token.Identifier(literal, range)
    }

    @Test
    fun testParserError_UnExpectedToken_singleLine_multiCharToken() {
        val program = "表示する result"
        // Error at 'result', range 9..14
        val errorToken = dummyToken("result", 5..10)
        val error = ParserError.UnExpectedToken(errorToken)
        val expected = """1行6文字目でエラーが発生しました
予期しないトークン: result
===============
1| 表示する result
            ^^^^^^""" // Note: explain shows 1 less ^ than token length
        assertEquals(expected, error.explain(program).trimIndent())
    }


    @Test
    fun testParserError_UnExpectedToken_fullWidth() {
        val program = "もし　あああ　ならば"
        // Error at 'あああ', range 3..5
        val errorToken = dummyToken("あああ", 3..5)
        val error = ParserError.UnExpectedToken(errorToken)
        val expected = """1行4文字目でエラーが発生しました
予期しないトークン: あああ
===============
1| もし　あああ　ならば
         ^^^^^^"""
        assertEquals(expected, error.explain(program).trimIndent())
    }

    // --- InternalError Test ---
    @Test
    fun testInternalError() {
        val errorMessage = "これは内部エラーです"
        val error = InternalError(errorMessage)
        assertEquals(errorMessage, error.explain("any program string"))
    }

    fun testRealProgram_Lexer_UnexpectedToken() {
        val program = "hello~world"
    }

    fun testRealProgram_Lexer_StringNotClose() {
        val program = " \"some str "
    }

    fun testRealProgram_Parser_ParenNotClose() {
        val program = "(1 + 2"
    }

    fun testRealProgram_Parser__() {
        val program = "x <> 1"
    }

}