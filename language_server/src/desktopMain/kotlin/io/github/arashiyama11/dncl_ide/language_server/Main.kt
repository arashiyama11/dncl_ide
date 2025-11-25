package io.github.arashiyama11.dncl_ide.language_server

import io.github.arashiyama11.dncl_ide.language_server.service.AstInfoService
import io.github.arashiyama11.dncl_ide.language_server.service.CodeActionService
import io.github.arashiyama11.dncl_ide.language_server.service.CompletionService
import io.github.arashiyama11.dncl_ide.language_server.service.DefinitionService
import io.github.arashiyama11.dncl_ide.language_server.service.FormattingService
import io.github.arashiyama11.dncl_ide.language_server.service.HoverService
import io.github.arashiyama11.dncl_ide.language_server.service.ReferenceService
import io.github.arashiyama11.dncl_ide.language_server.service.RenameService
import io.github.arashiyama11.dncl_ide.language_server.service.SemanticTokensService
import io.github.arashiyama11.dncl_ide.language_server.service.StdlibOnlyFileResolver
import kotlinx.coroutines.*
import kotlinx.coroutines.channels.consumeEach
import kotlinx.serialization.json.Json
import java.io.BufferedInputStream
import java.io.ByteArrayOutputStream
import java.io.EOFException
import java.io.File
import kotlin.time.Clock
import kotlin.time.ExperimentalTime

private val LimitedDefault = Dispatchers.Default.limitedParallelism(2)
private val limitedScope = CoroutineScope(SupervisorJob() + LimitedDefault)


suspend fun main() {
    logging("Starting DNCL Language Server")

    try {
        limitedScope.launch {
            val json = Json { ignoreUnknownKeys = true }
            val fileResolver = StdlibOnlyFileResolver()
            val astInfoService = AstInfoService(fileResolver)
            val documentAnalyzerService = DocumentAnalyzerImpl(fileResolver)
            val analyzer = DocumentAnalyzerImpl()
            val server = DNCLLanguageServer(
                DocumentManager(),
                CompletionService(fileResolver),
                HoverService(astInfoService),
                DefinitionService(astInfoService),
                ReferenceService(astInfoService),
                RenameService(astInfoService),
                FormattingService(),
                CodeActionService(),
                SemanticTokensService(astInfoService, fileResolver),
                astInfoService,
                scheduler = DefaultDocumentScheduler(limitedScope, analyzer),
                config = LanguageServerConfig(
                    debounceMillis = 100
                )
            )

            // 出力ループを起動
            val outputJob = launchOutputLoop(server)
            // 入力ループを起動
            val inputJob = launchInputLoop(server, json)
            logging("Language Server started")

            // シャットダウン等が必要ならここで待機
            joinAll(outputJob, inputJob)
        }.join()
    } catch (e: Throwable) {
        logging("fatal error\n$e")
    }
}

fun CoroutineScope.launchOutputLoop(server: DNCLLanguageServer) = launch(Dispatchers.IO) {
    server.output.consumeEach { it ->
        val bytes = it.encodeToByteArray()
        val length = "Content-Length: ${bytes.size}\r\n\r\n"
        logging("<-- ${String(bytes, Charsets.UTF_8)}")
        System.out.write(length.toByteArray())
        System.out.write(bytes)
        System.out.flush()
    }
}

val logFile by lazy {
    if (System.getenv("DNCL_LS_LOG_FILE")?.isNotEmpty() == true) {
        File(System.getenv("DNCL_LS_LOG_FILE"))
    } else {
        File("server.log")
    }
}

@OptIn(ExperimentalTime::class)
internal actual fun logging(message: String) {
    val time = Clock.System.now()
    logFile.appendText("$time $message\n")
}

fun CoroutineScope.launchInputLoop(
    server: DNCLLanguageServer,
    json: Json,
    timeoutMillis: Long = 3_000L
) = launch(Dispatchers.IO) {
    val rawIn = BufferedInputStream(System.`in`)

    while (isActive) {
        // タイムアウト付きでヘッダー＋本文をまとめて読み込む
        val message: String? = withTimeoutOrNull(timeoutMillis) {
            // ── ヘッダー取得 ────────────────────────────────────────────
            val headerBytes = ByteArrayOutputStream()
            var prevState = 0
            while (prevState < 4) {
                val b = rawIn.read().takeIf { it >= 0 } ?: throw EOFException()
                headerBytes.write(b)
                prevState = when (prevState) {
                    0 -> if (b == '\r'.code) 1 else 0
                    1 -> if (b == '\n'.code) 2 else if (b == '\r'.code) 1 else 0
                    2 -> if (b == '\r'.code) 3 else 0
                    3 -> if (b == '\n'.code) 4 else 0
                    else -> prevState
                }
            }
            val headers = headerBytes.toString("UTF-8")
            val length = headers
                .lineSequence()
                .mapNotNull {
                    it.split(":", limit = 2)
                        .takeIf { it[0].equals("Content-Length", true) }
                        ?.getOrNull(1)
                        ?.trim()
                        ?.toIntOrNull()
                }
                .firstOrNull()
                ?: return@withTimeoutOrNull null

            //logging("Received headers, Content-Length=$length")

            // ── 本文取得 ───────────────────────────────────────────────
            val body = ByteArray(length)
            var read = 0
            while (read < length) {
                val n = rawIn.read(body, read, length - read)
                if (n < 0) throw EOFException("Unexpected EOF")
                read += n
            }
            body.toString(Charsets.UTF_8)
        }

        if (message == null) {
            // タイムアウト or Content-Length が取れなかった
            logging("No input within ${timeoutMillis}ms, retrying…")
            continue
        }

        logging("--> $message")
        runCatching { json.decodeFromString<JsonRpcRequest>(message) }
            .onSuccess { launch { server.handleMessage(it) } }
            .onFailure { logging("Error processing message: ${it.message}") }
    }
}
