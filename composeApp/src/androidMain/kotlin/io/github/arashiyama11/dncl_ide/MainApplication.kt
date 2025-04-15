package io.github.arashiyama11.dncl_ide

import android.app.Application
import io.github.arashiyama11.dncl_ide.domain.domainModule
import io.github.arashiyama11.dncl_ide.util.RootPathProvider
import org.koin.android.ext.koin.androidContext
import org.koin.android.ext.koin.androidLogger
import org.koin.core.context.GlobalContext.startKoin
import org.koin.dsl.bind
import org.koin.dsl.module

class MainApplication : Application() {
    override fun onCreate() {
        super.onCreate()
        startKoin {
            androidLogger()
            androidContext(this@MainApplication)
            modules(domainModule, commonMainModule, module {
                single { RootPathProviderImpl(androidContext()) } bind RootPathProvider::class
            })
        }
    }
}

