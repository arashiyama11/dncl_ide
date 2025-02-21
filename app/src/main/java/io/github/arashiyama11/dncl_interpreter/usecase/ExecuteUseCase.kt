package io.github.arashiyama11.dncl_interpreter.usecase

import android.util.Log
import arrow.core.getOrElse
import io.github.arashiyama11.dncl.evaluator.Evaluator
import io.github.arashiyama11.dncl.lexer.Lexer
import io.github.arashiyama11.dncl.model.BuiltInFunction
import io.github.arashiyama11.dncl.model.DnclObject
import io.github.arashiyama11.dncl.model.SystemCommand
import io.github.arashiyama11.dncl.parser.Parser
import io.github.arashiyama11.model.DnclOutput
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
class ExecuteUseCase : IExecuteUseCase {
    override operator fun invoke(program: String): Flow<DnclOutput> {
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
                val evaluator = getEvaluator {
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

    private fun getEvaluator(onStdout: suspend (String) -> Unit): Evaluator {
        return Evaluator(
            { fn, arg ->
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
                        require(arg.size == 1)
                        when (arg[0]) {
                            is DnclObject.Array -> DnclObject.Int(
                                (arg[0] as DnclObject.Array).value.size,
                                arg[0].astNode
                            )

                            else -> DnclObject.TypeError("", arg[0].astNode)
                        }
                    }

                    BuiltInFunction.DIFF -> {
                        require(arg.size == 1)
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

                            else -> DnclObject.TypeError("", arg[0].astNode)
                        }
                    }

                    BuiltInFunction.RETURN -> {
                        require(arg.size == 1)
                        DnclObject.ReturnValue(arg[0], arg[0].astNode)
                    }
                }
            },
            {
                when (it) {
                    is SystemCommand.Input -> {
                        DnclObject.Null(it.astNode)
                    }

                    is SystemCommand.Unknown -> {
                        println("input: ${it.command}")
                        DnclObject.Null(it.astNode)
                    }
                }
            }, 0
        )
    }
}