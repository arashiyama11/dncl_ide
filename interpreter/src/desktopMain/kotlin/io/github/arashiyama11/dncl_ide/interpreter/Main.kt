package io.github.arashiyama11.dncl_ide.interpreter

import io.github.arashiyama11.dncl_ide.interpreter.api.Stdout
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
import kotlinx.coroutines.CoroutineScope
import io.github.arashiyama11.dncl_ide.interpreter.model.AstNode

@OptIn(DelicateCoroutinesApi::class)
fun main(args: Array<String>): Unit = runBlocking {
    if (args.isEmpty()) {
        // 標準入力にデータがあるかチェック
        val scanner = Scanner(System.`in`)
        if (scanner.hasNextLine()) {
            val sourceCode = scanner.useDelimiter("\\A").next()
            this.executeSourceCode(sourceCode)
        } else {
            runRepl()
        }
    } else {
        val filePath = args[0]
        val file = File(filePath)

        if (!file.exists()) {
            System.err.println("エラー: ファイルが見つかりません: $filePath")
            exitProcess(1)
        }

        val sourceCode = file.readText()
        this.executeSourceCode(sourceCode)
    }
}

@OptIn(DelicateCoroutinesApi::class)
suspend fun CoroutineScope.executeSourceCode(sourceCode: String) {
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
        arrayOrigin = 0,
        onEval = null
    )

    val env = EvaluatorFactory.createBuiltInFunctionEnvironment(
        stdout = StdoutImpl,
        onImport = { path ->
            System.err.println("REPLモードではimportはサポートされていません: $path")
            DnclObject.RuntimeError(
                "REPLモードではimportはサポートされていません: $path",
                AstNode.Program(emptyList())
            )
        }
    )

    this.launch {
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

@OptIn(DelicateCoroutinesApi::class)
suspend fun CoroutineScope.runRepl() {
    println("DNCL REPL (終了するには 'exit' または 'quit' と入力してください)")
    val scanner = Scanner(System.`in`)
    val inputChannel = Channel<String>()

    val env = EvaluatorFactory.createBuiltInFunctionEnvironment(
        stdout = StdoutImpl,
        onImport = { path ->
            System.err.println("REPLモードではimportはサポートされていません: $path")
            DnclObject.RuntimeError(
                "REPLモードではimportはサポートされていません: $path",
                AstNode.Program(emptyList())
            )
        }
    )

    this.launch {
        val inputScanner = Scanner(System.`in`)
        while (true) {
            if (inputChannel.isClosedForSend) break
            val line = inputScanner.nextLine()
            inputChannel.send(line)
        }
    }

    var currentInput = ""
    var prompt = ">>> "

    while (true) {
        print(prompt)
        val line = scanner.nextLine()

        if (line.lowercase() == "exit" || line.lowercase() == "quit") {
            break
        }

        val isBlankLine = line.trim().isEmpty()

        if (!isBlankLine) {
            currentInput += line + "\n"
        }

        val lexer = Lexer(currentInput)
        val parserResult = Parser(lexer)

        parserResult.fold(
            { error ->
                // LexerまたはParserの初期化エラー（致命的なエラー）
                System.err.println("構文解析エラー: ${error.explain(currentInput)}")
                currentInput = "" // 致命的なエラーなのでリセット
                prompt = ">>> "
            },
            { parser ->
                // Parserのインスタンスが正常に作成された場合
                val programResult = parser.parseProgram()

                programResult.fold(
                    { error ->
                        // parseProgram()が失敗した場合
                        val errorMessage = error.explain(currentInput)
                        if (isBlankLine || !(errorMessage.contains("EOF") || errorMessage.contains("expected") || errorMessage.contains(
                                "unclosed"
                            ))
                        ) {
                            // 空行で確定された場合、または復元不可能なエラーの場合、エラーを出力してリセット
                            System.err.println("構文解析エラー: $errorMessage")
                            currentInput = ""
                            prompt = ">>> "
                        } else {
                            // 不完全な入力とみなし、入力を継続
                            prompt = "... "
                        }
                    },
                    { program ->
                        // パース成功
                        // ここで、ユーザーがまだ入力を続けたいかどうかを判断する
                        var shouldContinue = false
                        if (!isBlankLine) { // 空行でない場合にのみ継続の可能性をチェック
                            val lines = currentInput.lines().filter { it.trim().isNotEmpty() }
                            val lastNonEmptyLine = lines.lastOrNull()
                            val secondLastNonEmptyLine =
                                if (lines.size >= 2) lines[lines.size - 2] else null

                            if (lastNonEmptyLine != null) {
                                val blockKeywords =
                                    listOf("もし", "関数", "繰り返し") // DNCLのキーワードに合わせて調整

                                // Case 1: The current line is a block-starting keyword ending with a colon
                                val isCurrentLineBlockStart = blockKeywords.any { keyword ->
                                    lastNonEmptyLine.trimStart()
                                        .startsWith(keyword) && lastNonEmptyLine.trimEnd()
                                        .endsWith(":")
                                }
                                if (isCurrentLineBlockStart) {
                                    shouldContinue = true
                                } else if (secondLastNonEmptyLine != null) {
                                    // Case 2: The current line is indented and the previous line was a block-starting keyword ending with a colon
                                    val isLastLineIndented =
                                        lastNonEmptyLine.startsWith(" ") || lastNonEmptyLine.startsWith(
                                            "\t"
                                        )
                                    val isPrevLineBlockStart = blockKeywords.any { keyword ->
                                        secondLastNonEmptyLine.trimStart()
                                            .startsWith(keyword) && secondLastNonEmptyLine.trimEnd()
                                            .endsWith(":")
                                    }
                                    shouldContinue = isLastLineIndented && isPrevLineBlockStart
                                }
                            }
                        }

                        if (shouldContinue) {
                            prompt = "... "
                        } else {
                            // 実行ロジック
                            val evaluator = EvaluatorFactory.create(
                                inputChannel = inputChannel,
                                arrayOrigin = 0,
                                onEval = null
                            )

                            val result = evaluator.eval(program, env)

                            result.fold(
                                { error ->
                                    System.err.println("実行時エラー: ${error.explain(currentInput)}")
                                },
                                { dnclObject ->
                                    if (dnclObject !is DnclObject.Nothing && dnclObject !is DnclObject.Null) {
                                        println(dnclObject)
                                    }
                                }
                            )
                            currentInput = ""
                            prompt = ">>> "
                        }
                    }
                )
            }
        )
        if (isBlankLine && currentInput.trim().isEmpty()) {
            continue
        }
    }
    inputChannel.close()
    exitProcess(0)
}

object StdoutImpl : Stdout {
    override suspend fun append(text: String) {
        print(text)
    }

    override suspend fun flush() {
        System.out.flush()
    }

    override suspend fun clear() {

    }

    override suspend fun commitFrame() {

    }

    override suspend fun replace(text: String) {
        print(text)
    }
}
