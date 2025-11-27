package io.github.arashiyama11.dncl_ide.interpreter.cli

import dncl_ide.interpreter.BuildConfig
import io.github.arashiyama11.dncl_ide.interpreter.api.Stdout
import io.github.arashiyama11.dncl_ide.interpreter.evaluator.EvaluatorFactory
import io.github.arashiyama11.dncl_ide.interpreter.lexer.Lexer
import io.github.arashiyama11.dncl_ide.interpreter.model.AstNode
import io.github.arashiyama11.dncl_ide.interpreter.model.DnclObject
import io.github.arashiyama11.dncl_ide.interpreter.parser.Parser
import io.github.arashiyama11.dncl_ide.interpreter.preprocessor.preProcess
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.flow.toList

suspend fun mainEntry(args: Array<String>) = runCli(args)


fun resolveLib(path: String): String {
    TODO()
}

suspend fun runCli(args: Array<String>) {
    when (val result = parseCliOptions(args)) {
        is CliParseResult.Help -> {
            println(result.message)
            exitProcess(0)
        }

        is CliParseResult.Error -> {
            stderrPrintln(result.message)
            exitProcess(result.exitCode)
        }

        is CliParseResult.Version -> {
            println(result.version)
            exitProcess(0)
        }

        is CliParseResult.Success -> {
            val options = result.options
            when {
                options.scriptPath != null -> runScript(options.scriptPath)
                options.forceStdin -> runStdinOnly()
                options.forceRepl -> runRepl()
                else -> runStdinOrRepl()
            }
        }
    }
}

private suspend fun runScript(filePath: String) {
    if (!fileExists(filePath)) {
        stderrPrintln("エラー: ファイルが見つかりません: $filePath")
        exitProcess(1)
    }
    val sourceCode = readFileText(filePath)
    executeSourceCode(sourceCode)
}

private suspend fun runStdinOnly() {
    val sourceCode = readAllStdin()
    if (sourceCode.isBlank()) {
        stderrPrintln("エラー: 標準入力にコードがありません (--stdin)")
        exitProcess(1)
    }
    executeSourceCode(sourceCode)
}

private suspend fun runStdinOrRepl() {
    if (stdinHasData()) {
        val sourceCode = readAllStdin()
        executeSourceCode(sourceCode)
    } else {
        runRepl()
    }
}

suspend fun executeSourceCode(sourceCode: String) = coroutineScope {
    val tokens = preProcess(Lexer(sourceCode), ::resolveLib).toList()
    val parser = Parser(tokens).fold(
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
    println("DNCL REPL v${BuildConfig.DNCL_VERSION} (終了するには 'exit' または 'quit' と入力してください)")
    val inputChannel = Channel<String>()

    val env = EvaluatorFactory.createBuiltInFunctionEnvironment(
        stdout = StdoutImpl,
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

        val tokens = preProcess(Lexer(currentInput), ::resolveLib).toList()
        val parserResult = Parser(tokens)

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

private data class CliOptions(
    val scriptPath: String?,
    val forceRepl: Boolean,
    val forceStdin: Boolean
)

private sealed interface CliParseResult {
    data class Success(val options: CliOptions) : CliParseResult
    data class Help(val message: String) : CliParseResult
    data class Error(val message: String, val exitCode: Int = 1) : CliParseResult
    data class Version(val version: String) : CliParseResult
}

private fun parseCliOptions(args: Array<String>): CliParseResult {
    if (args.isEmpty()) return CliParseResult.Success(
        CliOptions(
            scriptPath = null,
            forceRepl = false,
            forceStdin = false
        )
    )

    var forceRepl = false
    var forceStdin = false
    var scriptPath: String? = null
    var parsingOptions = true
    var showVersion = false

    for (arg in args) {
        if (parsingOptions) {
            when (arg) {
                "-h", "--help" -> return CliParseResult.Help(helpMessage())
                "-v", "--version" -> {
                    showVersion = true
                    continue
                }

                "-r", "--repl" -> {
                    forceRepl = true
                    continue
                }

                "-s", "--stdin" -> {
                    forceStdin = true
                    continue
                }

                "--" -> {
                    parsingOptions = false
                    continue
                }
            }
            if (arg.startsWith("-")) {
                return CliParseResult.Error("不明なオプション: $arg\n${helpMessage()}")
            }
        }

        if (scriptPath == null) {
            scriptPath = arg
        } else {
            return CliParseResult.Error("複数のスクリプトは指定できません: $arg\n${helpMessage()}")
        }
        parsingOptions = false
    }

    if (showVersion) {
        if (scriptPath != null || forceRepl || forceStdin) {
            return CliParseResult.Error("バージョン表示オプションは他の引数と併用できません\n${helpMessage()}")
        }
        return CliParseResult.Version(BuildConfig.DNCL_VERSION)
    }

    if (forceStdin && (scriptPath != null || forceRepl)) {
        return CliParseResult.Error("--stdinはスクリプト指定や--replと併用できません\n${helpMessage()}")
    }

    return CliParseResult.Success(
        CliOptions(
            scriptPath = scriptPath,
            forceRepl = forceRepl,
            forceStdin = forceStdin
        )
    )
}

private fun helpMessage(): String = """
DNCL CLI v${BuildConfig.DNCL_VERSION}
使い方:
  dncl [オプション] [スクリプトファイル]

オプション:
  -r, --repl    常にREPLモードを起動
  -s, --stdin   標準入力から常にコードを読み取る
  -v, --version バージョンを表示
  -h, --help    このヘルプを表示

引数なしの場合は、標準入力にデータがあればそれを実行し、なければREPLを起動します。
""".trimIndent()
