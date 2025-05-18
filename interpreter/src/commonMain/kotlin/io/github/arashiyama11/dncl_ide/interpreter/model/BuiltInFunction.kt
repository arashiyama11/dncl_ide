package io.github.arashiyama11.dncl_ide.interpreter.model

enum class BuiltInFunction(private val identifier: String) {
    PRINT("表示する"), LENGTH("要素数"), DIFF("差分"), RETURN("戻り値"),
    CONCAT("連結"), PUSH("末尾追加"), SHIFT("先頭削除"), UNSHIFT("先頭追加"),
    POP("末尾削除"), INT("整数変換"), FLOAT("浮動小数点変換"), STRING("文字列変換"),
    IMPORT("インポート"), CHAR_CODE("文字コード"), FROM_CHAR_CODE("コードから文字"),

    // 配列操作関数
    SLICE("部分配列"), JOIN("配列結合"), SORT("並べ替え"), REVERSE("逆順"), FIND("検索"),

    // 文字列操作関数
    SUBSTRING("部分文字列"), SPLIT("分割"), TRIM("空白除去"), REPLACE("置換"),

    // 数学関数
    ROUND("四捨五入"), FLOOR("切り捨て"), CEIL("切り上げ"), RANDOM("乱数"),
    MAX("最大値"), MIN("最小値"),

    // 型チェック関数
    IS_INT("整数判定"), IS_FLOAT("浮動小数点判定"), IS_STRING("文字列判定"),
    IS_ARRAY("配列判定"), IS_BOOLEAN("真偽値判定"),

    // システム関数
    CLEAR("出力消去"), SLEEP("待機");

    companion object {
        fun from(identifier: String): BuiltInFunction? =
            entries.find { it.identifier == identifier }

        fun allIdentifiers(): List<String> = entries.map { it.identifier }
    }
}
