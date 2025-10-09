package io.github.arashiyama11.dncl_ide.util

import kotlinx.coroutines.IO

actual val ioDispatcher: kotlinx.coroutines.CoroutineDispatcher
    get() = kotlinx.coroutines.Dispatchers.IO