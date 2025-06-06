package io.github.arashiyama11.dncl_ide.common

import kotlinx.coroutines.CoroutineExceptionHandler
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlin.coroutines.CoroutineContext

class AppScope() : CoroutineScope {
    private val coroutineExceptionHandler = CoroutineExceptionHandler { _, throwable ->
        //
        throw throwable
    }

    override val coroutineContext: CoroutineContext
        get() = Dispatchers.Default + coroutineExceptionHandler + SupervisorJob()
}

