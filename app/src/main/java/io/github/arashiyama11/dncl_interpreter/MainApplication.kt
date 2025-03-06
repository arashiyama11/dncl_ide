package io.github.arashiyama11.dncl_interpreter

import android.app.Application
import io.github.arashiyama11.data.DataModule
import org.koin.android.ext.koin.androidContext
import org.koin.android.ext.koin.androidLogger
import org.koin.core.annotation.ComponentScan
import org.koin.core.annotation.Module
import org.koin.core.context.startKoin
import org.koin.ksp.generated.*

class MainApplication : Application() {

    override fun onCreate() {
        super.onCreate()
        startKoin {
            androidLogger()
            androidContext(this@MainApplication)
            modules(DataModule().module)
            modules(AppModule().module)
            defaultModule()
        }
    }
}

@Module
@ComponentScan("io.github.arashiyama11.data", "io.github.arashiyama11.domain")
class AppModule