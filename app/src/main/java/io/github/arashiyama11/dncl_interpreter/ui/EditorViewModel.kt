package io.github.arashiyama11.dncl_interpreter.ui

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import io.github.arashiyama11.dncl_interpreter.model.DnclOutput
import io.github.arashiyama11.dncl_interpreter.usecase.ExecuteUseCase
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch


data class EditorUiState(
    val text: String = """i を 0 から 100 まで 1 ずつ増やしながら:
       表示する(i)
""",
    val stdout: String = "",
    val stderr: String = ""
)

class EditorViewModel() : ViewModel() {
    private val _uiState = MutableStateFlow(EditorUiState())
    val uiState = _uiState.asStateFlow()

    fun onTextChanged(text: String) {
        _uiState.update { it.copy(text = text) }
    }

    fun onRunButtonClicked() {
        viewModelScope.launch {
            _uiState.update { it.copy(stdout = "", stderr = "") }
            ExecuteUseCase().execute(uiState.value.text).collect { output ->
                when (output) {
                    is DnclOutput.Stderr -> {
                        _uiState.update { it.copy(stderr = "${it.stderr}\n${output.value}") }
                    }

                    is DnclOutput.Stdout -> {
                        _uiState.update { it.copy(stdout = "${it.stdout}\n${output.value}") }
                    }
                }
            }

        }
    }

    fun onCancelButtonClicked() {
    }
}