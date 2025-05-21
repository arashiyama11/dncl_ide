package io.github.arashiyama11.dncl_ide.interpreter.evaluator

import arrow.core.Either
import arrow.core.raise.either
import io.github.arashiyama11.dncl_ide.interpreter.model.AstNode
import io.github.arashiyama11.dncl_ide.interpreter.model.AllBuiltInFunction
import io.github.arashiyama11.dncl_ide.interpreter.model.DnclError
import io.github.arashiyama11.dncl_ide.interpreter.model.DnclObject
import io.github.arashiyama11.dncl_ide.interpreter.model.Environment
import io.github.arashiyama11.dncl_ide.interpreter.model.InternalError
import io.github.arashiyama11.dncl_ide.interpreter.model.SystemCommand
import io.github.arashiyama11.dncl_ide.interpreter.model.Token

interface CallBuiltInFunctionScope {
    val evaluator: Evaluator
    val fn: AllBuiltInFunction
    val args: List<DnclObject>
    val env: Environment
    val astNode: AstNode
}

class Evaluator(
    private val onCallSystemCommand: (SystemCommand) -> DnclObject,
    private val arrayOrigin: Int = 0,
    private val onEval: (suspend (AstNode, Environment) -> Unit)? = null
) : IEvaluator {
    private inline fun DnclObject.onReturnValueOrError(action: (DnclObject) -> Unit): DnclObject {
        if (this is DnclObject.ReturnValue || this is DnclObject.Error) action(this)
        return this
    }

    override suspend fun eval(node: AstNode, env: Environment): Either<DnclError, DnclObject> =
        either {
            Either.catch {
                onEval?.invoke(node, env)
                when (node) {
                    is AstNode.Program -> evalProgram(node, env).bind()
                    is AstNode.BlockStatement -> evalBlockStatement(node, env).bind()
                    is AstNode.WhileStatement -> evalWhileStatement(node, env).bind()
                    is AstNode.IfStatement -> evalIfStatement(node, env).bind()
                    is AstNode.ForStatement -> evalForStatement(node, env).bind()
                    is AstNode.AssignStatement -> evalAssignStatement(node, env).bind()
                    is AstNode.ExpressionStatement -> evalExpressionStatement(node, env).bind()
                    is AstNode.FunctionStatement -> evalFunctionStatement(node, env).bind()

                    is AstNode.IndexExpression -> evalIndexExpression(node, env).bind()
                    is AstNode.CallExpression -> evalCallExpression(node, env).bind()
                    is AstNode.InfixExpression -> evalInfixExpression(node, env).bind()
                    is AstNode.PrefixExpression -> evalPrefixExpression(node, env).bind()

                    is AstNode.ArrayLiteral -> evalArrayLiteral(node, env).bind()
                    is AstNode.Identifier -> evalIdentifier(node, env).bind()
                    is AstNode.FloatLiteral -> DnclObject.Float(node.value, node)
                    is AstNode.IntLiteral -> DnclObject.Int(node.value, node)
                    is AstNode.StringLiteral -> DnclObject.String(node.value, node)
                    is AstNode.SystemLiteral -> onCallSystemCommand(SystemCommand.from(node))
                    is AstNode.FunctionLiteral -> DnclObject.Function(
                        null,
                        node.parameters, node.body, env.createChildEnvironment(), node
                    )

                    is AstNode.WhileExpression -> raise(InternalError("while式はサポートされていません"))
                }
            }.mapLeft { InternalError(it.message ?: "") }.bind()
        }

    override suspend fun evalProgram(
        program: AstNode.Program, env: Environment
    ): Either<DnclError, DnclObject> =
        either {
            Either.catch {
                var result: DnclObject = DnclObject.Null(program)
                for (stmt in program.statements) {
                    result = eval(stmt, env).bind().onReturnValueOrError { return@either it }
                }
                result
            }.mapLeft { InternalError(it.message ?: "") }.bind()
        }

    private suspend fun evalBlockStatement(
        block: AstNode.BlockStatement,
        env: Environment
    ): Either<DnclError, DnclObject> =
        either {
            var result: DnclObject = DnclObject.Null(block)
            for (stmt in block.statements) {
                result = eval(stmt, env).bind().onReturnValueOrError { return@either it }
            }
            result
        }

    private suspend fun evalIfStatement(
        ifStmt: AstNode.IfStatement,
        env: Environment
    ): Either<DnclError, DnclObject> = either {
        val condition = eval(ifStmt.condition, env).bind().onReturnValueOrError { return@either it }
        if (isTruthy(condition)) {
            eval(ifStmt.consequence, env).bind()
        } else if (ifStmt.alternative != null) {
            eval(ifStmt.alternative, env).bind()
        } else {
            DnclObject.Null(ifStmt)
        }
    }

    private suspend fun evalWhileStatement(
        whileStmt: AstNode.WhileStatement,
        env: Environment
    ): Either<DnclError, DnclObject> = either {
        var condition =
            eval(whileStmt.condition, env).bind().onReturnValueOrError { return@either it }
        while (isTruthy(condition)) {
            eval(whileStmt.block, env).bind().onReturnValueOrError { return@either it }
            condition =
                eval(whileStmt.condition, env).bind().onReturnValueOrError { return@either it }
        }
        DnclObject.Null(whileStmt)
    }

    private suspend fun evalForStatement(
        forStmt: AstNode.ForStatement,
        env: Environment
    ): Either<DnclError, DnclObject> = either {
        val loopCounter = forStmt.loopCounter.literal
        val start = eval(forStmt.start, env).bind().onReturnValueOrError { return@either it }
        val end = eval(forStmt.end, env).bind().onReturnValueOrError { return@either it }
        val step = eval(forStmt.step, env).bind().onReturnValueOrError { return@either it }
        val stepType = forStmt.stepType
        val block = forStmt.block
        env.set(loopCounter, start)
        if (start !is DnclObject.Int) return@either DnclObject.TypeError(
            "Int",
            start::class.simpleName ?: "", start.astNode
        )
        if (end !is DnclObject.Int) return@either DnclObject.TypeError(
            "Int",
            end::class.simpleName ?: "", end.astNode
        )
        if (step !is DnclObject.Int) return@either DnclObject.TypeError(
            "Int",
            step::class.simpleName ?: "", step.astNode
        )
        while (true) {
            val loopCounterValue =
                env.get(loopCounter) ?: raise(InternalError("ループカウンターが見つかりません"))
            if (stepType == AstNode.ForStatement.Companion.StepType.INCREMENT) {
                if (loopCounterValue !is DnclObject.Int) return@either DnclObject.TypeError(
                    "Int",
                    loopCounterValue::class.simpleName ?: "", loopCounterValue.astNode
                )
                if (loopCounterValue.value > end.value) break
                eval(block, env).bind().onReturnValueOrError { return@either it }
                env.set(
                    loopCounter,
                    DnclObject.Int(loopCounterValue.value + step.value, loopCounterValue.astNode)
                )
            } else {
                if (loopCounterValue !is DnclObject.Int) break
                if (loopCounterValue.value < end.value) break
                eval(block, env).bind().onReturnValueOrError { return@either it }
                env.set(
                    loopCounter,
                    DnclObject.Int(loopCounterValue.value - step.value, loopCounterValue.astNode)
                )
            }
        }
        DnclObject.Null(forStmt)
    }

    private suspend fun evalAssignStatement(
        assignStmt: AstNode.AssignStatement,
        env: Environment
    ): Either<DnclError, DnclObject> = either {
        for ((id, value) in assignStmt.assignments) {
            when (id) {
                is AstNode.Identifier -> env.set(
                    id.value,
                    eval(value, env).bind().onReturnValueOrError { return@either it })

                is AstNode.IndexExpression -> {
                    val array = eval(id.left, env).bind().onReturnValueOrError { return@either it }
                    if (array !is DnclObject.Array) return@either DnclObject.TypeError(
                        "Array",
                        array::class.simpleName ?: "", array.astNode
                    )
                    val index = eval(id.right, env).bind().onReturnValueOrError { return@either it }
                    if (index !is DnclObject.Int) return@either DnclObject.TypeError(
                        "Int",
                        index::class.simpleName ?: "", index.astNode
                    )
                    if (index.value - arrayOrigin in array.value.indices)
                        array.value[index.value - arrayOrigin] =
                            eval(value, env).bind().onReturnValueOrError { return@either it }
                    else return@either DnclObject.IndexOutOfRangeError(
                        index.value - arrayOrigin,
                        array.value.size,
                        index.astNode
                    )
                }
            }
        }
        DnclObject.Null(assignStmt)
    }

    private suspend fun evalExpressionStatement(
        exprStmt: AstNode.ExpressionStatement,
        env: Environment
    ): Either<DnclError, DnclObject> = eval(exprStmt.expression, env)

    private suspend fun evalFunctionStatement(
        functionStmt: AstNode.FunctionStatement,
        env: Environment
    ): Either<DnclError, DnclObject> = either {
        val fn = DnclObject.Function(
            functionStmt.name,
            functionStmt.parameters,
            functionStmt.block,
            env.createChildEnvironment(), functionStmt
        )
        env.set(functionStmt.name, fn)
        fn.env.set(functionStmt.name, fn)
        DnclObject.Null(functionStmt)
    }

    private suspend fun evalIndexExpression(
        indexExpression: AstNode.IndexExpression,
        env: Environment
    ): Either<DnclError, DnclObject> = either {
        val array = eval(indexExpression.left, env).bind().onReturnValueOrError { return@either it }
        if (array !is DnclObject.Array) return@either DnclObject.TypeError(
            "Array",
            array::class.simpleName ?: "", array.astNode
        )
        val index =
            eval(indexExpression.right, env).bind().onReturnValueOrError { return@either it }
        if (index !is DnclObject.Int) return@either DnclObject.TypeError(
            "Int",
            index::class.simpleName ?: "", index.astNode
        )
        if (index.value - arrayOrigin in array.value.indices)
            array.value[index.value - arrayOrigin]
        else DnclObject.IndexOutOfRangeError(
            index.value - arrayOrigin,
            array.value.size,
            index.astNode
        )
    }

    private suspend fun evalCallExpression(
        callExpression: AstNode.CallExpression,
        env: Environment
    ): Either<DnclError, DnclObject> = either {
        val func =
            eval(callExpression.function, env).bind().onReturnValueOrError { return@either it }
        if (func is DnclObject.BuiltInFunction) {
            val args = callExpression.arguments.map {
                eval(it, env).bind().onReturnValueOrError { return@either it }
            }
            val scope = object : CallBuiltInFunctionScope {
                override val args: List<DnclObject> = args
                override val env: Environment = env
                override val evaluator: Evaluator = this@Evaluator
                override val fn: AllBuiltInFunction = func.identifier
                override val astNode: AstNode = callExpression
            }
            return@either func.execute(scope)
        }

        if (func !is DnclObject.Function) {
            return@either DnclObject.TypeError(
                "Function",
                func::class.simpleName ?: "", func.astNode
            )
        }
        val args = callExpression.arguments.map {
            eval(it, env).bind().onReturnValueOrError { return@either it }
        }
        val funcChild = func.env.createChildEnvironment()
        for ((param, arg) in func.parameters.zip(args)) {
            funcChild.set(param, arg)
        }
        val res = eval(func.body, funcChild).bind()
        if (res is DnclObject.ReturnValue) res.value else res
    }

    private suspend fun evalPrefixExpression(
        prefixExpression: AstNode.PrefixExpression,
        env: Environment
    ): Either<DnclError, DnclObject> = either {
        val right =
            eval(prefixExpression.right, env).bind().onReturnValueOrError { return@either it }
        when (prefixExpression.operator) {
            is Token.Bang -> DnclObject.Boolean(!isTruthy(right), prefixExpression)
            is Token.Minus -> when (right) {
                is DnclObject.Int -> DnclObject.Int(-right.value, prefixExpression)
                is DnclObject.Float -> DnclObject.Float(-right.value, prefixExpression)
                else -> DnclObject.TypeError(
                    "Int or Float",
                    right::class.simpleName ?: "",
                    prefixExpression
                )
            }

            is Token.Plus -> when (right) {
                is DnclObject.Int -> DnclObject.Int(+right.value, prefixExpression)
                is DnclObject.Float -> DnclObject.Float(+right.value, prefixExpression)
                else -> DnclObject.TypeError(
                    "Int or Float",
                    right::class.simpleName ?: "",
                    prefixExpression
                )
            }
        }
    }

    private suspend fun evalInfixExpression(
        infixExpression: AstNode.InfixExpression,
        env: Environment
    ): Either<DnclError, DnclObject> = either {
        val left = eval(infixExpression.left, env).bind().onReturnValueOrError { return@either it }
        val right =
            eval(infixExpression.right, env).bind().onReturnValueOrError { return@either it }
        when (infixExpression.operator) {
            is Token.Plus -> when {
                left is DnclObject.Int && right is DnclObject.Int -> DnclObject.Int(
                    left.value + right.value,
                    infixExpression
                )

                left is DnclObject.Float && right is DnclObject.Float -> DnclObject.Float(
                    left.value + right.value,
                    infixExpression
                )

                left is DnclObject.String && right is DnclObject.String -> DnclObject.String(
                    left.value + right.value,
                    infixExpression
                )

                left is DnclObject.Array && right is DnclObject.Array -> DnclObject.Array(
                    (left.value + right.value).toMutableList(),
                    infixExpression
                )

                else -> DnclObject.TypeError(
                    "Int, Float or String",
                    "${left::class.simpleName} and ${right::class.simpleName}", infixExpression
                )
            }

            is Token.Minus -> when {
                left is DnclObject.Int && right is DnclObject.Int -> DnclObject.Int(
                    left.value - right.value,
                    infixExpression
                )

                left is DnclObject.Float && right is DnclObject.Float -> DnclObject.Float(
                    left.value - right.value,
                    infixExpression
                )

                else -> DnclObject.TypeError(
                    "Int or Float",
                    "${left::class.simpleName} and ${right::class.simpleName}", infixExpression
                )
            }

            is Token.Times -> when {
                left is DnclObject.Int && right is DnclObject.Int -> DnclObject.Int(
                    left.value * right.value,
                    infixExpression
                )

                left is DnclObject.Float && right is DnclObject.Float -> DnclObject.Float(
                    left.value * right.value,
                    infixExpression
                )

                else -> DnclObject.TypeError(
                    "Int or Float",
                    "${left::class.simpleName} and ${right::class.simpleName}", infixExpression
                )
            }

            is Token.Divide -> when {
                left is DnclObject.Int && right is DnclObject.Int -> DnclObject.Float(
                    left.value.toFloat() / right.value,
                    infixExpression
                )

                left is DnclObject.Float && right is DnclObject.Float -> DnclObject.Float(
                    left.value / right.value,
                    infixExpression
                )

                else -> DnclObject.TypeError(
                    "Int or Float",
                    "${left::class.simpleName} and ${right::class.simpleName}", infixExpression
                )
            }

            is Token.DivideInt -> when {
                left is DnclObject.Int && right is DnclObject.Int -> DnclObject.Int(
                    left.value / right.value,
                    infixExpression
                )

                left is DnclObject.Float && right is DnclObject.Float -> DnclObject.Int(
                    (left.value / right.value).toInt(),
                    infixExpression
                )

                else -> DnclObject.TypeError(
                    "Int or Float",
                    "${left::class.simpleName} and ${right::class.simpleName}", infixExpression
                )

            }

            is Token.Modulo -> when {
                left is DnclObject.Int && right is DnclObject.Int -> DnclObject.Int(
                    left.value % right.value,
                    infixExpression
                )

                else -> DnclObject.TypeError(
                    "Int",
                    "${left::class.simpleName} and ${right::class.simpleName}", infixExpression
                )
            }

            is Token.Equal -> if (left::class == right::class) DnclObject.Boolean(
                left.hash() == right.hash(),
                infixExpression
            ) else DnclObject.Boolean(false, infixExpression)

            is Token.NotEqual -> if (left::class != right::class) DnclObject.Boolean(
                true,
                infixExpression
            ) else DnclObject.Boolean(left.hash() != right.hash(), infixExpression)

            is Token.LessThan -> when {
                left is DnclObject.Int && right is DnclObject.Int -> DnclObject.Boolean(
                    left.value < right.value,
                    infixExpression
                )

                left is DnclObject.Float && right is DnclObject.Float -> DnclObject.Boolean(
                    left.value < right.value,
                    infixExpression
                )

                else -> DnclObject.TypeError(
                    "Int or Float",
                    "${left::class.simpleName} and ${right::class.simpleName}", infixExpression
                )
            }

            is Token.LessThanOrEqual -> when {
                left is DnclObject.Int && right is DnclObject.Int -> DnclObject.Boolean(
                    left.value <= right.value,
                    infixExpression
                )

                left is DnclObject.Float && right is DnclObject.Float -> DnclObject.Boolean(
                    left.value <= right.value,
                    infixExpression
                )

                else -> DnclObject.TypeError(
                    "Int or Float",
                    "${left::class.simpleName} and ${right::class.simpleName}", infixExpression
                )
            }

            is Token.GreaterThan -> when {
                left is DnclObject.Int && right is DnclObject.Int -> DnclObject.Boolean(
                    left.value > right.value,
                    infixExpression
                )

                left is DnclObject.Float && right is DnclObject.Float -> DnclObject.Boolean(
                    left.value > right.value,
                    infixExpression
                )

                else -> DnclObject.TypeError(
                    "Int or Float",
                    "${left::class.simpleName} and ${right::class.simpleName}", infixExpression
                )
            }

            is Token.GreaterThanOrEqual -> when {
                left is DnclObject.Int && right is DnclObject.Int -> DnclObject.Boolean(
                    left.value >= right.value,
                    infixExpression
                )

                left is DnclObject.Float && right is DnclObject.Float -> DnclObject.Boolean(
                    left.value >= right.value,
                    infixExpression
                )

                else -> DnclObject.TypeError(
                    "Int or Float",
                    "${left::class.simpleName} and ${right::class.simpleName}", infixExpression
                )
            }

            is Token.And -> when {
                left is DnclObject.Boolean && right is DnclObject.Boolean -> DnclObject.Boolean(
                    left.value && right.value,
                    infixExpression
                )

                else -> DnclObject.TypeError(
                    "Boolean",
                    "${left::class.simpleName} and ${right::class.simpleName}", infixExpression
                )
            }

            is Token.Or -> when {
                left is DnclObject.Boolean && right is DnclObject.Boolean -> DnclObject.Boolean(
                    left.value || right.value,
                    infixExpression
                )

                else -> DnclObject.TypeError(
                    "Boolean",
                    "${left::class.simpleName} and ${right::class.simpleName}", infixExpression
                )
            }
        }
    }

    private suspend fun evalArrayLiteral(
        arrayLiteral: AstNode.ArrayLiteral,
        env: Environment
    ): Either<DnclError, DnclObject> = either {
        val elements = arrayLiteral.elements.map {
            eval(it, env).bind().onReturnValueOrError { return@either it }
        }
        DnclObject.Array(elements.toMutableList(), arrayLiteral)
    }

    private fun evalIdentifier(
        identifier: AstNode.Identifier,
        env: Environment
    ): Either<DnclError, DnclObject> = either {
        env.get(identifier.value)
            ?: DnclObject.UndefinedError(
                "変数「${identifier.value}」は定義されていません",
                identifier
            )
    }

    private fun isTruthy(obj: DnclObject): Boolean = when (obj) {
        is DnclObject.Boolean -> obj.value
        is DnclObject.Int -> obj.value != 0
        is DnclObject.Float -> obj.value != 0.0f
        is DnclObject.String -> obj.value.isNotEmpty()
        is DnclObject.Array -> obj.value.isNotEmpty()
        else -> false
    }
}