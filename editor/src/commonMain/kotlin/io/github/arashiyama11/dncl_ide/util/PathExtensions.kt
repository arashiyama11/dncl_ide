package io.github.arashiyama11.dncl_ide.util

import io.github.arashiyama11.dncl_ide.domain.model.EntryPath

fun EntryPath.toFileUri(): String {
    val rawPath = toString()
    val normalized = rawPath.replace('\\', '/')
    val prefix = if (normalized.startsWith("/")) "file://" else "file:///"
    return prefix + normalized
}
