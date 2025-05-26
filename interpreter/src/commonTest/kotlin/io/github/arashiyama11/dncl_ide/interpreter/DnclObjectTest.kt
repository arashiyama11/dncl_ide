package io.github.arashiyama11.dncl_ide.interpreter

import io.github.arashiyama11.dncl_ide.interpreter.model.AllBuiltInFunction
import io.github.arashiyama11.dncl_ide.interpreter.model.AstNode
import io.github.arashiyama11.dncl_ide.interpreter.model.DnclObject
import io.github.arashiyama11.dncl_ide.interpreter.model.Environment
import kotlin.test.Test
import kotlin.test.assertEquals

class DnclObjectTest {

    private val dummyAstNode = AstNode.Identifier("dummy", IntRange(0, 0))
    private val dummyEnv = Environment()

    @Test
    fun testIntObject() {
        val intObj = DnclObject.Int(123, dummyAstNode)
        assertEquals("123", intObj.toString())
        assertEquals(123.hashCode(), intObj.hash())
    }

    @Test
    fun testFloatObject() {
        val floatObj = DnclObject.Float(12.34f, dummyAstNode)
        assertEquals("12.34", floatObj.toString())
        assertEquals(12.34f.hashCode(), floatObj.hash())
    }

    @Test
    fun testStringObject() {
        val strObj = DnclObject.String("hello", dummyAstNode)
        assertEquals("hello", strObj.toString())
        assertEquals("hello".hashCode(), strObj.hash())
    }

    @Test
    fun testBooleanObject() {
        val boolTrueObj = DnclObject.Boolean(true, dummyAstNode)
        assertEquals("真", boolTrueObj.toString())
        assertEquals(true.hashCode(), boolTrueObj.hash())

        val boolFalseObj = DnclObject.Boolean(false, dummyAstNode)
        assertEquals("偽", boolFalseObj.toString())
        assertEquals(false.hashCode(), boolFalseObj.hash())
    }

    @Test
    fun testArrayObject() {
        val elements = mutableListOf<DnclObject>(
            DnclObject.Int(1, dummyAstNode),
            DnclObject.String("two", dummyAstNode)
        )
        val arrayObj = DnclObject.Array(elements, dummyAstNode)
        assertEquals("[1, two]", arrayObj.toString())
        assertEquals(elements.hashCode(), arrayObj.hash())
    }

    @Test
    fun testFunctionObject() {
        val params = listOf("x", "y")
        val body =
            AstNode.BlockStatement(listOf()) // Empty body for simplicity, removed extra IntRange
        val funcObj = DnclObject.Function("myFunc", params, body, dummyEnv, dummyAstNode)
        assertEquals("<関数 myFunc(x, y)>", funcObj.toString())
        val expectedHash = params.hashCode() + 31 * body.hashCode() + 31 * 31 * dummyEnv.hashCode()
        assertEquals(expectedHash, funcObj.hash())

        val anonymousFuncObj = DnclObject.Function(null, params, body, dummyEnv, dummyAstNode)
        assertEquals("<関数 anonymous(x, y)>", anonymousFuncObj.toString())
    }

    @Test
    fun testBuiltInFunctionObject() {
        val builtInFuncObj = DnclObject.BuiltInFunction(AllBuiltInFunction.PRINT, dummyAstNode) {
            throw NotImplementedError() // Dummy execute
        }
        assertEquals("<組込関数 PRINT>", builtInFuncObj.toString())
        assertEquals(AllBuiltInFunction.PRINT.hashCode(), builtInFuncObj.hash())
    }

    @Test
    fun testNullObject() {
        val nullObj = DnclObject.Null(dummyAstNode)
        assertEquals("null", nullObj.toString())
        assertEquals(0, nullObj.hash())
    }

    @Test
    fun testReturnValueObject() {
        val value = DnclObject.Int(42, dummyAstNode)
        val returnValueObj = DnclObject.ReturnValue(value, dummyAstNode)
        assertEquals("42", returnValueObj.toString())
        assertEquals(value.hashCode(), returnValueObj.hash())
    }

    //    @Test
    //    fun testRuntimeErrorObject() {
    //        val errorMsg = "Runtime error occurred"
    //        val runtimeErrorObj = DnclObject.RuntimeError(errorMsg, dummyAstNode)
    //        assertEquals(errorMsg, runtimeErrorObj.toString())
    //        assertEquals(errorMsg.hashCode(), runtimeErrorObj.hash())
    //    }
    //
    //    @Test
    //    fun testArgumentSizeErrorObject() {
    //        val errorMsg = "Argument size mismatch"
    //        val argSizeErrorObj = DnclObject.ArgumentSizeError(errorMsg, dummyAstNode)
    //        assertEquals(errorMsg, argSizeErrorObj.toString())
    //        assertEquals(errorMsg.hashCode(), argSizeErrorObj.hash())
    //    }
    //
    //    @Test
    //    fun testTypeErrorObject() {
    //        val errorMsg = "Type mismatch"
    //        val typeErrorObj1 = DnclObject.TypeError(errorMsg, dummyAstNode)
    //        assertEquals(errorMsg, typeErrorObj1.toString())
    //        assertEquals(errorMsg.hashCode(), typeErrorObj1.hash())
    //
    //        val typeErrorObj2 = DnclObject.TypeError("Int", "String", dummyAstNode)
    //        val expectedMsg = "期待される型: Int, 実際の型: String"
    //        assertEquals(expectedMsg, typeErrorObj2.toString())
    //        assertEquals(expectedMsg.hashCode(), typeErrorObj2.hash())
    //    }
    //
    //    @Test
    //    fun testUndefinedErrorObject() {
    //        val errorMsg = "Undefined variable"
    //        val undefinedErrorObj = DnclObject.UndefinedError(errorMsg, dummyAstNode)
    //        assertEquals(errorMsg, undefinedErrorObj.toString())
    //        assertEquals(errorMsg.hashCode(), undefinedErrorObj.hash())
    //    }
    //
    //    @Test
    //    fun testIndexOutOfRangeErrorObject() {
    //        val index = 5
    //        val length = 3
    //        val indexErrorObj = DnclObject.IndexOutOfRangeError(index, length, dummyAstNode)
    //        val expectedMsg = "インデックス: $index, 配列の長さ: $length"
    //        assertEquals(expectedMsg, indexErrorObj.toString()) // Corrected: was assertEquals(expectedMsg, indexErrorObj)
    //        assertEquals(expectedMsg.hashCode(), indexErrorObj.hash())
    //    }
} 