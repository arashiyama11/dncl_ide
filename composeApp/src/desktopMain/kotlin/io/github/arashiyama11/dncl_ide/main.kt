package io.github.arashiyama11.dncl_ide

import androidx.compose.ui.unit.DpSize
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Window
import androidx.compose.ui.window.application
import androidx.compose.ui.window.rememberWindowState
import io.github.arashiyama11.dncl_ide.domain.domainModule
import io.github.arashiyama11.dncl_ide.domain.model.EntryPath
import io.github.arashiyama11.dncl_ide.ui.App
import io.github.arashiyama11.dncl_ide.util.RootPathProvider
import org.koin.core.context.startKoin
import org.koin.dsl.bind
import org.koin.dsl.module


fun main() {
    class RootPathProviderImpl : RootPathProvider {
        override fun invoke(): EntryPath {
            return EntryPath.fromString(System.getProperty("user.dir") + "/dncl")
                .also { println(it) }
        }
    }


    startKoin {
        modules(commonMainModule, domainModule, module {
            single { RootPathProviderImpl() } bind RootPathProvider::class
        })
    }

    application {
        Window(
            onCloseRequest = ::exitApplication,
            title = "Smart Phone Size",
            state = rememberWindowState(size = DpSize(360.dp, 800.dp)),
        ) {
            App()
        }

        Window(
            onCloseRequest = ::exitApplication,
            title = "Tablet Size Portrait",
            state = rememberWindowState(size = DpSize(512.dp, 683.dp)),
        ) {
            App()
        }

        Window(
            onCloseRequest = ::exitApplication,
            title = "Tablet Size Landscape",
            state = rememberWindowState(size = DpSize(683.dp, 512.dp)),
        ) {
            App()
        }
    }
}
