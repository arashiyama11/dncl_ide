package io.github.arashiyama11.dncl_ide.interpreter.model


enum class Precedence {
    LOWEST,
    WHILE,
    OR,
    AND,
    EQUALS,
    LESSGREATER,
    SUM,
    PRODUCT,
    PREFIX,
    CALL,
    INDEX
}

