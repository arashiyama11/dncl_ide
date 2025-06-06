package io.github.arashiyama11.dncl_ide.common

import io.github.arashiyama11.dncl_ide.adapter.CreatingType
import io.github.arashiyama11.dncl_ide.domain.model.EntryPath
import io.github.arashiyama11.dncl_ide.domain.repository.FileRepository
import io.github.arashiyama11.dncl_ide.domain.usecase.SettingsUseCase
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach

data class AppState(
    val selectedEntryPath: EntryPath? = null,
    val rootFolder: String? = null,
    val creatingType: CreatingType? = null,
    val inputtingEntryPath: String? = null,
    val inputtingFileName: String? = null,
    val lastClickedFolder: String? = null
)

class AppStateStore(
    private val settingsUseCase: SettingsUseCase,
    private val fileRepository: FileRepository,
    private val appScope: AppScope
) : AutoCloseable {
    private val state = MutableStateFlow(AppState())
    private val jobs: MutableList<Job> = mutableListOf()

    init {
        jobs += settingsUseCase.settingsState.onEach {
            state.value = state.value.copy(
                inputtingEntryPath = it.inputtingEntryPath,
                inputtingFileName = it.inputtingFileName
            )
        }.launchIn(appScope)

        jobs += fileRepository.selectedEntryPath.onEach { path ->
            state.value = state.value.copy(selectedEntryPath = path)
        }.launchIn(appScope)
    }

    override fun close() {
    }
}