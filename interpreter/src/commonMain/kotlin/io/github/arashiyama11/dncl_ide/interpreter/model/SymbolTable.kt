package io.github.arashiyama11.dncl_ide.interpreter.model

import io.github.arashiyama11.dncl_ide.interpreter.model.AstNode

data class Symbol(
    val name: String,
    val kind: SymbolKind,
    val range: IntRange,
    val type: String? = null, // 型情報 (例: "Int", "String", "Function")
    val definitionNode: AstNode? = null // 定義元のASTノード
)

enum class SymbolKind {
    VARIABLE,
    FUNCTION,
    PARAMETER,
    BUILT_IN_FUNCTION,
    UNKNOWN
}

class SymbolTable(private val outer: SymbolTable? = null) {
    private val store: MutableMap<String, Symbol> = mutableMapOf()

    fun define(symbol: Symbol) {
        store[symbol.name] = symbol
    }

    fun resolve(name: String): Symbol? {
        return store[name] ?: outer?.resolve(name)
    }

    fun allSymbols(): List<Symbol> {
        val symbols = mutableListOf<Symbol>()
        symbols.addAll(store.values)
        outer?.allSymbols()?.let { symbols.addAll(it) }
        return symbols.distinctBy { it.name }
    }

    fun createChildScope(): SymbolTable {
        return SymbolTable(this)
    }
}
