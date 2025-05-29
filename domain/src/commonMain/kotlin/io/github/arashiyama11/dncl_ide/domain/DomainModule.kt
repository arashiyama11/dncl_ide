package io.github.arashiyama11.dncl_ide.domain

import io.github.arashiyama11.dncl_ide.domain.usecase.ExecuteUseCase
import io.github.arashiyama11.dncl_ide.domain.usecase.FileNameValidationUseCase
import io.github.arashiyama11.dncl_ide.domain.usecase.FileUseCase
import io.github.arashiyama11.dncl_ide.domain.usecase.NotebookFileUseCase
import io.github.arashiyama11.dncl_ide.domain.usecase.SettingsUseCase
import io.github.arashiyama11.dncl_ide.domain.usecase.SuggestionUseCase
import org.koin.core.module.dsl.singleOf
import org.koin.dsl.module

val domainModule = module {
    singleOf(::ExecuteUseCase)
    singleOf(::FileNameValidationUseCase)
    singleOf(::FileUseCase)
    singleOf(::SettingsUseCase)
    singleOf(::SuggestionUseCase)
    singleOf(::NotebookFileUseCase)
}