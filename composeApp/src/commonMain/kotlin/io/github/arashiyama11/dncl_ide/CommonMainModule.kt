package io.github.arashiyama11.dncl_ide

import io.github.arashiyama11.dncl_ide.adapter.IdeViewModel
import io.github.arashiyama11.dncl_ide.adapter.NotebookViewModel
import io.github.arashiyama11.dncl_ide.adapter.SelectFileScreenViewModel
import io.github.arashiyama11.dncl_ide.adapter.SelectNotebookScreenViewModel
import io.github.arashiyama11.dncl_ide.adapter.SettingsScreenViewModel
import io.github.arashiyama11.dncl_ide.common.AppScope
import io.github.arashiyama11.dncl_ide.common.AppStateStore
import io.github.arashiyama11.dncl_ide.util.SyntaxHighLighter
import io.github.arashiyama11.dncl_ide.domain.repository.FileRepository
import io.github.arashiyama11.dncl_ide.domain.repository.SettingsRepository
import io.github.arashiyama11.dncl_ide.repository.FileRepositoryImpl
import io.github.arashiyama11.dncl_ide.repository.SettingsRepositoryImpl
import io.github.arashiyama11.dncl_ide.util.Platform
import io.github.arashiyama11.dncl_ide.util.currentPlatform
import org.koin.compose.getKoin
import org.koin.core.module.dsl.binds
import org.koin.core.module.dsl.singleOf
import org.koin.core.module.dsl.viewModelOf
import org.koin.dsl.koinApplication
import org.koin.dsl.module

val commonMainModule = module {
    if (currentPlatform == Platform.Android) {
        viewModelOf(::IdeViewModel)
        viewModelOf(::SettingsScreenViewModel)
        viewModelOf(::SelectFileScreenViewModel)
        viewModelOf(::SelectNotebookScreenViewModel)
    } else {
        singleOf(::IdeViewModel)
        singleOf(::SettingsScreenViewModel)
        singleOf(::SelectFileScreenViewModel)
        singleOf(::SelectNotebookScreenViewModel)
    }

    singleOf(::AppScope)
    singleOf(::AppStateStore)
    singleOf(::NotebookViewModel)
    singleOf(::SyntaxHighLighter)
    singleOf(::FileRepositoryImpl) { binds(listOf(FileRepository::class)) }
    singleOf(::SettingsRepositoryImpl) { binds(listOf(SettingsRepository::class)) }
}