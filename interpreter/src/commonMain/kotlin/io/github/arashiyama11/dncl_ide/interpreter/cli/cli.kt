package io.github.arashiyama11.dncl_ide.interpreter.cli

import io.github.arashiyama11.dncl_ide.interpreter.api.Stdout
import io.github.arashiyama11.dncl_ide.interpreter.evaluator.EvaluatorFactory
import io.github.arashiyama11.dncl_ide.interpreter.lexer.Lexer
import io.github.arashiyama11.dncl_ide.interpreter.model.AstNode
import io.github.arashiyama11.dncl_ide.interpreter.model.DnclObject
import io.github.arashiyama11.dncl_ide.interpreter.parser.Parser
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.coroutineScope


suspend fun mainEntry(args: Array<String>) {
    runCli(args)
}

suspend fun runCli(args: Array<String>) {
    if (args.isEmpty()) {
        // stdin にデータが来ているかチェック（パイプかどうか等）
        if (stdinHasData()) {
            val sourceCode = readAllStdin()
            executeSourceCode(sourceCode)
        } else {
            runRepl()
        }
    } else {
        val filePath = args[0]
        if (!fileExists(filePath)) {
            stderrPrintln("エラー: ファイルが見つかりません: $filePath")
            exitProcess(1)
        }
        val sourceCode = readFileText(filePath)
        executeSourceCode(sourceCode)
    }
}

suspend fun executeSourceCode(sourceCode: String) = coroutineScope {
    // ここは元の処理に合わせて調整してください
    val lexer = Lexer(sourceCode)
    val parser = Parser(lexer).fold(
        { error ->
            stderrPrintln("構文解析エラー: ${error.explain(sourceCode)}")
            exitProcess(1)
        },
        { it }
    )

    val program = parser.parseProgram().fold(
        { error ->
            stderrPrintln("構文解析エラー: ${error.explain(sourceCode)}")
            exitProcess(1)
        },
        { it }
    )

    val inputChannel = Channel<String>()

    val evaluator = EvaluatorFactory.create(
        inputChannel = inputChannel,
        arrayOrigin = 0,
        onEval = null
    )

    val env = EvaluatorFactory.createBuiltInFunctionEnvironment(
        stdout = StdoutImpl,
        onImport = { path ->
            stderrPrintln("REPLモードではimportはサポートされていません: $path")
            DnclObject.RuntimeError(
                "REPLモードではimportはサポートされていません: $path",
                AstNode.Program(emptyList())
            )
        }
    )

    // 入力行を別コルーチンで読み取ってチャネルに流す（プラットフォーム依存実装）
    val stdinReaderJob = startStdinLineReader(inputChannel)

    val result = evaluator.eval(program, env)

    result.fold(
        { error ->
            stderrPrintln("実行時エラー: ${error.explain(sourceCode)}")
        },
        { dnclObject ->
            if (dnclObject !is DnclObject.Nothing && dnclObject !is DnclObject.Null) {
                println(dnclObject)
            }
        }
    )

    inputChannel.close()
    // stdinReaderJob をキャンセルして終了（必要なら）
    stdinReaderJob?.cancel()
}

suspend fun runRepl(): Nothing = coroutineScope {
    println("DNCL REPL (終了するには 'exit' または 'quit' と入力してください)")
    val inputChannel = Channel<String>()

    val env = EvaluatorFactory.createBuiltInFunctionEnvironment(
        stdout = StdoutImpl,
        onImport = { path ->
            stderrPrintln("REPLモードではimportはサポートされていません: $path")
            DnclObject.RuntimeError(
                "REPLモードではimportはサポートされていません: $path",
                AstNode.Program(emptyList())
            )
        }
    )

    // バックグラウンドで stdin を読み、inputChannel に流す（プラットフォーム実装）
    //val stdinReaderJob = startStdinLineReader(inputChannel)

    var currentInput = ""
    var prompt = ">>> "

    while (true) {
        print(prompt)
        // 標準入力から行を受け取る（ここではブロッキングに readLine() を使ってもOK）
        val line = readlnOrNull() ?: break

        if (line.lowercase() == "exit" || line.lowercase() == "quit") break

        val isBlankLine = line.trim().isEmpty()
        if (!isBlankLine) currentInput += line + "\n"

        val lexer = Lexer(currentInput)
        val parserResult = Parser(lexer)

        parserResult.fold(
            { error ->
                stderrPrintln("構文解析エラー: ${error.explain(currentInput)}")
                currentInput = ""
                prompt = ">>> "
            },
            { parser ->
                val programResult = parser.parseProgram()
                programResult.fold(
                    { error ->
                        val errorMessage = error.explain(currentInput)
                        val incompleteMarkers = listOf("EOF", "expected", "unclosed")
                        if (isBlankLine || incompleteMarkers.none { errorMessage.contains(it) }) {
                            stderrPrintln("構文解析エラー: $errorMessage")
                            currentInput = ""
                            prompt = ">>> "
                        } else {
                            prompt = "... "
                        }
                    },
                    { program ->
                        // 実行
                        val evaluator = EvaluatorFactory.create(
                            inputChannel = inputChannel,
                            arrayOrigin = 0,
                            onEval = null
                        )
                        val result = evaluator.eval(program, env)
                        result.fold(
                            { error -> stderrPrintln("実行時エラー: ${error.explain(currentInput)}") },
                            { dnclObject ->
                                if (dnclObject !is DnclObject.Nothing && dnclObject !is DnclObject.Null) {
                                    println(dnclObject)
                                }
                            }
                        )
                        currentInput = ""
                        prompt = ">>> "
                    }
                )
            }
        )

        if (isBlankLine && currentInput.trim().isEmpty()) continue
    }

    inputChannel.close()
    // stdinReaderJob?.cancel()
    exitProcess(0)
}

object StdoutImpl : Stdout {
    override suspend fun append(text: String) {
        print(text)
    }

    override suspend fun flush() {
        println("")
    }

    override suspend fun clear() {}
    override suspend fun commitFrame() {}
    override suspend fun replace(text: String) {
        print(text)
    }
}
