package io.github.arashiyama11.domain.usecase

import arrow.core.Either
import io.github.arashiyama11.domain.model.EntryName
import io.github.arashiyama11.domain.model.EntryPath
import io.github.arashiyama11.domain.model.ValidationError

interface IFileNameValidationUseCase {
    suspend operator fun invoke(entryPath: EntryPath): Either<ValidationError, Unit>
}