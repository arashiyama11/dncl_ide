package io.github.arashiyama11.domain.usecase

import arrow.core.Either
import io.github.arashiyama11.domain.model.FileName
import io.github.arashiyama11.domain.model.ValidationError

interface IFileNameValidationUseCase {
    suspend operator fun invoke(fileName: FileName): Either<ValidationError, Unit>
}