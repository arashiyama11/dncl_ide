package io.github.arashiyama11.dncl_ide.domain.model

import io.github.arashiyama11.dncl_ide.interpreter.model.DnclObject
import io.github.arashiyama11.dncl_ide.interpreter.model.Environment
import kotlin.jvm.JvmInline


sealed interface DnclOutput {
    @JvmInline
    value class Stdout(val value: String) : DnclOutput

    data class Error(val value: String) : DnclOutput

    data class RuntimeError(val value: DnclObject.Error) : DnclOutput

    data class LineEvaluation(val lineNumber: Int) : DnclOutput

    data class EnvironmentUpdate(val environment: Environment) : DnclOutput

    object Clear : DnclOutput
}
