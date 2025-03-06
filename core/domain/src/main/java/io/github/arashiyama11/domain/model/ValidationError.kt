package io.github.arashiyama11.domain.model

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