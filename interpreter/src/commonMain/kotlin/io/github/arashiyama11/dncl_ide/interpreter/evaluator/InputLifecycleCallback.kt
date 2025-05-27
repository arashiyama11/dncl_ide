package io.github.arashiyama11.dncl_ide.interpreter.evaluator

/**
 * Callback interface for monitoring the input lifecycle during DNCL program execution.
 */
interface InputLifecycleCallback {
    /**
     * Called just before the evaluator starts waiting for an external input.
     */
    suspend fun onWaitingForInput()

    /**
     * Called immediately after an external input has been received by the evaluator.
     */
    suspend fun onInputReceived()
} 