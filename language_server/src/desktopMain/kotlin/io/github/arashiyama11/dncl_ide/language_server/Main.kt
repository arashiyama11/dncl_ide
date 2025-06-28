package io.github.arashiyama11.dncl_ide.language_server

import kotlinx.coroutines.*
import kotlinx.coroutines.channels.consumeEach
import kotlinx.serialization.json.Json
import java.io.BufferedInputStream
import java.io.File
import java.io.InputStreamReader

fun main() = runBlocking {
    logging("Starting DNCL Language Server")
    val json = Json { ignoreUnknownKeys = true }
    val astInfoService = AstInfoService()
    val server = DNCLLanguageServer(
        DocumentManager(),
        DiagnosticService(),
        CompletionService(),
        HoverService(astInfoService),
        DefinitionService(astInfoService),
        ReferenceService(astInfoService),
        RenameService(astInfoService),
        FormattingService(),
        CodeActionService(),
        SemanticTokensService(),
        astInfoService
    )

    // 出力ループを起動
    val outputJob = launchOutputLoop(server)
    // 入力ループを起動
    val inputJob = launchInputLoop(server, json)
    logging("Language Server started")

    // シャットダウン等が必要ならここで待機
    joinAll(outputJob, inputJob)
}

fun CoroutineScope.launchOutputLoop(server: DNCLLanguageServer) = launch(Dispatchers.IO) {
    server.output.consumeEach { it ->
        val bytes = it.encodeToByteArray()
        // フレーミング
        System.out.write("Content-Length: ${bytes.size}\r\n\r\n".toByteArray())
        System.out.write(bytes)
        System.out.flush()
    }
}


fun logging(message: String) {
    File("server.log").appendText("$message\n")
}

fun CoroutineScope.launchInputLoop(
    server: DNCLLanguageServer,
    json: Json
) = launch(Dispatchers.IO) {
    val inStream = BufferedInputStream(System.`in`)
    val reader = InputStreamReader(inStream, Charsets.UTF_8).buffered()

    while (isActive) {
        // ヘッダー読み取り
        val headers = mutableMapOf<String, String>()
        var line: String = reader.readLine() ?: break
        if (line.isBlank()) continue
        while (line.isNotBlank()) {
            val (key, value) = line.split(":", limit = 2)
            headers[key.trim()] = value.trim()
            line = reader.readLine() ?: break
        }

        // Content-Length を取得
        val length = headers["Content-Length"]?.toIntOrNull() ?: continue

        // 本文読み取り（バイト数指定）
        val buf = CharArray(length)
        var read = 0
        while (read < length) {
            val r = reader.read(buf, read, length - read)
            if (r < 0) break
            read += r
        }
        val message = String(buf, 0, read)

        // JSON-RPC リクエストとして処理
        runCatching { json.decodeFromString<JsonRpcRequest>(message) }
            .onSuccess {
                withContext(Dispatchers.Default) {
                    server.handleMessage(it)
                }
            }
    }
}
