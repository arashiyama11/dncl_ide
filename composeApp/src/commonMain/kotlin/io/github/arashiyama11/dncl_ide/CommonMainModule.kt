package io.github.arashiyama11.dncl_ide

import io.github.arashiyama11.dncl_ide.adapter.DrawerViewModel
import io.github.arashiyama11.dncl_ide.adapter.IdeViewModel
import io.github.arashiyama11.dncl_ide.util.SyntaxHighLighter
import io.github.arashiyama11.dncl_ide.domain.repository.IFileRepository
import io.github.arashiyama11.dncl_ide.domain.repository.ISettingsRepository
import io.github.arashiyama11.dncl_ide.repository.FileRepository
import io.github.arashiyama11.dncl_ide.repository.SettingsRepository
import org.koin.core.module.dsl.binds
import org.koin.core.module.dsl.singleOf
import org.koin.core.module.dsl.viewModelOf
import org.koin.dsl.module

val commonMainModule = module {
    viewModelOf(::DrawerViewModel)
    viewModelOf(::IdeViewModel)
    singleOf(::SyntaxHighLighter)
    singleOf(::FileRepository) { binds(listOf(IFileRepository::class)) }
    singleOf(::SettingsRepository) { binds(listOf(ISettingsRepository::class)) }
}