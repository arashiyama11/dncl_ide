package io.github.arashiyama11.dncl.evaluator

import arrow.core.Either
import io.github.arashiyama11.dncl.model.AstNode
import io.github.arashiyama11.dncl.model.DnclError
import io.github.arashiyama11.dncl.model.DnclObject
import io.github.arashiyama11.dncl.model.Environment

interface IEvaluator {
    fun eval(node: AstNode, env: Environment): Either<DnclError, DnclObject>
    fun evalProgram(
        program: AstNode.Program,
        env: Environment = Environment()
    ): Either<DnclError, DnclObject>
}