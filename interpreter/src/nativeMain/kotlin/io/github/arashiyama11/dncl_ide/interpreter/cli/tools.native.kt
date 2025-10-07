package io.github.arashiyama11.dncl_ide.interpreter.cli

import kotlinx.cinterop.ExperimentalForeignApi
import kotlinx.cinterop.UnsafeNumber
import kotlinx.cinterop.alloc
import kotlinx.cinterop.convert
import kotlinx.cinterop.free
import kotlinx.cinterop.nativeHeap
import kotlinx.cinterop.ptr
import kotlinx.cinterop.refTo
import kotlinx.coroutines.DelicateCoroutinesApi
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.launch
import platform.posix.SEEK_END
import platform.posix.SEEK_SET
import platform.posix.STDIN_FILENO
import platform.posix.fclose
import platform.posix.fopen
import platform.posix.fprintf
import platform.posix.fread
import platform.posix.fseek
import platform.posix.ftell
import platform.posix.isatty
import platform.posix.stat
import platform.posix.stderr


@OptIn(ExperimentalForeignApi::class)
actual fun fileExists(path: String): Boolean {
    val statbuf = nativeHeap.alloc<stat>()
    val res = stat(path, statbuf.ptr)
    nativeHeap.free(statbuf)
    return res == 0
}

@OptIn(ExperimentalForeignApi::class, UnsafeNumber::class)
actual fun readFileText(path: String): String {
    val file = fopen(path, "rb") ?: throw IllegalArgumentException("ファイルを開けません: $path")
    try {
        fseek(file, 0, SEEK_END)
        val size = ftell(file)
        fseek(file, 0, SEEK_SET)
        val buffer = ByteArray(size.toInt())
        if (size > 0) {
            val read = fread(buffer.refTo(0), 1.convert(), size.convert(), file).toInt()
            return buildString {
                append(buffer.copyOf(read))
            }
        }
        return ""
    } finally {
        fclose(file)
    }
}

actual fun stdinHasData(): Boolean {
    return isatty(STDIN_FILENO) == 0
}

actual fun readAllStdin(): String {
    val sb = StringBuilder()
    while (true) {
        val line = readlnOrNull() ?: break
        sb.append(line).append("\n")
    }
    return sb.toString()
}

@OptIn(DelicateCoroutinesApi::class)
actual fun startStdinLineReader(channel: Channel<String>): Job? {
    return GlobalScope.launch {
        while (true) {
            val line = readlnOrNull() ?: break
            channel.send(line)
        }
    }
}

@OptIn(ExperimentalForeignApi::class)
actual fun stderrPrintln(message: String) {
    fprintf(stderr, "%s\n", message)
}

actual fun exitProcess(code: Int): Nothing {
    platform.posix._exit(code)
    throw IllegalStateException("Unreachable")
}