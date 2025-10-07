package io.github.arashiyama11.dncl_ide.interpreter.cli

import kotlinx.coroutines.Job
import kotlinx.coroutines.channels.Channel


actual fun fileExists(path: String): Boolean = TODO()

actual fun readFileText(path: String): String = TODO()

actual fun stdinHasData(): Boolean = TODO()

actual fun readAllStdin(): String = TODO()

actual fun exitProcess(code: Int): Nothing = TODO()

actual fun startStdinLineReader(channel: Channel<String>): Job? = TODO()

actual fun stderrPrintln(message: String): Unit = TODO()