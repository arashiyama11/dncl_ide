package io.github.arashiyama11.dncl_ide.util

import io.github.arashiyama11.dncl_ide.domain.model.EntryPath

interface RootPathProvider {
    operator fun invoke(): EntryPath
}