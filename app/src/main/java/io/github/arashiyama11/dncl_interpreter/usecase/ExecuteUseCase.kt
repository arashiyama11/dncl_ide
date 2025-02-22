package io.github.arashiyama11.dncl_interpreter.usecase

import android.util.Log
import arrow.core.getOrElse
import io.github.arashiyama11.data.repository.IFileRepository
import io.github.arashiyama11.dncl.evaluator.Evaluator
import io.github.arashiyama11.dncl.lexer.Lexer
import io.github.arashiyama11.dncl.model.BuiltInFunction
import io.github.arashiyama11.dncl.model.DnclObject
import io.github.arashiyama11.dncl.model.SystemCommand
import io.github.arashiyama11.dncl.parser.Parser
import io.github.arashiyama11.model.DnclOutput
import io.github.arashiyama11.model.FileName
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.channelFlow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withContext
import kotlinx.coroutines.withTimeoutOrNull
import org.koin.core.annotation.Single

@Single(binds = [IExecuteUseCase::class])
class ExecuteUseCase(private val fileRepository: IFileRepository) : IExecuteUseCase {
    override operator fun invoke(program: String, input: String): Flow<DnclOutput> {
        val parser = Parser(Lexer(program)).getOrElse { err ->
            return flowOf(
                DnclOutput.Error(
                    err.explain(program)
                )
            )
        }

        val ast = parser.parseProgram().getOrElse { err ->
            return flowOf(
                DnclOutput.Error(
                    err.explain(program)
                )
            )
        }
        return channelFlow {
            withContext(Dispatchers.Default) {
                val evaluator = getEvaluator(input) {
                    send(DnclOutput.Stdout(it))
                }

                evaluator.evalProgram(ast).let { err ->
                    if (err.isLeft()) {
                        send(DnclOutput.Error(err.leftOrNull()!!.message.orEmpty()))
                    } else if (err.getOrNull() is DnclObject.Error) {
                        val e = err.getOrNull()!! as DnclObject.Error
                        send(DnclOutput.RuntimeError(e))
                    }
                    close()
                }
            }
            awaitClose()
        }
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

    private fun getEvaluator(input: String, onStdout: suspend (String) -> Unit): Evaluator {
        return Evaluator(
            { fn, arg, env ->
                when (fn) {
                    BuiltInFunction.PRINT -> {
                        runBlocking {
                            withTimeoutOrNull(100) {
                                onStdout(arg.joinToString(" ") { it.toString() })
                            } ?: Log.e("ExecuteUseCase", "onStdout timeout")
                        }
                        DnclObject.Null(arg[0].astNode)
                    }

                    BuiltInFunction.LENGTH -> {
                        checkArgSize(arg, 1)?.let { return@Evaluator it }
                        when (arg[0]) {
                            is DnclObject.Array -> DnclObject.Int(
                                (arg[0] as DnclObject.Array).value.size,
                                arg[0].astNode
                            )

                            else -> DnclObject.TypeError(
                                "第一引数は配列でなければなりません",
                                arg[0].astNode
                            )
                        }
                    }

                    BuiltInFunction.DIFF -> {
                        checkArgSize(arg, 1)?.let { return@Evaluator it }
                        when (arg[0]) {
                            is DnclObject.String -> {
                                val str = (arg[0] as DnclObject.String).value
                                require(str.length == 1)
                                if (str == " ") DnclObject.Int(
                                    -1,
                                    arg[0].astNode
                                ) else DnclObject.Int(
                                    str[0].code - 'a'.code,
                                    arg[0].astNode
                                )
                            }

                            else -> DnclObject.TypeError(
                                "第一引数は文字列でなければなりません",
                                arg[0].astNode
                            )
                        }
                    }

                    BuiltInFunction.RETURN -> {
                        checkArgSize(arg, 1)?.let { return@Evaluator it }
                        DnclObject.ReturnValue(arg[0], arg[0].astNode)
                    }

                    BuiltInFunction.CONCAT -> {
                        require(arg.size == 2) {
                            Log.d("ExecuteUseCase", "concat error")
                            "concat error"
                        }
                        when {
                            arg[0] is DnclObject.Array && arg[1] is DnclObject.Array -> {
                                val a = (arg[0] as DnclObject.Array).value
                                val b = (arg[1] as DnclObject.Array).value
                                DnclObject.Array((a + b).toMutableList(), arg[0].astNode)
                            }

                            else -> DnclObject.TypeError(
                                "第一引数、第二引数ともに配列でなければなりません",
                                arg[0].astNode
                            )
                        }
                    }

                    BuiltInFunction.PUSH -> {
                        checkArgSize(arg, 2)?.let { return@Evaluator it }
                        when (arg[0]) {
                            is DnclObject.Array -> {
                                val a = (arg[0] as DnclObject.Array).value
                                val b = arg[1]
                                a.add(b)
                                DnclObject.Null(arg[0].astNode)
                            }

                            else -> DnclObject.TypeError(
                                "第一引数は配列でなければなりません",
                                arg[0].astNode
                            )
                        }
                    }

                    BuiltInFunction.SHIFT -> {
                        checkArgSize(arg, 1)?.let { return@Evaluator it }
                        when (arg[0]) {
                            is DnclObject.Array -> {
                                val a = (arg[0] as DnclObject.Array).value
                                if (a.isEmpty()) DnclObject.Null(arg[0].astNode) else a.removeAt(0)
                            }

                            else -> DnclObject.TypeError(
                                "第一引数は配列でなければなりません",
                                arg[0].astNode
                            )
                        }
                    }

                    BuiltInFunction.UNSHIFT -> {
                        checkArgSize(arg, 2)?.let { return@Evaluator it }
                        when (arg[0]) {
                            is DnclObject.Array -> {
                                val a = (arg[0] as DnclObject.Array).value
                                val b = arg[1]
                                a.add(0, b)
                                DnclObject.Null(arg[0].astNode)
                            }

                            else -> DnclObject.TypeError(
                                "第一引数は配列でなければなりません",
                                arg[0].astNode
                            )
                        }
                    }

                    BuiltInFunction.POP -> {
                        checkArgSize(arg, 1)?.let { return@Evaluator it }
                        when (arg[0]) {
                            is DnclObject.Array -> {
                                val a = (arg[0] as DnclObject.Array).value
                                if (a.isEmpty()) DnclObject.Null(arg[0].astNode) else a.removeAt(a.size - 1)
                            }

                            else -> DnclObject.TypeError(
                                "第一引数は配列でなければなりません",
                                arg[0].astNode
                            )
                        }
                    }

                    BuiltInFunction.INT -> {
                        checkArgSize(arg, 1)?.let { return@Evaluator it }
                        when (arg[0]) {
                            is DnclObject.String -> {
                                val str = (arg[0] as DnclObject.String).value
                                DnclObject.Int(str.toIntOrNull() ?: 0, arg[0].astNode)
                            }

                            is DnclObject.Float -> {
                                val flt = (arg[0] as DnclObject.Float).value
                                DnclObject.Int(flt.toInt(), arg[0].astNode)
                            }

                            is DnclObject.Int -> arg[0]

                            else -> DnclObject.TypeError(
                                "文字列,小数,整数でなければなりません",
                                arg[0].astNode
                            )
                        }
                    }

                    BuiltInFunction.FLOAT -> {
                        checkArgSize(arg, 1)?.let { return@Evaluator it }
                        when (arg[0]) {
                            is DnclObject.String -> {
                                val str = (arg[0] as DnclObject.String).value
                                DnclObject.Float(str.toFloatOrNull() ?: 0f, arg[0].astNode)
                            }

                            is DnclObject.Int -> {
                                val int = (arg[0] as DnclObject.Int).value
                                DnclObject.Float(int.toFloat(), arg[0].astNode)
                            }

                            is DnclObject.Float -> arg[0]

                            else -> DnclObject.TypeError(
                                "文字列,小数,整数でなければなりません",
                                arg[0].astNode
                            )
                        }
                    }

                    BuiltInFunction.STRING -> {
                        checkArgSize(arg, 1)?.let { return@Evaluator it }
                        DnclObject.String(arg[0].toString(), arg[0].astNode)
                    }

                    BuiltInFunction.IMPORT -> {
                        checkArgSize(arg, 1)?.let { return@Evaluator it }
                        when (arg[0]) {
                            is DnclObject.String -> {
                                val str = (arg[0] as DnclObject.String).value
                                val file = runBlocking {
                                    withTimeoutOrNull(100) {
                                        fileRepository.getFileByName(FileName(str))
                                    }
                                }
                                if (file == null) {
                                    return@Evaluator DnclObject.RuntimeError(
                                        "ファイル:$str が見つかりません",
                                        arg[0].astNode
                                    )
                                } else {
                                    val parser =
                                        Parser(Lexer(file.content.value)).getOrElse { err ->
                                            return@Evaluator DnclObject.RuntimeError(
                                                err.explain(file.content.value),
                                                arg[0].astNode
                                            )
                                        }

                                    val prog = parser.parseProgram().getOrElse { err ->
                                        return@Evaluator DnclObject.RuntimeError(
                                            err.explain(file.content.value),
                                            arg[0].astNode
                                        )
                                    }
                                    evalProgram(prog, env)
                                }
                            }

                            else -> return@Evaluator DnclObject.TypeError("", arg[0].astNode)
                        }
                        DnclObject.Null(arg[0].astNode)
                    }

                    BuiltInFunction.CHAR_CODE -> {
                        checkArgSize(arg, 1)?.let { return@Evaluator it }
                        when (arg[0]) {
                            is DnclObject.String -> {
                                val str = (arg[0] as DnclObject.String).value
                                if (str.length != 1) return@Evaluator DnclObject.RuntimeError(
                                    "文字列の長さは1でなければなりません",
                                    arg[0].astNode
                                )
                                DnclObject.Int(str[0].code, arg[0].astNode)
                            }

                            else -> DnclObject.TypeError(
                                "第一引数は文字列でなければなりません",
                                arg[0].astNode
                            )
                        }
                    }

                    BuiltInFunction.FROM_CHAR_CODE -> {
                        checkArgSize(arg, 1)?.let { return@Evaluator it }
                        when (arg[0]) {
                            is DnclObject.Int -> {
                                val int = (arg[0] as DnclObject.Int).value
                                DnclObject.String(int.toChar().toString(), arg[0].astNode)
                            }

                            else -> DnclObject.TypeError(
                                "第一引数は正数でなければなりません",
                                arg[0].astNode
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
            }, 0
        )
    }
}