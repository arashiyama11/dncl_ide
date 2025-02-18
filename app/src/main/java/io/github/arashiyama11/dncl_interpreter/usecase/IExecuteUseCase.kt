package io.github.arashiyama11.dncl_interpreter.usecase

import io.github.arashiyama11.dncl_interpreter.model.DnclOutput
import kotlinx.coroutines.flow.Flow

interface IExecuteUseCase {
    operator fun invoke(program: String): Flow<DnclOutput>
}