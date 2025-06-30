package io.github.arashiyama11.dncl_ide.interpreter

import io.github.arashiyama11.dncl_ide.interpreter.lexer.Lexer
import io.github.arashiyama11.dncl_ide.interpreter.parser.Parser
import kotlinx.coroutines.runBlocking
import java.io.File
import java.util.Scanner
import io.github.arashiyama11.dncl_ide.interpreter.model.DnclObject
import kotlin.system.exitProcess
import io.github.arashiyama11.dncl_ide.interpreter.evaluator.EvaluatorFactory
import kotlinx.coroutines.DelicateCoroutinesApi
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.launch

@OptIn(DelicateCoroutinesApi::class)
fun main(args: Array<String>): Unit = runBlocking {
    if (args.isEmpty()) {
        System.err.println("使用法: dncl <ファイルパス>")
        exitProcess(1)
    }

    val filePath = args[0]
    val file = File(filePath)

    if (!file.exists()) {
        System.err.println("エラー: ファイルが見つかりません: $filePath")
        exitProcess(1)
    }

    val sourceCode = file.readText()

    val lexer = Lexer(sourceCode)
    val parser = Parser(lexer).fold(
        { error ->
            System.err.println("構文解析エラー: ${error.explain(sourceCode)}")
            exitProcess(1)
        },
        { it }
    )

    val program = parser.parseProgram().fold(
        { error ->
            System.err.println("構文解析エラー: ${error.explain(sourceCode)}")
            exitProcess(1)
        },
        { it }
    )

    val inputChannel = Channel<String>()

    val evaluator = EvaluatorFactory.create(
        inputChannel = inputChannel,
        arrayOrigin = 0, // 必要に応じて変更
        onEval = null // CLIでは不要
    )

    val env = EvaluatorFactory.createBuiltInFunctionEnvironment(
        onStdout = { output ->
            println(output)
        },
        onImport = { path ->
            // TODO: import機能の実装
            DnclObject.Nothing(program) // InternalErrorの代わりにDnclObject.Nothingを返す
        }
    )

    // SystemCommand.Inputが呼ばれたときに標準入力から読み取るためのコルーチンを起動
    launch {
        val scanner = Scanner(System.`in`)
        while (true) {
            if (inputChannel.isClosedForSend) break
            val line = scanner.nextLine()
            inputChannel.send(line)
        }
    }

    val result = evaluator.eval(program, env)

    result.fold(
        { error ->
            System.err.println("実行時エラー: ${error.explain(sourceCode)}")
        },
        { dnclObject ->
            if (dnclObject !is DnclObject.Nothing && dnclObject !is DnclObject.Null) {
                println(dnclObject)
            }
        }
    )
    inputChannel.close()
}