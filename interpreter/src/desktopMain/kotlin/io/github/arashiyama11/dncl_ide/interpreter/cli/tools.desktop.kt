package io.github.arashiyama11.dncl_ide.interpreter.cli

import kotlinx.coroutines.DelicateCoroutinesApi
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.launch
import java.io.File
import java.util.Scanner


actual fun fileExists(path: String): Boolean = File(path).exists()

actual fun readFileText(path: String): String = File(path).readText()

actual fun stdinHasData(): Boolean {
    // JVMでは System.console() が null のときパイプ入力（IDEなど）を受け取れることが多い
    // ただし確実ではないので Scanner を使って peek しない方針にする（簡易判定）
    return System.`in`.available() > 0
}

actual fun readAllStdin(): String {
    val scanner = Scanner(System.`in`)
    scanner.useDelimiter("\\A")
    return if (scanner.hasNext()) scanner.next() else ""
}

@OptIn(DelicateCoroutinesApi::class)
actual fun startStdinLineReader(channel: Channel<String>): Job? {
    return GlobalScope.launch(Dispatchers.IO) {
        val scanner = Scanner(System.`in`)
        try {
            while (scanner.hasNextLine()) {
                val line = scanner.nextLine()
                channel.send(line)
            }
        } finally {
            scanner.close()
        }
    }
}

actual fun stderrPrintln(message: String) {
    System.err.println(message)
}

actual fun exitProcess(code: Int): Nothing {
    kotlin.system.exitProcess(code)
}
