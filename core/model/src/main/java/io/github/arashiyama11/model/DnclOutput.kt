package io.github.arashiyama11.model

import io.github.arashiyama11.dncl.model.DnclObject

sealed interface DnclOutput {
    @JvmInline
    value class Stdout(val value: String) : DnclOutput

    data class Error(val value: String) : DnclOutput

    data class RuntimeError(val value: DnclObject.Error) : DnclOutput
}