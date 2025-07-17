package io.github.arashiyama11.dncl_ide.util

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.consumeAsFlow
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock

sealed interface OutputEvent {
    data class Append(val cellId: String?, val text: String) : OutputEvent
    data class Clear(val cellId: String?, val immediately: Boolean = true) : OutputEvent
    data class Replace(val cellId: String?, val text: String) : OutputEvent
    data class CommitFrame(val cellId: String?) : OutputEvent
}

interface Stdout {
    fun append(cellId: String? = null, text: String)
    fun flush(cellId: String? = null)
    fun clear(cellId: String? = null)
    fun commitFrame(cellId: String? = null)
    fun replace(cellId: String? = null, text: String) = clear(cellId).also { append(cellId, text) }
}

class StdoutImpl(
    private val eventFlow: MutableSharedFlow<OutputEvent>
) : Stdout {
    private val buffer = mutableMapOf<String?, StringBuilder>()
    private val bufferMutex = Mutex()

    override fun append(cellId: String?, text: String) {
        launch {
            bufferMutex.withLock {
                buffer.getOrPut(cellId) { StringBuilder() }.append(text)
            }
        }
    }

    override fun flush(cellId: String?) {
        launch {
            bufferMutex.withLock {
                buffer[cellId]?.let {
                    if (it.isNotEmpty()) {
                        eventFlow.emit(OutputEvent.Append(cellId, it.toString()))
                        it.clear()
                    }
                }
            }
        }
    }

    override fun clear(cellId: String?) {
        launch {
            bufferMutex.withLock {
                buffer.remove(cellId)
            }
            eventFlow.emit(OutputEvent.Clear(cellId))
        }
    }

    override fun commitFrame(cellId: String?) {
        launch {
            flush(cellId)
            eventFlow.emit(OutputEvent.CommitFrame(cellId))
        }
    }

    override fun replace(cellId: String?, text: String) {
        launch {
            bufferMutex.withLock {
                buffer.remove(cellId)
            }
            eventFlow.emit(OutputEvent.Replace(cellId, text))
        }
    }

    private fun launch(block: suspend () -> Unit) {
        CoroutineScope(Dispatchers.Default).launch {
            block()
        }
    }
}

class OutputHandler(
    private val scope: CoroutineScope,
    private val onUpdate: (Map<String?, String>) -> Unit
) : Stdout {
    private val eventChannel = Channel<OutputEvent>(Channel.BUFFERED)
    private val eventFlow = MutableSharedFlow<OutputEvent>()
    private val stdoutImpl = StdoutImpl(eventFlow)
    private var job: Job? = null
    private val outputs = mutableMapOf<String?, String>()
    private val outputsMutex = Mutex()
    var onFrameCommit: (() -> Unit)? = null

    init {
        start()
    }

    private fun start() {
        job = scope.launch {
            eventFlow.collect { event ->
                outputsMutex.withLock {
                    when (event) {
                        is OutputEvent.Append -> {
                            val current = outputs.getOrPut(event.cellId) { "" }
                            outputs[event.cellId] = current + event.text
                        }
                        is OutputEvent.Clear -> {
                            if (event.immediately) {
                                outputs.remove(event.cellId)
                            }
                        }
                        is OutputEvent.Replace -> {
                            outputs[event.cellId] = event.text
                        }
                        is OutputEvent.CommitFrame -> {
                            onFrameCommit?.invoke()
                        }
                    }
                }
                onUpdate(outputs)
            }
        }
    }

    fun stop() {
        job?.cancel()
    }

    override fun append(cellId: String?, text: String) = stdoutImpl.append(cellId, text)
    override fun flush(cellId: String?) = stdoutImpl.flush(cellId)
    override fun clear(cellId: String?) = stdoutImpl.clear(cellId)
    override fun commitFrame(cellId: String?) = stdoutImpl.commitFrame(cellId)
    override fun replace(cellId: String?, text: String) = stdoutImpl.replace(cellId, text)
}
