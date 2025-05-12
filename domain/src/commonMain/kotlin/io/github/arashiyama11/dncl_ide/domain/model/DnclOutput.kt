package io.github.arashiyama11.dncl_ide.domain.model

import io.github.arashiyama11.dncl_ide.interpreter.model.DnclObject
import io.github.arashiyama11.dncl_ide.interpreter.model.Environment
import kotlin.jvm.JvmInline


sealed interface DnclOutput {
    @JvmInline
    value class Stdout(val value: String) : DnclOutput

    @JvmInline
    value class Error(val value: String) : DnclOutput

    @JvmInline
    value class RuntimeError(val value: DnclObject.Error) : DnclOutput

    @JvmInline
    value class LineEvaluation(val value: Int) : DnclOutput

    @JvmInline
    value class EnvironmentUpdate(val environment: Environment) : DnclOutput

    object Clear : DnclOutput
}
