package io.github.arashiyama11.dncl_ide.util

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableSharedFlow
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
    fun append(text: String)
    fun flush()
    fun clear()
    fun commitFrame()
    fun replace(text: String)
}

private class OutputBroker(
    private val eventFlow: MutableSharedFlow<OutputEvent>
) {
    private val buffer = mutableMapOf<String?, StringBuilder>()
    private val bufferMutex = Mutex()

    fun append(cellId: String?, text: String) {
        launch {
            bufferMutex.withLock {
                buffer.getOrPut(cellId) { StringBuilder() }.append(text)
            }
        }
    }

    fun flush(cellId: String?) {
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

    fun clear(cellId: String?) {
        launch {
            bufferMutex.withLock {
                buffer.remove(cellId)
            }
            eventFlow.emit(OutputEvent.Clear(cellId))
        }
    }

    fun commitFrame(cellId: String?) {
        launch {
            flush(cellId)
            eventFlow.emit(OutputEvent.CommitFrame(cellId))
        }
    }

    fun replace(cellId: String?, text: String) {
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

private class StdoutImpl(
    private val cellId: String?,
    private val broker: OutputBroker
) : Stdout {
    override fun append(text: String) = broker.append(cellId, text)
    override fun flush() = broker.flush(cellId)
    override fun clear() = broker.clear(cellId)
    override fun commitFrame() = broker.commitFrame(cellId)
    override fun replace(text: String) = broker.replace(cellId, text)
}

class OutputHandler(
    private val scope: CoroutineScope,
    private val onUpdate: (Map<String?, String>) -> Unit
) {
    private val eventFlow = MutableSharedFlow<OutputEvent>()
    private val broker = OutputBroker(eventFlow)
    private var job: Job? = null
    private val outputs = mutableMapOf<String?, String>()
    private val outputsMutex = Mutex()
    var onFrameCommit: (() -> Unit)? = null

    val stdout: Stdout = StdoutImpl(null, broker)

    fun stdoutFor(cellId: String): Stdout = StdoutImpl(cellId, broker)

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
}