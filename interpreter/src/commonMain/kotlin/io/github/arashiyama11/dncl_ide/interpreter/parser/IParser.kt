package io.github.arashiyama11.dncl_ide.interpreter.parser

import arrow.core.Either
import io.github.arashiyama11.dncl_ide.interpreter.model.AstNode
import io.github.arashiyama11.dncl_ide.interpreter.model.DnclError

interface IParser {
    fun parseProgram(): Either<DnclError, AstNode.Program>
}