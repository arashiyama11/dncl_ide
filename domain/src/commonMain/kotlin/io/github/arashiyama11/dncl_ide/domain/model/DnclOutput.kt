package io.github.arashiyama11.dncl_ide.domain.model

import io.github.arashiyama11.dncl_ide.domain.canvas.CanvasFrame
import io.github.arashiyama11.dncl_ide.interpreter.model.DnclError
import io.github.arashiyama11.dncl_ide.interpreter.model.DnclObject
import io.github.arashiyama11.dncl_ide.interpreter.model.Environment
import kotlin.jvm.JvmInline


sealed interface DnclOutput {
    @JvmInline
    value class SyntaxError(val value: DnclError) : DnclOutput

    @JvmInline
    value class RuntimeError(val value: DnclObject.Error) : DnclOutput

    @JvmInline
    value class LineEvaluation(val value: Int) : DnclOutput

    @JvmInline
    value class EnvironmentUpdate(val environment: Environment) : DnclOutput

    @JvmInline
    value class WaitingForInput(val isWaiting: Boolean) : DnclOutput

    @JvmInline
    value class StdoutAppend(val value: String) : DnclOutput
    data object StdoutFlush : DnclOutput
    data object StdoutClear : DnclOutput
    data object StdoutCommitFrame : DnclOutput

    @JvmInline
    value class StdoutReplace(val value: String) : DnclOutput

    data class CanvasFrameOutput(val frame: CanvasFrame) : DnclOutput
}
