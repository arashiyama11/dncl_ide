package io.github.arashiyama11.dncl_ide.interpreter.model

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

    suspend fun set(string: String, obj: DnclObject) = mutex.withLock {
        store[string] = obj
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