package io.github.arashiyama11.dncl_ide.interpreter.api

interface Stdout {
    suspend fun append(text: String)
    suspend fun flush()
    suspend fun clear()
    suspend fun commitFrame()
    suspend fun replace(text: String)
}
