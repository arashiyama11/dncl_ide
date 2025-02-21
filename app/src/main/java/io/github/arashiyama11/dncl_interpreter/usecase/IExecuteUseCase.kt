package io.github.arashiyama11.dncl_interpreter.usecase

import io.github.arashiyama11.model.DnclOutput
import kotlinx.coroutines.flow.Flow

interface IExecuteUseCase {
    operator fun invoke(program: String, input: String): Flow<DnclOutput>
}