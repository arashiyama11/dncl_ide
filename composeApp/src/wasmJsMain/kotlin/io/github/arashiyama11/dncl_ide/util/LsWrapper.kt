//package io.github.arashiyama11.dncl_ide.util
//
//import io.github.arashiyama11.dncl_ide.language_server.DNCLLanguageServer
//import kotlinx.coroutines.DelicateCoroutinesApi
//import kotlinx.coroutines.GlobalScope
//import kotlinx.coroutines.channels.ReceiveChannel
//import kotlinx.coroutines.launch
//import org.w3c.dom.WebSocket
//
//
//interface DNCLLanguageServer {
//    val output: ReceiveChannel<String>
//    suspend fun handleMessage(message: String)
//}
//
//
//@OptIn(ExperimentalWasmJsInterop::class, DelicateCoroutinesApi::class)
//class FakeLsWebSocket(
//    private val ls: DNCLLanguageServer
//) {
//    val readyState = 1
//    val onopen = null
//    val onmessage = null
//    val onclose = null
//    val onerror = null
//    val _listeners = mutableMapOf<String, (MessageEvent) -> Unit>()
//
//    init {
//        GlobalScope.launch {
//            _emit("open")
//
//
//            for (msg in ls.output) {
//                WebSocket
//                if (onmessage != null) {
//                    onmessage.invoke<String, Any>(msg)
//                }
//
//                _emit("message")
//            }
//        }
//    }
//
//    fun _emit(kind: String, event: JsAny) {
//
//    }
//}
//
//
//data class Listeners(
//    val message: MutableList<(MessageEvent) -> Unit> = mutableListOf(),
//    val open: MutableList<() -> Unit> = mutableListOf(),
//    val close: MutableList<() -> Unit> = mutableListOf()
//)
//
//data class MessageEvent @OptIn(ExperimentalWasmJsInterop::class) constructor(val data: JsAny)
