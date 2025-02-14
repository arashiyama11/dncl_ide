package io.github.arashiyama11.dncl.model

class Environment(private val outer: Environment? = null) {
    private val store: MutableMap<String, DnclObject> = mutableMapOf()

    fun get(string: String): DnclObject? {
        return store[string] ?: outer?.get(string)
    }

    fun set(string: String, obj: DnclObject) {
        store[string] = obj
    }

    fun createChildEnvironment(): Environment {
        return Environment(this)
    }
}