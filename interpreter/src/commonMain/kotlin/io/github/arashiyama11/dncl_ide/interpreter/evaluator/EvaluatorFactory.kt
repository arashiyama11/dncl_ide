package io.github.arashiyama11.dncl_ide.interpreter.evaluator

import io.github.arashiyama11.dncl_ide.interpreter.model.BuiltInFunction
import io.github.arashiyama11.dncl_ide.interpreter.model.DnclObject
import io.github.arashiyama11.dncl_ide.interpreter.model.SystemCommand
import kotlinx.coroutines.delay

object EvaluatorFactory {
    fun create(
        input: String,
        arrayOrigin: Int,
        onStdout: suspend CallBuiltInFunctionScope.(String) -> Unit,
        onClear: suspend CallBuiltInFunctionScope.() -> Unit = {},
        onImport: suspend CallBuiltInFunctionScope.(String) -> DnclObject
    ): Evaluator {
        return Evaluator(
            {
                when (fn) {
                    BuiltInFunction.PRINT -> {
                        onStdout(args.joinToString(" ") { it.toString() })
                        DnclObject.Null(astNode)
                    }

                    BuiltInFunction.LENGTH -> {
                        checkArgSize(1)?.let { return@Evaluator it }
                        when (args[0]) {
                            is DnclObject.Array -> DnclObject.Int(
                                (args[0] as DnclObject.Array).value.size,
                                astNode
                            )

                            else -> DnclObject.TypeError(
                                "第一引数は配列でなければなりません",
                                astNode
                            )
                        }
                    }

                    BuiltInFunction.DIFF -> {
                        checkArgSize(1)?.let { return@Evaluator it }
                        when (args[0]) {
                            is DnclObject.String -> {
                                val str = (args[0] as DnclObject.String).value
                                require(str.length == 1)
                                if (str == " ") DnclObject.Int(
                                    -1,
                                    astNode
                                ) else DnclObject.Int(
                                    str[0].code - 'a'.code,
                                    astNode
                                )
                            }

                            else -> DnclObject.TypeError(
                                "第一引数は文字列でなければなりません",
                                astNode
                            )
                        }
                    }

                    BuiltInFunction.RETURN -> {
                        checkArgSize(1)?.let { return@Evaluator it }
                        DnclObject.ReturnValue(args[0], astNode)
                    }

                    BuiltInFunction.CONCAT -> {
                        checkArgSize(2)?.let { return@Evaluator it }
                        when {
                            args[0] is DnclObject.Array && args[1] is DnclObject.Array -> {
                                val a = (args[0] as DnclObject.Array).value
                                val b = (args[1] as DnclObject.Array).value
                                DnclObject.Array((a + b).toMutableList(), astNode)
                            }

                            else -> DnclObject.TypeError(
                                "第一引数、第二引数ともに配列でなければなりません",
                                astNode
                            )
                        }
                    }

                    BuiltInFunction.PUSH -> {
                        checkArgSize(2)?.let { return@Evaluator it }
                        when (args[0]) {
                            is DnclObject.Array -> {
                                val a = (args[0] as DnclObject.Array).value
                                val b = args[1]
                                a.add(b)
                                DnclObject.Null(astNode)
                            }

                            else -> DnclObject.TypeError(
                                "第一引数は配列でなければなりません",
                                astNode
                            )
                        }
                    }

                    BuiltInFunction.SHIFT -> {
                        checkArgSize(1)?.let { return@Evaluator it }
                        when (args[0]) {
                            is DnclObject.Array -> {
                                val a = (args[0] as DnclObject.Array).value
                                if (a.isEmpty()) DnclObject.Null(args[0].astNode) else a.removeAt(0)
                            }

                            else -> DnclObject.TypeError(
                                "第一引数は配列でなければなりません",
                                astNode
                            )
                        }
                    }

                    BuiltInFunction.UNSHIFT -> {
                        checkArgSize(2)?.let { return@Evaluator it }
                        when (args[0]) {
                            is DnclObject.Array -> {
                                val a = (args[0] as DnclObject.Array).value
                                val b = args[1]
                                a.add(0, b)
                                DnclObject.Null(astNode)
                            }

                            else -> DnclObject.TypeError(
                                "第一引数は配列でなければなりません",
                                astNode
                            )
                        }
                    }

                    BuiltInFunction.POP -> {
                        checkArgSize(1)?.let { return@Evaluator it }
                        when (args[0]) {
                            is DnclObject.Array -> {
                                val a = (args[0] as DnclObject.Array).value
                                if (a.isEmpty()) DnclObject.Null(astNode) else a.removeAt(a.size - 1)
                            }

                            else -> DnclObject.TypeError(
                                "第一引数は配列でなければなりません",
                                astNode
                            )
                        }
                    }

                    BuiltInFunction.INT -> {
                        checkArgSize(1)?.let { return@Evaluator it }
                        when (args[0]) {
                            is DnclObject.String -> {
                                val str = (args[0] as DnclObject.String).value
                                DnclObject.Int(str.toIntOrNull() ?: 0, astNode)
                            }

                            is DnclObject.Float -> {
                                val flt = (args[0] as DnclObject.Float).value
                                DnclObject.Int(flt.toInt(), astNode)
                            }

                            is DnclObject.Int -> args[0]

                            else -> DnclObject.TypeError(
                                "文字列,小数,整数でなければなりません",
                                astNode
                            )
                        }
                    }

                    BuiltInFunction.FLOAT -> {
                        checkArgSize(1)?.let { return@Evaluator it }
                        when (args[0]) {
                            is DnclObject.String -> {
                                val str = (args[0] as DnclObject.String).value
                                DnclObject.Float(str.toFloatOrNull() ?: 0f, astNode)
                            }

                            is DnclObject.Int -> {
                                val int = (args[0] as DnclObject.Int).value
                                DnclObject.Float(int.toFloat(), astNode)
                            }

                            is DnclObject.Float -> args[0]

                            else -> DnclObject.TypeError(
                                "文字列,小数,整数でなければなりません",
                                astNode
                            )
                        }
                    }

                    BuiltInFunction.STRING -> {
                        checkArgSize(1)?.let { return@Evaluator it }
                        DnclObject.String(args[0].toString(), astNode)
                    }

                    BuiltInFunction.IMPORT -> {
                        checkArgSize(1)?.let { return@Evaluator it }
                        when (args[0]) {
                            is DnclObject.String -> {
                                onImport((args[0] as DnclObject.String).value)
                            }

                            else -> return@Evaluator DnclObject.TypeError("", astNode)
                        }
                    }

                    BuiltInFunction.CHAR_CODE -> {
                        checkArgSize(1)?.let { return@Evaluator it }
                        when (args[0]) {
                            is DnclObject.String -> {
                                val str = (args[0] as DnclObject.String).value
                                if (str.length != 1) return@Evaluator DnclObject.RuntimeError(
                                    "文字列の長さは1でなければなりません",
                                    astNode
                                )
                                DnclObject.Int(str[0].code, astNode)
                            }

                            else -> DnclObject.TypeError(
                                "第一引数は文字列でなければなりません",
                                astNode
                            )
                        }
                    }

                    BuiltInFunction.FROM_CHAR_CODE -> {
                        checkArgSize(1)?.let { return@Evaluator it }
                        when (args[0]) {
                            is DnclObject.Int -> {
                                val int = (args[0] as DnclObject.Int).value
                                DnclObject.String(int.toChar().toString(), astNode)
                            }

                            else -> DnclObject.TypeError(
                                "第一引数は正数でなければなりません",
                                astNode
                            )
                        }
                    }

                    // 配列操作関数
                    BuiltInFunction.SLICE -> {
                        checkArgSize(3)?.let { return@Evaluator it }
                        when {
                            args[0] is DnclObject.Array && args[1] is DnclObject.Int && args[2] is DnclObject.Int -> {
                                val array = (args[0] as DnclObject.Array).value
                                val start = (args[1] as DnclObject.Int).value
                                val end = (args[2] as DnclObject.Int).value
                                if (start < 0 || start >= array.size || end < start || end > array.size) {
                                    DnclObject.IndexOutOfRangeError(
                                        start,
                                        array.size,
                                        astNode
                                    )
                                } else {
                                    DnclObject.Array(
                                        array.subList(start, end).toMutableList(),
                                        astNode
                                    )
                                }
                            }

                            else -> DnclObject.TypeError(
                                "第一引数は配列、第二引数と第三引数は整数でなければなりません",
                                astNode
                            )
                        }
                    }

                    BuiltInFunction.JOIN -> {
                        checkArgSize(2)?.let { return@Evaluator it }
                        when {
                            args[0] is DnclObject.Array && args[1] is DnclObject.String -> {
                                val array = (args[0] as DnclObject.Array).value
                                val separator = (args[1] as DnclObject.String).value
                                DnclObject.String(
                                    array.joinToString(separator) { it.toString() },
                                    astNode
                                )
                            }

                            else -> DnclObject.TypeError(
                                "第一引数は配列、第二引数は文字列でなければなりません",
                                astNode
                            )
                        }
                    }

                    BuiltInFunction.SORT -> {
                        checkArgSize(1)?.let { return@Evaluator it }
                        when (args[0]) {
                            is DnclObject.Array -> {
                                val array = (args[0] as DnclObject.Array).value
                                val sortedArray = array.sortedWith { a, b ->
                                    when {
                                        a is DnclObject.Int && b is DnclObject.Int ->
                                            a.value.compareTo(b.value)

                                        a is DnclObject.Float && b is DnclObject.Float ->
                                            a.value.compareTo(b.value)

                                        a is DnclObject.String && b is DnclObject.String ->
                                            a.value.compareTo(b.value)

                                        else -> 0
                                    }
                                }.toMutableList()
                                DnclObject.Array(sortedArray, astNode)
                            }

                            else -> DnclObject.TypeError(
                                "第一引数は配列でなければなりません",
                                astNode
                            )
                        }
                    }

                    BuiltInFunction.REVERSE -> {
                        checkArgSize(1)?.let { return@Evaluator it }
                        when (args[0]) {
                            is DnclObject.Array -> {
                                val array = (args[0] as DnclObject.Array).value
                                DnclObject.Array(array.reversed().toMutableList(), astNode)
                            }

                            else -> DnclObject.TypeError(
                                "第一引数は配列でなければなりません",
                                astNode
                            )
                        }
                    }

                    BuiltInFunction.FIND -> {
                        checkArgSize(2)?.let { return@Evaluator it }
                        when (args[0]) {
                            is DnclObject.Array -> {
                                val array = (args[0] as DnclObject.Array).value
                                val target = args[1]
                                val index =
                                    array.indexOfFirst { it.toString() == target.toString() }
                                DnclObject.Int(index, astNode)
                            }

                            else -> DnclObject.TypeError(
                                "第一引数は配列でなければなりません",
                                astNode
                            )
                        }
                    }

                    // 文字列操作関数
                    BuiltInFunction.SUBSTRING -> {
                        checkArgSize(3)?.let { return@Evaluator it }
                        when {
                            args[0] is DnclObject.String && args[1] is DnclObject.Int && args[2] is DnclObject.Int -> {
                                val str = (args[0] as DnclObject.String).value
                                val start = (args[1] as DnclObject.Int).value
                                val end = (args[2] as DnclObject.Int).value
                                if (start < 0 || start >= str.length || end < start || end > str.length) {
                                    DnclObject.IndexOutOfRangeError(
                                        start,
                                        str.length,
                                        astNode
                                    )
                                } else {
                                    DnclObject.String(str.substring(start, end), astNode)
                                }
                            }

                            else -> DnclObject.TypeError(
                                "第一引数は文字列、第二引数と第三引数は整数でなければなりません",
                                astNode
                            )
                        }
                    }

                    BuiltInFunction.SPLIT -> {
                        checkArgSize(2)?.let { return@Evaluator it }
                        when {
                            args[0] is DnclObject.String && args[1] is DnclObject.String -> {
                                val str = (args[0] as DnclObject.String).value
                                val delimiter = (args[1] as DnclObject.String).value
                                val parts = str.split(delimiter)
                                val result = parts.map {
                                    DnclObject.String(
                                        it,
                                        astNode
                                    ) as DnclObject
                                }
                                    .toMutableList()
                                DnclObject.Array(result, astNode)
                            }

                            else -> DnclObject.TypeError(
                                "第一引数と第二引数は文字列でなければなりません",
                                astNode
                            )
                        }
                    }

                    BuiltInFunction.TRIM -> {
                        checkArgSize(1)?.let { return@Evaluator it }
                        when (args[0]) {
                            is DnclObject.String -> {
                                val str = (args[0] as DnclObject.String).value
                                DnclObject.String(str.trim(), astNode)
                            }

                            else -> DnclObject.TypeError(
                                "第一引数は文字列でなければなりません",
                                astNode
                            )
                        }
                    }

                    BuiltInFunction.REPLACE -> {
                        checkArgSize(3)?.let { return@Evaluator it }
                        when {
                            args[0] is DnclObject.String && args[1] is DnclObject.String && args[2] is DnclObject.String -> {
                                val str = (args[0] as DnclObject.String).value
                                val oldValue = (args[1] as DnclObject.String).value
                                val newValue = (args[2] as DnclObject.String).value
                                DnclObject.String(str.replace(oldValue, newValue), astNode)
                            }

                            else -> DnclObject.TypeError(
                                "すべての引数は文字列でなければなりません",
                                astNode
                            )
                        }
                    }

                    // 数学関数
                    BuiltInFunction.ROUND -> {
                        checkArgSize(1)?.let { return@Evaluator it }
                        when (args[0]) {
                            is DnclObject.Float -> {
                                val value = (args[0] as DnclObject.Float).value
                                DnclObject.Int(
                                    kotlin.math.round(value.toDouble()).toInt(),
                                    astNode
                                )
                            }

                            is DnclObject.Int -> args[0]
                            else -> DnclObject.TypeError(
                                "第一引数は数値でなければなりません",
                                astNode
                            )
                        }
                    }

                    BuiltInFunction.FLOOR -> {
                        checkArgSize(1)?.let { return@Evaluator it }
                        when (args[0]) {
                            is DnclObject.Float -> {
                                val value = (args[0] as DnclObject.Float).value
                                DnclObject.Int(
                                    kotlin.math.floor(value.toDouble()).toInt(),
                                    astNode
                                )
                            }

                            is DnclObject.Int -> args[0]
                            else -> DnclObject.TypeError(
                                "第一引数は数値でなければなりません",
                                astNode
                            )
                        }
                    }

                    BuiltInFunction.CEIL -> {
                        checkArgSize(1)?.let { return@Evaluator it }
                        when (args[0]) {
                            is DnclObject.Float -> {
                                val value = (args[0] as DnclObject.Float).value
                                DnclObject.Int(
                                    kotlin.math.ceil(value.toDouble()).toInt(),
                                    astNode
                                )
                            }

                            is DnclObject.Int -> args[0]
                            else -> DnclObject.TypeError(
                                "第一引数は数値でなければなりません",
                                astNode
                            )
                        }
                    }

                    BuiltInFunction.RANDOM -> {
                        DnclObject.Float(kotlin.random.Random.nextFloat(), astNode)
                    }

                    BuiltInFunction.MAX -> {
                        if (args.isEmpty()) {
                            return@Evaluator DnclObject.ArgumentSizeError(
                                "引数が少ないです",
                                astNode
                            )
                        }

                        val allNumbers = args.all { it is DnclObject.Int || it is DnclObject.Float }
                        if (!allNumbers) {
                            return@Evaluator DnclObject.TypeError(
                                "すべての引数は数値でなければなりません",
                                astNode
                            )
                        }

                        val values = args.map {
                            when (it) {
                                is DnclObject.Int -> it.value.toFloat()
                                is DnclObject.Float -> it.value
                                else -> 0f
                            }
                        }

                        val maxValue = values.maxOrNull() ?: 0f
                        if (maxValue == maxValue.toInt().toFloat()) {
                            DnclObject.Int(maxValue.toInt(), args[0].astNode)
                        } else {
                            DnclObject.Float(maxValue, args[0].astNode)
                        }
                    }

                    BuiltInFunction.MIN -> {
                        if (args.isEmpty()) {
                            return@Evaluator DnclObject.ArgumentSizeError(
                                "引数が少ないです",
                                astNode
                            )
                        }

                        val allNumbers = args.all { it is DnclObject.Int || it is DnclObject.Float }
                        if (!allNumbers) {
                            return@Evaluator DnclObject.TypeError(
                                "すべての引数は数値でなければなりません",
                                astNode
                            )
                        }

                        val values = args.map {
                            when (it) {
                                is DnclObject.Int -> it.value.toFloat()
                                is DnclObject.Float -> it.value
                                else -> 0f
                            }
                        }

                        val minValue = values.minOrNull() ?: 0f
                        if (minValue == minValue.toInt().toFloat()) {
                            DnclObject.Int(minValue.toInt(), astNode)
                        } else {
                            DnclObject.Float(minValue, astNode)
                        }
                    }

                    // 型チェック関数
                    BuiltInFunction.IS_INT -> {
                        checkArgSize(1)?.let { return@Evaluator it }
                        DnclObject.Boolean(args[0] is DnclObject.Int, astNode)
                    }

                    BuiltInFunction.IS_FLOAT -> {
                        checkArgSize(1)?.let { return@Evaluator it }
                        DnclObject.Boolean(args[0] is DnclObject.Float, astNode)
                    }

                    BuiltInFunction.IS_STRING -> {
                        checkArgSize(1)?.let { return@Evaluator it }
                        DnclObject.Boolean(args[0] is DnclObject.String, astNode)
                    }

                    BuiltInFunction.IS_ARRAY -> {
                        checkArgSize(1)?.let { return@Evaluator it }
                        DnclObject.Boolean(args[0] is DnclObject.Array, astNode)
                    }

                    BuiltInFunction.IS_BOOLEAN -> {
                        checkArgSize(1)?.let { return@Evaluator it }
                        DnclObject.Boolean(args[0] is DnclObject.Boolean, astNode)
                    }

                    BuiltInFunction.CLEAR -> {
                        onClear()
                        DnclObject.Null(astNode)
                    }

                    BuiltInFunction.SLEEP -> {
                        checkArgSize(1)?.let { return@Evaluator it }
                        when (args[0]) {
                            is DnclObject.Int -> {
                                val milliseconds = (args[0] as DnclObject.Int).value.toLong()
                                delay(milliseconds)
                                DnclObject.Null(args[0].astNode)
                            }

                            is DnclObject.Float -> {
                                val milliseconds = (args[0] as DnclObject.Float).value.toLong()
                                delay(milliseconds)
                                DnclObject.Null(args[0].astNode)
                            }

                            else -> DnclObject.TypeError(
                                "第一引数は数値でなければなりません",
                                args[0].astNode
                            )
                        }
                    }
                }
            },
            {
                when (it) {
                    is SystemCommand.Input -> {
                        DnclObject.String(input, it.astNode)
                    }

                    is SystemCommand.Unknown -> {
                        DnclObject.Null(it.astNode)
                    }
                }
            }, arrayOrigin
        )
    }

    private fun CallBuiltInFunctionScope.checkArgSize(
        expectedSize: Int,
    ): DnclObject.ArgumentSizeError? {
        return if (args.size < expectedSize) DnclObject.ArgumentSizeError(
            "引数が少ないです",
            astNode
        )
        else if (args.size > expectedSize) DnclObject.ArgumentSizeError(
            "引数が多すぎます",
            astNode
        )
        else null
    }
}
