package io.github.arashiyama11.dncl_ide

import io.github.arashiyama11.dncl_ide.domain.model.EntryPath
import io.github.arashiyama11.dncl_ide.util.IRootPathProvider
import platform.Foundation.NSDocumentDirectory
import platform.Foundation.NSUserDomainMask
import platform.Foundation.NSSearchPathForDirectoriesInDomains

class RootPathProvider : IRootPathProvider {
    override fun invoke(): EntryPath {
        val paths = NSSearchPathForDirectoriesInDomains(
            directory = NSDocumentDirectory,
            domainMask = NSUserDomainMask,
            expandTilde = true
        )


        return (paths.first() as String).let {
            EntryPath.fromString(it)
        }.also { println(it) }
    }
}