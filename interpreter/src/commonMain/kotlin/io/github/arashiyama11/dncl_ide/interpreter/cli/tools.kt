package io.github.arashiyama11.dncl_ide.interpreter.cli

import kotlinx.coroutines.Job
import kotlinx.coroutines.channels.Channel


expect fun fileExists(path: String): Boolean
expect fun readFileText(path: String): String
expect fun stdinHasData(): Boolean
expect fun readAllStdin(): String

expect fun exitProcess(code: Int): Nothing

expect fun startStdinLineReader(channel: Channel<String>): Job?
expect fun stderrPrintln(message: String)
