package io.github.arashiyama11.dncl_interpreter.usecase

import arrow.core.Either
import arrow.core.left
import arrow.core.right
import org.koin.core.annotation.Single


sealed interface ValidationError {
    val message: String

    data object FileNameEmpty : ValidationError {
        override val message = "ファイル名を入力してください。"
    }

    data class ForbiddenChar(override val message: String) : ValidationError
    data object FileNameTooLong : ValidationError {
        override val message = "ファイル名は255文字以内で入力してください。"
    }

    data class ReservedName(override val message: String) : ValidationError
    data class AlreadyExists(override val message: String) : ValidationError
}

@Single
class FileValidationUseCase(private val fileRepository: io.github.arashiyama11.data.repository.IFileRepository) {
    suspend fun validateFileName(fileName: io.github.arashiyama11.model.FileName): Either<ValidationError, Unit> {

        if (fileName.value.isBlank()) {
            return ValidationError.FileNameEmpty.left()
        }

        forbiddenCharacters.forEach { char ->
            if (fileName.value.contains(char)) {
                return ValidationError.ForbiddenChar("ファイル名に使用できない文字 '$char' が含まれています。")
                    .left()
            }
        }

        if (fileName.value.length > MAX_FILE_NAME_LENGTH) {
            return ValidationError.FileNameTooLong.left()
        }


        if (reservedNames.contains(fileName.value.uppercase())) {
            return ValidationError.ReservedName("予約された名前は使用できません。").left()
        }

        val allFileNames = fileRepository.getAllFileNames()
        if (allFileNames != null && allFileNames.contains(fileName)) {
            return ValidationError.AlreadyExists("同じ名前のファイルがすでに存在します。").left()
        }
        return Unit.right()
    }

    companion object {
        const val MAX_FILE_NAME_LENGTH = 255
        val forbiddenCharacters = listOf(
            '/',
            '\\',
            ':',
            '*',
            '?',
            '"',
            '<',
            '>',
            '|',
            '\u0000',
            '\u0001',
            '\u0002',
            '\u0003',
            '\u0004',
            '\u0005',
            '\u0006',
            '\u0007',
            '\u0008',
            '\u0009',
            '\u000A',
            '\u000B',
            '\u000C',
            '\u000D',
            '\u000E',
            '\u000F',
            '\u0010',
            '\u0011',
            '\u0012',
            '\u0013',
            '\u0014',
            '\u0015',
            '\u0016',
            '\u0017',
            '\u0018',
            '\u0019',
            '\u001A',
            '\u001B',
            '\u001C',
            '\u001D',
            '\u001E',
            '\u001F'
        )

        val reservedNames = listOf(
            "CON", "PRN", "AUX", "NUL",
            "COM1", "COM2", "COM3", "COM4", "COM5", "COM6", "COM7", "COM8", "COM9",
            "LPT1", "LPT2", "LPT3", "LPT4", "LPT5", "LPT6", "LPT7", "LPT8", "LPT9"
        )
    }
}
