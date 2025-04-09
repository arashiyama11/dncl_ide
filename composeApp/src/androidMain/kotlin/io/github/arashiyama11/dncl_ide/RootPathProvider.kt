package io.github.arashiyama11.dncl_ide

import android.content.Context
import io.github.arashiyama11.dncl_ide.domain.model.EntryPath
import io.github.arashiyama11.dncl_ide.util.IRootPathProvider

class RootPathProvider(private val context: Context) : IRootPathProvider {
    override fun invoke(): EntryPath {
        return EntryPath.fromString(context.filesDir.resolve("programs").absolutePath.toString())
    }
}