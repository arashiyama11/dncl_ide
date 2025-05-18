package io.github.arashiyama11.dncl_ide

import io.github.arashiyama11.dncl_ide.adapter.DrawerViewModel
import io.github.arashiyama11.dncl_ide.adapter.IdeViewModel
import io.github.arashiyama11.dncl_ide.util.SyntaxHighLighter
import io.github.arashiyama11.dncl_ide.domain.repository.FileRepository
import io.github.arashiyama11.dncl_ide.domain.repository.SettingsRepository
import io.github.arashiyama11.dncl_ide.repository.FileRepositoryImpl
import io.github.arashiyama11.dncl_ide.repository.SettingsRepositoryImpl
import io.github.arashiyama11.dncl_ide.util.TextSuggestions
import org.koin.core.module.dsl.binds
import org.koin.core.module.dsl.singleOf
import org.koin.core.module.dsl.viewModelOf
import org.koin.dsl.module

val commonMainModule = module {
    viewModelOf(::DrawerViewModel)
    viewModelOf(::IdeViewModel)
    singleOf(::SyntaxHighLighter)
    singleOf(::TextSuggestions)
    singleOf(::FileRepositoryImpl) { binds(listOf(FileRepository::class)) }
    singleOf(::SettingsRepositoryImpl) { binds(listOf(SettingsRepository::class)) }
}