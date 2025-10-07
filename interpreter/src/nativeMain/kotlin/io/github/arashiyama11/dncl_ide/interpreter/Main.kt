package io.github.arashiyama11.dncl_ide.interpreter

import io.github.arashiyama11.dncl_ide.interpreter.cli.mainEntry
import kotlinx.coroutines.runBlocking


fun main(args: Array<String>): Unit = runBlocking {
    mainEntry(args)
}
