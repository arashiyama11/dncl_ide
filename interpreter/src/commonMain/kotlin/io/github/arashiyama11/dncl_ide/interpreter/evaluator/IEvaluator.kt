package io.github.arashiyama11.dncl_ide.interpreter.evaluator

import arrow.core.Either
import io.github.arashiyama11.dncl_ide.interpreter.model.AstNode
import io.github.arashiyama11.dncl_ide.interpreter.model.DnclError
import io.github.arashiyama11.dncl_ide.interpreter.model.DnclObject
import io.github.arashiyama11.dncl_ide.interpreter.model.Environment

interface IEvaluator {
    fun eval(node: AstNode, env: Environment): Either<DnclError, DnclObject>
    fun evalProgram(
        program: AstNode.Program,
        env: Environment = Environment()
    ): Either<DnclError, DnclObject>
}