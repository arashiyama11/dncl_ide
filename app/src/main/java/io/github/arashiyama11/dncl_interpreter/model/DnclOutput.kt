package io.github.arashiyama11.dncl_interpreter.model

sealed interface DnclOutput {
    @JvmInline
    value class Stdout(val value: String) : DnclOutput

    @JvmInline
    value class Stderr(val value: String) : DnclOutput
}