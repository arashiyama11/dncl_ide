package io.github.arashiyama11.dncl_interpreter.usecase

import arrow.core.Either
import io.github.arashiyama11.model.FileName

interface IFileNameValidationUseCase {
    suspend operator fun invoke(fileName: FileName): Either<ValidationError, Unit>
}