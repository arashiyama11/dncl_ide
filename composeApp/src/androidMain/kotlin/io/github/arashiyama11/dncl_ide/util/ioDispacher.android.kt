package io.github.arashiyama11.dncl_ide.util

import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers

actual val ioDispatcher: CoroutineDispatcher
    get() = Dispatchers.IO