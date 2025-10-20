package io.github.arashiyama11.dncl_ide.util

import androidx.compose.ui.ExperimentalComposeUiApi
import io.github.arashiyama11.dncl_ide.commonMainModule
import org.koin.core.context.startKoin
import org.koin.dsl.bind
import org.koin.dsl.module
import io.github.arashiyama11.dncl_ide.domain.domainModule
import io.github.arashiyama11.dncl_ide.domain.model.EntryPath
import io.github.arashiyama11.dncl_ide.util.RootPathProvider
import io.github.arashiyama11.dncl_ide.domain.repository.FileRepository

@OptIn(ExperimentalComposeUiApi::class, ExperimentalWasmJsInterop::class)
fun main() {
    class RootPathProviderImpl : RootPathProvider {
        override fun invoke(): EntryPath {
            return EntryPath.fromString("")
        }
    }


    val koin = startKoin {
        modules(commonMainModule, domainModule, module {
            single { RootPathProviderImpl() } bind RootPathProvider::class
            single { MockFileRepository() } bind FileRepository::class
        })
    }.koin



    with(koin) {
        webApp()
    }
}

