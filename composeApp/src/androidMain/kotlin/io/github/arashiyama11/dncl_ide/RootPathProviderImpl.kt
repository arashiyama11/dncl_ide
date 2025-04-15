package io.github.arashiyama11.dncl_ide

import android.content.Context
import io.github.arashiyama11.dncl_ide.domain.model.EntryPath
import io.github.arashiyama11.dncl_ide.util.RootPathProvider

class RootPathProviderImpl(private val context: Context) : RootPathProvider {
    override fun invoke(): EntryPath {
        return EntryPath.fromString(context.filesDir.resolve("programs").absolutePath.toString())
    }
}