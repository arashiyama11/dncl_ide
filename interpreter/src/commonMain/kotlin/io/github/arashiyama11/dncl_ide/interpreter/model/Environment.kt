package io.github.arashiyama11.dncl_ide.interpreter.model

import arrow.core.Either
import arrow.core.Left
import arrow.core.Right
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock

class Environment(private val outer: Environment? = null) {
    val store: MutableMap<String, DnclObject> = mutableMapOf()
    val mutex = Mutex()

    fun allKeys(): Set<String> {
        return store.keys + outer?.allKeys().orEmpty()
    }

    fun get(string: String): DnclObject? {
        return store[string] ?: outer?.get(string)
    }

    suspend fun set(string: String, obj: DnclObject): Either<DnclObject.CannotAssignNothingError, Unit> {
        if (obj is DnclObject.Nothing) {
            return Left(
                DnclObject.CannotAssignNothingError(
                    "変数「${string}」にNothingを代入することはできません",
                    obj.astNode
                )
            )
        }
        mutex.withLock {
            store[string] = obj
        }
        return Right(Unit)
    }

    fun createChildEnvironment(): Environment {
        return Environment(this)
    }

    suspend fun copy(): Environment = mutex.withLock {
        val newEnv = Environment(outer)
        newEnv.store.putAll(store)
        return newEnv
    }

    override fun toString(): String {
        return store.toString()
    }
}