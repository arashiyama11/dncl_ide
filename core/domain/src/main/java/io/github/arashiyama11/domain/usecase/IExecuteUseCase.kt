package io.github.arashiyama11.domain.usecase

import io.github.arashiyama11.domain.model.DnclOutput
import kotlinx.coroutines.flow.Flow

interface IExecuteUseCase {
    operator fun invoke(program: String, input: String): Flow<DnclOutput>
}