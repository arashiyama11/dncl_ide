package io.github.arashiyama11.dncl_ide.domain.usecase

import io.github.arashiyama11.dncl_ide.domain.model.Definition
import io.github.arashiyama11.dncl_ide.interpreter.lexer.Lexer
import io.github.arashiyama11.dncl_ide.interpreter.model.AllBuiltInFunction
import io.github.arashiyama11.dncl_ide.interpreter.parser.Parser
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertTrue

class SuggestionUseCaseTest {
    private lateinit var suggestionUseCase: SuggestionUseCase

    @BeforeTest
    fun setup() {
        suggestionUseCase = SuggestionUseCase()
    }

    private fun getSuggestions(code: String, position: Int): List<Definition> {
        val lexer = Lexer(code)
        val parser =
            Parser(lexer).getOrNull() ?: throw IllegalStateException("Parse failed for code: $code")
        val program =
            parser.parseProgram().getOrNull()
                ?: throw IllegalStateException("Program parse failed for code: $code")
        val tokens = lexer.toList()
        return suggestionUseCase.suggestWithParsedData(code, position, tokens, program)
    }

    @Test
    fun testGlobalVariableSuggestion() {
        val code = """
            globalVar = 10
            anotherGlobal = "test"
        """.trimIndent()
        val position = code.length

        val suggestions = getSuggestions(code, position)

        assertTrue(suggestions.any { it.literal == "globalVar" })
        assertTrue(suggestions.any { it.literal == "anotherGlobal" })
        assertTrue(suggestions.any { it.literal == "表示する" }) // Built-in function
    }

    @Test
    fun testLocalVariableSuggestion() {
        val code = """
            関数 myFunc() を:
                localVar = 20
                anotherLocal = "local"
            と定義する
        """.trimIndent()
        val position =
            code.indexOf("anotherLocal = \"local\"") + "anotherLocal".length // カーソルをanotherLocalの直後に置く

        val suggestions = getSuggestions(code, position)

        assertTrue(suggestions.any { it.literal == "localVar" })
        assertTrue(suggestions.any { it.literal == "anotherLocal" })
    }

    @Test
    fun testBuiltInFunctionSuggestion() {
        val code = ""
        val position = 0

        val suggestions = getSuggestions(code, position)

        AllBuiltInFunction.allIdentifiers().forEach { funcName ->
            assertTrue(suggestions.any { it.literal == funcName && it.isFunction })
        }
    }

    @Test
    fun testScopeBasedSuggestionPriority() {
        val code = """
            globalVar = 10
            関数 myFunc() を:
                localVar = 20
                innerVar = 30 // カーソル位置の目印
            と定義する
        """.trimIndent()
        val position = code.indexOf("innerVar = 30") + "innerVar".length // innerVar の直後にカーソル

        val suggestions = getSuggestions(code, position)

        val localVarIndex = suggestions.indexOfFirst { it.literal == "localVar" }
        val innerVarIndex = suggestions.indexOfFirst { it.literal == "innerVar" }
        val globalVarIndex = suggestions.indexOfFirst { it.literal == "globalVar" }

        assertTrue(localVarIndex != -1, "localVar not found")
        assertTrue(innerVarIndex != -1, "innerVar not found")
        assertTrue(globalVarIndex != -1, "globalVar not found")

        // innerVar と localVar は globalVar より優先される
        assertTrue(
            innerVarIndex < globalVarIndex,
            "innerVar should be before globalVar. inner: $innerVarIndex, global: $globalVarIndex, suggestions: ${suggestions.map { it.literal }}"
        )
        assertTrue(
            localVarIndex < globalVarIndex,
            "localVar should be before globalVar. local: $localVarIndex, global: $globalVarIndex, suggestions: ${suggestions.map { it.literal }}"
        )
    }

    @Test
    fun testPartialMatchPriorityWithCurrentToken() {
        val code = """
            appleVar = 1
            apricotVar = 2
            bananaVar = 3
            ap # カーソルは "ap" の直後
        """.trimIndent()
        val position = code.indexOf("ap") + "ap".length

        val suggestions = getSuggestions(code, position)

        val appleIndex = suggestions.indexOfFirst { it.literal == "appleVar" }
        val apricotIndex = suggestions.indexOfFirst { it.literal == "apricotVar" }

        assertTrue(
            appleIndex != -1,
            "appleVar not found in suggestions: ${suggestions.map { it.literal }}"
        )
        assertTrue(
            apricotIndex != -1,
            "apricotVar not found in suggestions: ${suggestions.map { it.literal }}"
        )

        // "ap" で始まるものが優先される
        // 具体的な順序は実装依存だが、appleVar と apricotVar は bananaVar より前にあるはず
        val bananaIndex = suggestions.indexOfFirst { it.literal == "bananaVar" }
        if (bananaIndex != -1) { // bananaVar が候補にあれば
            assertTrue(appleIndex < bananaIndex)
            assertTrue(apricotIndex < bananaIndex)
        }

        // "ap" との前方一致度が高いものが先にくる (apricotのほうがappleより一致度が高いとは限らない。実装次第)
        // ここでは、apricot と apple が存在することを確認するに留める
    }

    @Test
    fun testPriorityWhenNoCurrentToken() {
        val code = """
            firstVar = 1
            関数 someFunc() を:
                secondVar = 2
                 # カーソルはここ
                 
            と定義する
            thirdVar = 3
        """.trimIndent()
        val position = code.indexOf("# カーソルはここ") + 3

        val suggestions = getSuggestions(code, position)

        val firstVarIndex = suggestions.indexOfFirst { it.literal == "firstVar" }
        val secondVarIndex = suggestions.indexOfFirst { it.literal == "secondVar" }
        val thirdVarIndex = suggestions.indexOfFirst { it.literal == "thirdVar" }
        val printFuncIndex = suggestions.indexOfFirst { it.literal == "表示する" }

        assertTrue(
            firstVarIndex != -1,
            "firstVar not found. Suggestions: ${suggestions.map { it.literal }}"
        )
        assertTrue(
            secondVarIndex != -1,
            "secondVar not found. Suggestions: ${suggestions.map { it.literal }}"
        )
        // thirdVar はカーソルより後なので、優先度が低いか、または候補に出ない場合もある (実装による)
        // assertTrue(thirdVarIndex != -1, "thirdVar not found. Suggestions: ${suggestions.map{it.literal}}")
        assertTrue(
            printFuncIndex != -1,
            "表示する not found. Suggestions: ${suggestions.map { it.literal }}"
        )

        // カーソルより手前で、より近いものが優先される (secondVar > firstVar)
        assertTrue(
            secondVarIndex < firstVarIndex,
            "secondVar should be before firstVar. second: $secondVarIndex, first: $firstVarIndex"
        )

        // ユーザー定義変数は組み込み関数より優先される
        assertTrue(
            firstVarIndex < printFuncIndex,
            "firstVar should be before 表示する. first: $firstVarIndex, print: $printFuncIndex"
        )
        assertTrue(
            secondVarIndex < printFuncIndex,
            "secondVar should be before 表示する. second: $secondVarIndex, print: $printFuncIndex"
        )

        // thirdVar がもし候補にある場合、firstVar や secondVar より後になる (カーソルより後方定義のため)
        if (thirdVarIndex != -1) {
            assertTrue(
                firstVarIndex < thirdVarIndex,
                "firstVar should be before thirdVar if thirdVar exists"
            )
            assertTrue(
                secondVarIndex < thirdVarIndex,
                "secondVar should be before thirdVar if thirdVar exists"
            )
        }
    }


    @Test
    fun testSuggestionWhenParseFails() {
        val code = "invalid code @@@"
        val position = code.length
        val suggestions = suggestionUseCase.suggestWhenFailingParse(code, position)

        assertTrue(
            suggestions.isEmpty(),
            "Suggestions should be empty on parse failure. Found: ${suggestions.map { it.literal }}"
        )
    }

    @Test
    fun testFunctionDefinitionIsFunctionTrue() {
        val code = """
            関数 add(a, b) を:
                戻り値(a + b)
            と定義する
        """.trimIndent()
        val position = code.length
        val suggestions = getSuggestions(code, position)
        val funcDef = suggestions.find { it.literal == "add" }
        assertTrue(funcDef != null, "Function 'add' not found in suggestions")
        assertTrue(funcDef.isFunction, "'add' should be marked as a function")
    }

    @Test
    fun testVariableDefinitionIsFunctionFalse() {
        val code = "myVar = 123"
        val position = code.length
        val suggestions = getSuggestions(code, position)
        val varDef = suggestions.find { it.literal == "myVar" }
        assertTrue(varDef != null, "Variable 'myVar' not found in suggestions")
        assertTrue(!varDef.isFunction, "'myVar' should not be marked as a function")
    }
} 