package io.github.arashiyama11.dncl.parser

import arrow.core.Either
import io.github.arashiyama11.dncl.model.AstNode
import io.github.arashiyama11.dncl.model.DnclError

interface IParser {
    fun parseProgram(): Either<DnclError, AstNode.Program>
}