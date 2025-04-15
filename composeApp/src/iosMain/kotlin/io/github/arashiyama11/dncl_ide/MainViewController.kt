package io.github.arashiyama11.dncl_ide

import androidx.compose.ui.window.ComposeUIViewController
import io.github.arashiyama11.dncl_ide.domain.domainModule
import io.github.arashiyama11.dncl_ide.util.RootPathProvider
import io.github.arashiyama11.dncl_ide.ui.App
import org.koin.core.context.startKoin
import org.koin.dsl.bind
import org.koin.dsl.module


fun MainViewController() = ComposeUIViewController { App() }

fun initKoin() {
    startKoin {
        modules(commonMainModule, domainModule, module {
            single { RootPathProviderImpl() } bind RootPathProvider::class
        })
    }
}