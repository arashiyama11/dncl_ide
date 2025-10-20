package io.github.arashiyama11.dncl_ide

import io.github.arashiyama11.dncl_ide.adapter.IdeViewModel
import io.github.arashiyama11.dncl_ide.adapter.NotebookViewModel
import io.github.arashiyama11.dncl_ide.adapter.SelectFileScreenViewModel
import io.github.arashiyama11.dncl_ide.adapter.SelectNotebookScreenViewModel
import io.github.arashiyama11.dncl_ide.adapter.SettingsScreenViewModel
import io.github.arashiyama11.dncl_ide.common.AppScope
import io.github.arashiyama11.dncl_ide.common.AppStateStore
import io.github.arashiyama11.dncl_ide.common.StatePermission
import io.github.arashiyama11.dncl_ide.editor.lsp.DefaultLanguageFeatureProvider
import io.github.arashiyama11.dncl_ide.editor.lsp.LanguageServerClient
import io.github.arashiyama11.dncl_ide.editor.lsp.LanguageServerSession
import io.github.arashiyama11.dncl_ide.editor.lsp.LanguageFeatureProvider
import io.github.arashiyama11.dncl_ide.util.SyntaxHighLighter
import io.github.arashiyama11.dncl_ide.domain.repository.FileRepository
import io.github.arashiyama11.dncl_ide.domain.repository.SettingsRepository
import io.github.arashiyama11.dncl_ide.domain.usecase.ExecuteUseCase
import io.github.arashiyama11.dncl_ide.repository.FileRepositoryImpl
import io.github.arashiyama11.dncl_ide.repository.SettingsRepositoryImpl
import io.github.arashiyama11.dncl_ide.util.Platform
import io.github.arashiyama11.dncl_ide.util.currentPlatform
import io.github.arashiyama11.dncl_ide.util.ioDispatcher
import kotlinx.coroutines.CoroutineScope
import kotlinx.io.files.FileSystem
import kotlinx.io.files.SystemFileSystem
import org.koin.core.module.dsl.binds
import org.koin.core.module.dsl.singleOf
import org.koin.dsl.bind
import org.koin.dsl.module

val commonMainModule = module {
    singleOf(::SettingsScreenViewModel)
    singleOf(::SelectFileScreenViewModel)
    singleOf(::SelectNotebookScreenViewModel)
    singleOf(::IdeViewModel)


    single<AppStateStore<StatePermission.Write>> {
        AppStateStore(get(), get(), get())
    }.bind<AppStateStore<StatePermission.Read>>()

    single {
        ExecuteUseCase(get(), get(), ioDispatcher)
    }

    singleOf(::AppScope) { binds(listOf(CoroutineScope::class)) }
    singleOf(::NotebookViewModel)
    singleOf(::SyntaxHighLighter)
    single<LanguageServerClient> { LanguageServerSession(get()) }
    singleOf(::DefaultLanguageFeatureProvider) { binds(listOf(LanguageFeatureProvider::class)) }
    singleOf(::FileRepositoryImpl) { binds(listOf(FileRepository::class)) }
    singleOf(::SettingsRepositoryImpl) { binds(listOf(SettingsRepository::class)) }

    if (currentPlatform != Platform.Web) {
        single<FileSystem> { SystemFileSystem }
    }
}
