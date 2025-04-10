package io.github.arashiyama11.dncl_ide.interpreter.model

enum class BuiltInFunction(private val identifier: String) {
    PRINT("表示する"), LENGTH("要素数"), DIFF("差分"), RETURN("戻り値"),
    CONCAT("連結"), PUSH("末尾追加"), SHIFT("先頭削除"), UNSHIFT("先頭追加"),
    POP("末尾削除"), INT("整数変換"), FLOAT("浮動小数点変換"), STRING("文字列変換"),
    IMPORT("インポート"), CHAR_CODE("文字コード"), FROM_CHAR_CODE("コードから文字");

    companion object {
        fun from(identifier: String): BuiltInFunction? =
            entries.find { it.identifier == identifier }
    }
}