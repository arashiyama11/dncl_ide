package io.github.arashiyama11.dncl_ide.util

import kotlinx.atomicfu.atomic
import kotlinx.atomicfu.update
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock

sealed class OutputEvent {
    data class Stdout(val value: String, val cellId: String? = null) : OutputEvent()
    data class Clear(val cellId: String? = null) : OutputEvent()
    data class End(val cellId: String? = null) : OutputEvent()
}

class OutputHandler(
    private val scope: CoroutineScope,
    private val onUpdate: (output: Map<String?, String>) -> Unit
) {
    private val eventChannel = Channel<OutputEvent>(Channel.UNLIMITED)
    private val outputMutex = Mutex()
    private val outputBuffers = mutableMapOf<String?, StringBuilder>()
    private val pendingOutputCount = atomic(0)
    private var watchJob: Job? = null

    init {
        start()
    }

    fun send(event: OutputEvent) {
        scope.launch {
            eventChannel.send(event)
        }
    }

    private fun start() {
        watchJob = scope.launch(Dispatchers.Default) {
            var isBusy = false
            for (event in eventChannel) {
                if (!isActive) break

                when (event) {
                    is OutputEvent.Stdout -> {
                        outputMutex.withLock {
                            outputBuffers.getOrPut(event.cellId) { StringBuilder() }
                                .append(event.value).append("\n")
                        }
                        pendingOutputCount.incrementAndGet()
                    }

                    is OutputEvent.Clear -> {
                        outputMutex.withLock {
                            outputBuffers[event.cellId]?.clear()
                        }
                        pendingOutputCount.incrementAndGet()
                    }

                    is OutputEvent.End -> {
                        // End event can trigger a final update
                    }
                }

                val count = pendingOutputCount.value
                if (count > 0) {
                    val x = 4L - count.toLong()
                    val t = x * x * x + x * 10L
                    if (t > 0) delay(t)
                }

                if ((event is OutputEvent.End || eventChannel.isEmpty) && isBusy) {
                    isBusy = false
                    flush()
                    pendingOutputCount.update { 0 }
                }

                if (!eventChannel.isEmpty && !isBusy) {
                    isBusy = true
                }

                if (!isBusy) {
                    flush()
                } else {
                    if (pendingOutputCount.value < 20) {
                        flush()
                    }
                }
            }
        }
    }

    private suspend fun flush() {
        val currentOutputs = outputMutex.withLock {
            outputBuffers.mapValues { it.value.toString() }
        }
        onUpdate(currentOutputs)
    }

    fun stop() {
        watchJob?.cancel()
        eventChannel.close()
    }

    fun clear(cellId: String? = null) {
        scope.launch {
            outputMutex.withLock {
                if (cellId == null) {
                    outputBuffers.clear()
                } else {
                    outputBuffers.remove(cellId)
                }
            }
            flush()
        }
    }
}