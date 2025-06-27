package io.github.arashiyama11.dncl_ide.language_server

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import kotlinx.serialization.json.*

fun main() = runBlocking {
    val json = Json { ignoreUnknownKeys = true }
    println("server started, waiting for requests...")


    val server = DNCLLanguageServer()
    launch(Dispatchers.Default) {
        for (response in server.output) {
            println(response)
        }
    }

    while (true) {
        val line = readlnOrNull() ?: run {
            delay(100)
            continue
        }
        if (line.isBlank()) continue

        val req =
            runCatching { json.decodeFromString<JsonRpcRequest>(line) }.getOrNull() ?: continue
        server.handleMessage(req)
    }
}
