package io.github.arashiyama11.dncl_ide.interpreter.evaluator

import io.github.arashiyama11.dncl_ide.interpreter.model.AstNode
import io.github.arashiyama11.dncl_ide.interpreter.model.BuiltInFunction
import io.github.arashiyama11.dncl_ide.interpreter.model.DnclObject
import io.github.arashiyama11.dncl_ide.interpreter.model.SystemCommand
import kotlinx.coroutines.delay

object EvaluatorFactory {
    fun create(
        input: String,
        arrayOrigin: Int,
        onStdout: suspend (String) -> Unit,
        onClear: suspend () -> Unit = {},
        onImport: suspend CallBuiltInFunctionScope.(String) -> DnclObject
    ): Evaluator {
        return Evaluator(
            {
                when (fn) {
                    BuiltInFunction.PRINT -> {
                        onStdout(args.joinToString(" ") { it.toString() })
                        DnclObject.Null(args[0].astNode)
                    }

                    BuiltInFunction.LENGTH -> {
                        checkArgSize(this.args, 1)?.let { return@Evaluator it }
                        when (args[0]) {
                            is DnclObject.Array -> DnclObject.Int(
                                (args[0] as DnclObject.Array).value.size,
                                args[0].astNode
                            )

                            else -> DnclObject.TypeError(
                                "第一引数は配列でなければなりません",
                                args[0].astNode
                            )
                        }
                    }

                    BuiltInFunction.DIFF -> {
                        checkArgSize(this.args, 1)?.let { return@Evaluator it }
                        when (args[0]) {
                            is DnclObject.String -> {
                                val str = (args[0] as DnclObject.String).value
                                require(str.length == 1)
                                if (str == " ") DnclObject.Int(
                                    -1,
                                    args[0].astNode
                                ) else DnclObject.Int(
                                    str[0].code - 'a'.code,
                                    args[0].astNode
                                )
                            }

                            else -> DnclObject.TypeError(
                                "第一引数は文字列でなければなりません",
                                args[0].astNode
                            )
                        }
                    }

                    BuiltInFunction.RETURN -> {
                        checkArgSize(args, 1)?.let { return@Evaluator it }
                        DnclObject.ReturnValue(args[0], args[0].astNode)
                    }

                    BuiltInFunction.CONCAT -> {
                        require(args.size == 2) {
                            "concat error"
                        }
                        when {
                            args[0] is DnclObject.Array && args[1] is DnclObject.Array -> {
                                val a = (args[0] as DnclObject.Array).value
                                val b = (args[1] as DnclObject.Array).value
                                DnclObject.Array((a + b).toMutableList(), args[0].astNode)
                            }

                            else -> DnclObject.TypeError(
                                "第一引数、第二引数ともに配列でなければなりません",
                                args[0].astNode
                            )
                        }
                    }

                    BuiltInFunction.PUSH -> {
                        checkArgSize(args, 2)?.let { return@Evaluator it }
                        when (args[0]) {
                            is DnclObject.Array -> {
                                val a = (args[0] as DnclObject.Array).value
                                val b = args[1]
                                a.add(b)
                                DnclObject.Null(args[0].astNode)
                            }

                            else -> DnclObject.TypeError(
                                "第一引数は配列でなければなりません",
                                args[0].astNode
                            )
                        }
                    }

                    BuiltInFunction.SHIFT -> {
                        checkArgSize(args, 1)?.let { return@Evaluator it }
                        when (args[0]) {
                            is DnclObject.Array -> {
                                val a = (args[0] as DnclObject.Array).value
                                if (a.isEmpty()) DnclObject.Null(args[0].astNode) else a.removeAt(0)
                            }

                            else -> DnclObject.TypeError(
                                "第一引数は配列でなければなりません",
                                args[0].astNode
                            )
                        }
                    }

                    BuiltInFunction.UNSHIFT -> {
                        checkArgSize(args, 2)?.let { return@Evaluator it }
                        when (args[0]) {
                            is DnclObject.Array -> {
                                val a = (args[0] as DnclObject.Array).value
                                val b = args[1]
                                a.add(0, b)
                                DnclObject.Null(args[0].astNode)
                            }

                            else -> DnclObject.TypeError(
                                "第一引数は配列でなければなりません",
                                args[0].astNode
                            )
                        }
                    }

                    BuiltInFunction.POP -> {
                        checkArgSize(args, 1)?.let { return@Evaluator it }
                        when (args[0]) {
                            is DnclObject.Array -> {
                                val a = (args[0] as DnclObject.Array).value
                                if (a.isEmpty()) DnclObject.Null(args[0].astNode) else a.removeAt(a.size - 1)
                            }

                            else -> DnclObject.TypeError(
                                "第一引数は配列でなければなりません",
                                args[0].astNode
                            )
                        }
                    }

                    BuiltInFunction.INT -> {
                        checkArgSize(args, 1)?.let { return@Evaluator it }
                        when (args[0]) {
                            is DnclObject.String -> {
                                val str = (args[0] as DnclObject.String).value
                                DnclObject.Int(str.toIntOrNull() ?: 0, args[0].astNode)
                            }

                            is DnclObject.Float -> {
                                val flt = (args[0] as DnclObject.Float).value
                                DnclObject.Int(flt.toInt(), args[0].astNode)
                            }

                            is DnclObject.Int -> args[0]

                            else -> DnclObject.TypeError(
                                "文字列,小数,整数でなければなりません",
                                args[0].astNode
                            )
                        }
                    }

                    BuiltInFunction.FLOAT -> {
                        checkArgSize(args, 1)?.let { return@Evaluator it }
                        when (args[0]) {
                            is DnclObject.String -> {
                                val str = (args[0] as DnclObject.String).value
                                DnclObject.Float(str.toFloatOrNull() ?: 0f, args[0].astNode)
                            }

                            is DnclObject.Int -> {
                                val int = (args[0] as DnclObject.Int).value
                                DnclObject.Float(int.toFloat(), args[0].astNode)
                            }

                            is DnclObject.Float -> args[0]

                            else -> DnclObject.TypeError(
                                "文字列,小数,整数でなければなりません",
                                args[0].astNode
                            )
                        }
                    }

                    BuiltInFunction.STRING -> {
                        checkArgSize(args, 1)?.let { return@Evaluator it }
                        DnclObject.String(args[0].toString(), args[0].astNode)
                    }

                    BuiltInFunction.IMPORT -> {
                        checkArgSize(args, 1)?.let { return@Evaluator it }
                        when (args[0]) {
                            is DnclObject.String -> {
                                onImport((args[0] as DnclObject.String).value)
                            }

                            else -> return@Evaluator DnclObject.TypeError("", args[0].astNode)
                        }
                    }

                    BuiltInFunction.CHAR_CODE -> {
                        checkArgSize(args, 1)?.let { return@Evaluator it }
                        when (args[0]) {
                            is DnclObject.String -> {
                                val str = (args[0] as DnclObject.String).value
                                if (str.length != 1) return@Evaluator DnclObject.RuntimeError(
                                    "文字列の長さは1でなければなりません",
                                    args[0].astNode
                                )
                                DnclObject.Int(str[0].code, args[0].astNode)
                            }

                            else -> DnclObject.TypeError(
                                "第一引数は文字列でなければなりません",
                                args[0].astNode
                            )
                        }
                    }

                    BuiltInFunction.FROM_CHAR_CODE -> {
                        checkArgSize(args, 1)?.let { return@Evaluator it }
                        when (args[0]) {
                            is DnclObject.Int -> {
                                val int = (args[0] as DnclObject.Int).value
                                DnclObject.String(int.toChar().toString(), args[0].astNode)
                            }

                            else -> DnclObject.TypeError(
                                "第一引数は正数でなければなりません",
                                args[0].astNode
                            )
                        }
                    }

                    // 配列操作関数
                    BuiltInFunction.SLICE -> {
                        checkArgSize(args, 3)?.let { return@Evaluator it }
                        when {
                            args[0] is DnclObject.Array && args[1] is DnclObject.Int && args[2] is DnclObject.Int -> {
                                val array = (args[0] as DnclObject.Array).value
                                val start = (args[1] as DnclObject.Int).value
                                val end = (args[2] as DnclObject.Int).value
                                if (start < 0 || start >= array.size || end < start || end > array.size) {
                                    DnclObject.IndexOutOfRangeError(
                                        start,
                                        array.size,
                                        args[0].astNode
                                    )
                                } else {
                                    DnclObject.Array(
                                        array.subList(start, end).toMutableList(),
                                        args[0].astNode
                                    )
                                }
                            }

                            else -> DnclObject.TypeError(
                                "第一引数は配列、第二引数と第三引数は整数でなければなりません",
                                args[0].astNode
                            )
                        }
                    }

                    BuiltInFunction.JOIN -> {
                        checkArgSize(args, 2)?.let { return@Evaluator it }
                        when {
                            args[0] is DnclObject.Array && args[1] is DnclObject.String -> {
                                val array = (args[0] as DnclObject.Array).value
                                val separator = (args[1] as DnclObject.String).value
                                DnclObject.String(
                                    array.joinToString(separator) { it.toString() },
                                    args[0].astNode
                                )
                            }

                            else -> DnclObject.TypeError(
                                "第一引数は配列、第二引数は文字列でなければなりません",
                                args[0].astNode
                            )
                        }
                    }

                    BuiltInFunction.SORT -> {
                        checkArgSize(args, 1)?.let { return@Evaluator it }
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
                                DnclObject.Array(sortedArray, args[0].astNode)
                            }

                            else -> DnclObject.TypeError(
                                "第一引数は配列でなければなりません",
                                args[0].astNode
                            )
                        }
                    }

                    BuiltInFunction.REVERSE -> {
                        checkArgSize(args, 1)?.let { return@Evaluator it }
                        when (args[0]) {
                            is DnclObject.Array -> {
                                val array = (args[0] as DnclObject.Array).value
                                DnclObject.Array(array.reversed().toMutableList(), args[0].astNode)
                            }

                            else -> DnclObject.TypeError(
                                "第一引数は配列でなければなりません",
                                args[0].astNode
                            )
                        }
                    }

                    BuiltInFunction.FIND -> {
                        checkArgSize(args, 2)?.let { return@Evaluator it }
                        when (args[0]) {
                            is DnclObject.Array -> {
                                val array = (args[0] as DnclObject.Array).value
                                val target = args[1]
                                val index =
                                    array.indexOfFirst { it.toString() == target.toString() }
                                DnclObject.Int(index, args[0].astNode)
                            }

                            else -> DnclObject.TypeError(
                                "第一引数は配列でなければなりません",
                                args[0].astNode
                            )
                        }
                    }

                    // 文字列操作関数
                    BuiltInFunction.SUBSTRING -> {
                        checkArgSize(args, 3)?.let { return@Evaluator it }
                        when {
                            args[0] is DnclObject.String && args[1] is DnclObject.Int && args[2] is DnclObject.Int -> {
                                val str = (args[0] as DnclObject.String).value
                                val start = (args[1] as DnclObject.Int).value
                                val end = (args[2] as DnclObject.Int).value
                                if (start < 0 || start >= str.length || end < start || end > str.length) {
                                    DnclObject.IndexOutOfRangeError(
                                        start,
                                        str.length,
                                        args[0].astNode
                                    )
                                } else {
                                    DnclObject.String(str.substring(start, end), args[0].astNode)
                                }
                            }

                            else -> DnclObject.TypeError(
                                "第一引数は文字列、第二引数と第三引数は整数でなければなりません",
                                args[0].astNode
                            )
                        }
                    }

                    BuiltInFunction.SPLIT -> {
                        checkArgSize(args, 2)?.let { return@Evaluator it }
                        when {
                            args[0] is DnclObject.String && args[1] is DnclObject.String -> {
                                val str = (args[0] as DnclObject.String).value
                                val delimiter = (args[1] as DnclObject.String).value
                                val parts = str.split(delimiter)
                                val result = parts.map {
                                    DnclObject.String(
                                        it,
                                        args[0].astNode
                                    ) as DnclObject
                                }
                                    .toMutableList()
                                DnclObject.Array(result, args[0].astNode)
                            }

                            else -> DnclObject.TypeError(
                                "第一引数と第二引数は文字列でなければなりません",
                                args[0].astNode
                            )
                        }
                    }

                    BuiltInFunction.TRIM -> {
                        checkArgSize(args, 1)?.let { return@Evaluator it }
                        when (args[0]) {
                            is DnclObject.String -> {
                                val str = (args[0] as DnclObject.String).value
                                DnclObject.String(str.trim(), args[0].astNode)
                            }

                            else -> DnclObject.TypeError(
                                "第一引数は文字列でなければなりません",
                                args[0].astNode
                            )
                        }
                    }

                    BuiltInFunction.REPLACE -> {
                        checkArgSize(args, 3)?.let { return@Evaluator it }
                        when {
                            args[0] is DnclObject.String && args[1] is DnclObject.String && args[2] is DnclObject.String -> {
                                val str = (args[0] as DnclObject.String).value
                                val oldValue = (args[1] as DnclObject.String).value
                                val newValue = (args[2] as DnclObject.String).value
                                DnclObject.String(str.replace(oldValue, newValue), args[0].astNode)
                            }

                            else -> DnclObject.TypeError(
                                "すべての引数は文字列でなければなりません",
                                args[0].astNode
                            )
                        }
                    }

                    // 数学関数
                    BuiltInFunction.ROUND -> {
                        checkArgSize(args, 1)?.let { return@Evaluator it }
                        when (args[0]) {
                            is DnclObject.Float -> {
                                val value = (args[0] as DnclObject.Float).value
                                DnclObject.Int(
                                    kotlin.math.round(value.toDouble()).toInt(),
                                    args[0].astNode
                                )
                            }

                            is DnclObject.Int -> args[0]
                            else -> DnclObject.TypeError(
                                "第一引数は数値でなければなりません",
                                args[0].astNode
                            )
                        }
                    }

                    BuiltInFunction.FLOOR -> {
                        checkArgSize(args, 1)?.let { return@Evaluator it }
                        when (args[0]) {
                            is DnclObject.Float -> {
                                val value = (args[0] as DnclObject.Float).value
                                DnclObject.Int(
                                    kotlin.math.floor(value.toDouble()).toInt(),
                                    args[0].astNode
                                )
                            }

                            is DnclObject.Int -> args[0]
                            else -> DnclObject.TypeError(
                                "第一引数は数値でなければなりません",
                                args[0].astNode
                            )
                        }
                    }

                    BuiltInFunction.CEIL -> {
                        checkArgSize(args, 1)?.let { return@Evaluator it }
                        when (args[0]) {
                            is DnclObject.Float -> {
                                val value = (args[0] as DnclObject.Float).value
                                DnclObject.Int(
                                    kotlin.math.ceil(value.toDouble()).toInt(),
                                    args[0].astNode
                                )
                            }

                            is DnclObject.Int -> args[0]
                            else -> DnclObject.TypeError(
                                "第一引数は数値でなければなりません",
                                args[0].astNode
                            )
                        }
                    }

                    BuiltInFunction.RANDOM -> {
                        // For random function with no arguments, we need a valid AstNode
                        // Use a system literal if args is empty
                        val astNode =
                            if (args.isEmpty()) AstNode.SystemLiteral("", 0..0) else args[0].astNode
                        DnclObject.Float(kotlin.random.Random.nextFloat(), astNode)
                    }

                    BuiltInFunction.MAX -> {
                        if (args.isEmpty()) {
                            return@Evaluator DnclObject.ArgumentSizeError(
                                "引数が少ないです",
                                args[0].astNode
                            )
                        }

                        val allNumbers = args.all { it is DnclObject.Int || it is DnclObject.Float }
                        if (!allNumbers) {
                            return@Evaluator DnclObject.TypeError(
                                "すべての引数は数値でなければなりません",
                                args[0].astNode
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
                                args[0].astNode
                            )
                        }

                        val allNumbers = args.all { it is DnclObject.Int || it is DnclObject.Float }
                        if (!allNumbers) {
                            return@Evaluator DnclObject.TypeError(
                                "すべての引数は数値でなければなりません",
                                args[0].astNode
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
                            DnclObject.Int(minValue.toInt(), args[0].astNode)
                        } else {
                            DnclObject.Float(minValue, args[0].astNode)
                        }
                    }

                    // 型チェック関数
                    BuiltInFunction.IS_INT -> {
                        checkArgSize(args, 1)?.let { return@Evaluator it }
                        DnclObject.Boolean(args[0] is DnclObject.Int, args[0].astNode)
                    }

                    BuiltInFunction.IS_FLOAT -> {
                        checkArgSize(args, 1)?.let { return@Evaluator it }
                        DnclObject.Boolean(args[0] is DnclObject.Float, args[0].astNode)
                    }

                    BuiltInFunction.IS_STRING -> {
                        checkArgSize(args, 1)?.let { return@Evaluator it }
                        DnclObject.Boolean(args[0] is DnclObject.String, args[0].astNode)
                    }

                    BuiltInFunction.IS_ARRAY -> {
                        checkArgSize(args, 1)?.let { return@Evaluator it }
                        DnclObject.Boolean(args[0] is DnclObject.Array, args[0].astNode)
                    }

                    BuiltInFunction.IS_BOOLEAN -> {
                        checkArgSize(args, 1)?.let { return@Evaluator it }
                        DnclObject.Boolean(args[0] is DnclObject.Boolean, args[0].astNode)
                    }

                    BuiltInFunction.CLEAR -> {
                        onClear()
                        // Use a valid AstNode from args if available, otherwise create a system literal
                        val astNode =
                            if (args.isNotEmpty()) args[0].astNode else AstNode.SystemLiteral(
                                "",
                                0..0
                            )
                        DnclObject.Null(astNode)
                    }

                    BuiltInFunction.SLEEP -> {
                        checkArgSize(args, 1)?.let { return@Evaluator it }
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

    private fun checkArgSize(
        arg: List<DnclObject>,
        expectedSize: Int,
    ): DnclObject.ArgumentSizeError? {
        return if (arg.size < expectedSize) DnclObject.ArgumentSizeError(
            "引数が少ないです",
            arg[0].astNode
        )
        else if (arg.size > expectedSize) DnclObject.ArgumentSizeError(
            "引数が多すぎます",
            arg[0].astNode
        )
        else null
    }
}
