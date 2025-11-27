package io.github.arashiyama11.dncl_ide

import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.unit.DpSize
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Window
import androidx.compose.ui.window.WindowPosition
import androidx.compose.ui.window.application
import androidx.compose.ui.window.rememberWindowState
import io.github.arashiyama11.dncl_ide.domain.domainModule
import io.github.arashiyama11.dncl_ide.domain.model.EntryPath
import io.github.arashiyama11.dncl_ide.ui.app.App
import io.github.arashiyama11.dncl_ide.util.RootPathProvider
import org.koin.core.context.startKoin
import org.koin.dsl.bind
import org.koin.dsl.module
import java.util.prefs.Preferences


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
        val preferences =
            remember { Preferences.userNodeForPackage(RootPathProviderImpl::class.java) }
        val initialSize = preferences.run {
            val width = getFloat("windowWidth", 360f)
            val height = getFloat("windowHeight", 800f)
            DpSize(width.dp, height.dp)
        }
        val initialPosition = preferences.run {
            val x = getFloat("windowPosX", Float.NaN)
            val y = getFloat("windowPosY", Float.NaN)
            if (x.isNaN() || y.isNaN()) WindowPosition.PlatformDefault else WindowPosition.Absolute(
                x.dp,
                y.dp
            )
        }

        val windowState = rememberWindowState(
            size = initialSize,
            position = initialPosition,
        )

        Window(
            onCloseRequest = ::exitApplication,
            title = "Smart Phone Size",
            state = windowState,
        ) {
            App()
        }

        // ウィンドウのサイズと位置を変更のたびに保存
        LaunchedEffect(windowState) {
            snapshotFlow { windowState.size }
                .collect { size ->
                    preferences.putFloat("windowWidth", size.width.value)
                    preferences.putFloat("windowHeight", size.height.value)
                }
        }

        LaunchedEffect(windowState) {
            snapshotFlow { windowState.position }
                .collect { position ->
                    if (position is WindowPosition.Absolute) {
                        preferences.putFloat("windowPosX", position.x.value)
                        preferences.putFloat("windowPosY", position.y.value)
                    }
                }
        }
    }
}
