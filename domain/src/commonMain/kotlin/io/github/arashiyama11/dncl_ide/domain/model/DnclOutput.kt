package io.github.arashiyama11.dncl_ide.domain.model

import io.github.arashiyama11.dncl_ide.interpreter.model.DnclObject
import kotlin.jvm.JvmInline


sealed interface DnclOutput {
    @JvmInline
    value class Stdout(val value: String) : DnclOutput

    data class Error(val value: String) : DnclOutput

    data class RuntimeError(val value: DnclObject.Error) : DnclOutput
}