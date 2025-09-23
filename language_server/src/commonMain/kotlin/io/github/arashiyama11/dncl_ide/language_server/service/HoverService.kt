package io.github.arashiyama11.dncl_ide.language_server.service

import io.github.arashiyama11.dncl_ide.interpreter.lexer.Lexer
import io.github.arashiyama11.dncl_ide.interpreter.model.Token
import io.github.arashiyama11.dncl_ide.interpreter.model.AstNode
import io.github.arashiyama11.dncl_ide.language_server.ast.SymbolKind
import io.github.arashiyama11.dncl_ide.interpreter.model.AllBuiltInFunction
import io.github.arashiyama11.dncl_ide.language_server.Hover
import io.github.arashiyama11.dncl_ide.language_server.MarkupContent
import io.github.arashiyama11.dncl_ide.language_server.util.calculatePosition

class HoverService(
    private val astInfoService: AstInfoService
) {
    fun getHover(code: String, offset: Int): Hover? {
        // First parse and analyze the code
        val astInfo = astInfoService.parseAndAnalyze(code) ?: return null

        val lexer = Lexer(code)
        val tokens = lexer.toList().mapNotNull { it.getOrNull() }

        val hoveredToken = tokens.firstOrNull { token ->
            offset in token.range
        }

        val hoverContent: String? = when (hoveredToken) {
            is Token.Japanese, is Token.Identifier -> {
                val symbol = astInfoService.findSymbolAtOffset(astInfo, offset)
                when (symbol?.kind) {
                    SymbolKind.VARIABLE -> {
                        val start = calculatePosition(code, symbol.range.first)
                        """**変数**: `${symbol.name}`  
                           定義位置: ${start.line + 1}行${start.character + 1}文字目  
                           ${if (symbol.type != null) "型: ${symbol.type}" else ""}""".trimIndent()
                    }

                    SymbolKind.FUNCTION -> {
                        val functionNode = symbol.definitionNode as? AstNode.FunctionStatement
                        val params =
                            functionNode?.parameters?.joinToString(", ") { it.literal } ?: ""
                        val start = calculatePosition(code, symbol.range.first)
                        """**関数**: `${symbol.name}($params)`  
                           定義位置: ${start.line + 1}行${start.character + 1}文字目  
                        """.trimIndent()
                    }

                    SymbolKind.PARAMETER -> {
                        val start = calculatePosition(code, symbol.range.first)
                        """**パラメータ**: `${symbol.name}`  
                           定義位置: ${start.line + 1}行${start.character + 1}文字目  
                           関数のパラメータとして定義されています""".trimIndent()
                    }

                    SymbolKind.BUILT_IN_FUNCTION -> {
                        val builtInFunction = AllBuiltInFunction.from(symbol.name)
                        if (builtInFunction != null) {
                            "**組み込み関数**: ${descriptionOfBuiltInFunction(builtInFunction)}"
                        } else {
                            "**組み込み関数**: `${symbol.name}`"
                        }
                    }

                    SymbolKind.UNKNOWN, null -> {
                        // シンボルが見つからない場合は組み込み関数かどうかチェック
                        val tokenText = hoveredToken.literal
                        val builtInFunction = AllBuiltInFunction.from(tokenText)
                        if (builtInFunction != null) {
                            "**組み込み関数**: ${descriptionOfBuiltInFunction(builtInFunction)}"
                        } else null
                    }
                }
            }

            is Token.Int -> {
                "**整数リテラル**: `${hoveredToken.literal}`\n\n" + "値: ${hoveredToken.literal}"
            }

            is Token.Float -> {
                "**浮動小数点リテラル**: `${hoveredToken.literal}`\n\n" +
                        "値: ${hoveredToken.literal}"
            }

            is Token.String -> {
                "**文字列リテラル**: `${hoveredToken.literal}`\n\n" +
                        "値: ${hoveredToken.literal}"
            }

            is Token.Boolean -> {
                "**ブール値リテラル**: `${hoveredToken.literal}`\n\n" +
                        "値: ${hoveredToken.value}"
            }

            else -> null
        }

        return hoverContent?.let { content ->
            Hover(
                contents = MarkupContent(
                    kind = "markdown",
                    value = content
                ),
                range = null
            )
        }
    }

    private fun descriptionOfBuiltInFunction(fn: AllBuiltInFunction): String {
        return when (fn) {
            AllBuiltInFunction.PRINT -> """
                    `表示する(message1, message2, ...)`  
                    与えられた引数を画面に表示する関数です。どの型の引数も受け付けます。  
                    複数指定した場合はスペース区切りで表示されます。  
                    使用例  
                    ```dncl
                    表示する("こんにちは")
                    表示する("数値:", 123, "フロート:", 3.14)
                    ```
                """.trimIndent()

            AllBuiltInFunction.LENGTH -> """
                    `要素数(list: 配列)`  
                    配列の要素数を取得して返す関数です。  
                    使用例
                    ```
                    count = 要素数([1, 2, 3])  # 3
                    ```
                """.trimIndent()

            AllBuiltInFunction.DIFF -> """
                    `差分(char: 文字)`  
                    文字1つの文字列を'a'からのオフセット（差分）として返す関数です。  
                    スペースの場合は-1を返します。
                    使用例
                    ```dncl
                    d1 = 差分("a")  # 0
                    d2 = 差分("c")  # 2
                    d3 = 差分(" ")  # -1
                    ```
                """.trimIndent()

            AllBuiltInFunction.RETURN -> """
                    `戻り値(value)`  
                    関数から値を返すためのキーワード関数です。  
                    使用例
                    ```dncl
                    関数 サンプル() を:
                      戻り値(42)
                    と定義する
                    ```
                """.trimIndent()

            AllBuiltInFunction.CONCAT -> """
                    `連結(array1, array2)`  
                    2つの配列を連結して新しい配列を返す関数です。  
                    使用例
                    ```dncl
                    a = [1, 2]
                    b = [3, 4]
                    c = 連結(a, b)  # [1, 2, 3, 4]
                    ```
                """.trimIndent()

            AllBuiltInFunction.PUSH -> """
                    `末尾追加(array, element)`  
                    配列の末尾に要素を追加する関数です。元の配列を変更します。  
                    使用例
                    ```dncl
                    arr = [1, 2]
                    末尾追加(arr, 3)
                    # arr は [1, 2, 3]
                    ```
                """.trimIndent()

            AllBuiltInFunction.SHIFT -> """
                    `先頭削除(array)`  
                    配列の先頭要素を削除し、その要素を返す関数です。  
                    使用例
                    ```dncl
                    arr = [1, 2, 3]
                    first = 先頭削除(arr)  # 1
                    # arr は [2, 3]
                    ```
                """.trimIndent()

            AllBuiltInFunction.UNSHIFT -> """
                    `先頭追加(array, element)`  
                    配列の先頭に要素を追加する関数です。元の配列を変更します。  
                    使用例
                    ```dncl
                    arr = [2, 3]
                    先頭追加(arr, 1)
                    # arr は [1, 2, 3]
                    ```
                """.trimIndent()

            AllBuiltInFunction.POP -> """
                    `末尾削除(array)`  
                    配列の末尾要素を削除し、その要素を返す関数です。  
                    使用例
                    ```dncl
                    arr = [1, 2, 3]
                    last = 末尾削除(arr)  # 3
                    # arr は [1, 2]
                    ```
                """.trimIndent()

            AllBuiltInFunction.INT -> """
                    `整数変換(value)`  
                    文字列や小数を整数に変換する関数です。文字列が数値でない場合は0になります。  
                    使用例
                    ```dncl
                    i1 = 整数変換("123")   # 123
                    i2 = 整数変換(3.99)     # 3
                    ```
                """.trimIndent()

            AllBuiltInFunction.FLOAT -> """
                    `浮動小数点変換(value)`  
                    文字列や整数を浮動小数点数に変換する関数です。文字列が数値でない場合は0.0になります。  
                    使用例
                    ```dncl
                    f1 = 浮動小数点変換("3.14")  # 3.14
                    f2 = 浮動小数点変換(2)       # 2.0
                    ```
                """.trimIndent()

            AllBuiltInFunction.STRING -> """
                    `文字列変換(value)`  
                    任意の値を文字列に変換する関数です。  
                    使用例
                    ```dncl
                    s1 = 文字列変換(123)     # "123"
                    s2 = 文字列変換(真)      # "true"
                    ```
                """.trimIndent()

            AllBuiltInFunction.IMPORT -> """
                    `インポート(moduleName)`  
                    外部モジュールやファイルをインポートする関数です。返り値はインポート結果オブジェクトです。  
                    使用例
                    ```dncl
                    m = インポート("モジュール名.dncl")
                    ```
                """.trimIndent()

            AllBuiltInFunction.CHAR_CODE -> """
                    `文字コード(char)`  
                    1文字のUnicodeコードポイントを取得して整数で返す関数です。  
                    使用例
                    ```dncl
                    code = 文字コード("A")  # 65
                    ```
                """.trimIndent()

            AllBuiltInFunction.FROM_CHAR_CODE -> """
                    `コードから文字(codePoint)`  
                    整数のUnicodeコードポイントから文字列を生成する関数です。  
                    使用例
                    ```dncl
                    ch = コードから文字(65)  # "A"
                    ```
                """.trimIndent()

            AllBuiltInFunction.SLICE -> """
                    `部分配列(array, start, end)`  
                    配列の指定開始から終了直前までを切り出して新しい配列を返す関数です。  
                    使用例
                    ```dncl
                    part = 部分配列([1,2,3,4], 1, 3)  # [2,3]
                    ```
                """.trimIndent()

            AllBuiltInFunction.JOIN -> """
                    `配列結合(array, separator)`  
                    配列の要素を区切り文字で結合して文字列にする関数です。  
                    使用例
                    ```dncl
                    s = 配列結合(["A","B","C"], ",")  # "A,B,C"
                    ```
                """.trimIndent()

            AllBuiltInFunction.SORT -> """
                    `並べ替え(array)`  
                    配列を昇順に並べ替えて新しい配列を返す関数です。  
                    使用例
                    ```dncl
                    sorted = 並べ替え([3,1,2])  # [1,2,3]
                    ```
                """.trimIndent()

            AllBuiltInFunction.REVERSE -> """
                    `逆順(array)`  
                    配列の要素順を逆にして新しい配列を返す関数です。  
                    使用例
                    ```dncl
                    rev = 逆順([1,2,3])  # [3,2,1]
                    ```
                """.trimIndent()

            AllBuiltInFunction.FIND -> """
                    `検索(array, value)`  
                    配列の中から指定の値と一致する最初の要素のインデックスを返す関数です。  
                    使用例
                    ```dncl
                    idx = 検索(["a","b","c"], "b")  # 1
                    ```
                """.trimIndent()

            AllBuiltInFunction.SUBSTRING -> """
                    `部分文字列(string, start, end)`  
                    文字列の指定範囲を切り取って返す関数です。（開始インデックス含む、終了インデックス直前まで）  
                    使用例
                    ```dncl
                    sub = 部分文字列("Hello", 1, 4)  # "ell"
                    ```
                """.trimIndent()

            AllBuiltInFunction.SPLIT -> """
                    `分割(string, delimiter)`  
                    文字列を区切り文字で分割し、文字列配列を返す関数です。  
                    使用例
                    ```dncl
                    parts = 分割("a,b,c", ",")  # ["a","b","c"]
                    ```
                """.trimIndent()

            AllBuiltInFunction.TRIM -> """
                    `空白除去(string)`  
                    文字列の先頭と末尾の空白を取り除く関数です。  
                    使用例
                    ```dncl
                    t = 空白除去("  test  ")  # "test"
                    ```
                """.trimIndent()

            AllBuiltInFunction.STDOUT_REPLACE -> """
                    `置換(string, target, replacement)`  
                    文字列中の指定部分を別の文字列に置換して返す関数です。  
                    使用例
                    ```dncl
                    r = 置換("abcabc", "b", "x")  # "axcaxc"
                    ```
                """.trimIndent()

            AllBuiltInFunction.ROUND -> """
                    `四捨五入(float)`  
                    小数を四捨五入して整数として返す関数です。  
                    使用例
                    ```dncl
                    n = 四捨五入(3.5)  # 4
                    ```
                """.trimIndent()

            AllBuiltInFunction.FLOOR -> """
                    `切り捨て(float)`  
                    小数を切り捨てて整数として返す関数です。  
                    使用例
                    ```dncl
                    n = 切り捨て(3.9)  # 3
                    ```
                """.trimIndent()

            AllBuiltInFunction.CEIL -> """
                    `切り上げ(float)`  
                    小数を切り上げて整数として返す関数です。  
                    使用例
                    ```dncl
                    n = 切り上げ(3.1)  # 4
                    ```
                """.trimIndent()

            AllBuiltInFunction.RANDOM -> """
                    `乱数()`  
                    0以上1未満の乱数を生成する関数です。  
                    使用例
                    ```dncl
                    r = 乱数()
                    ```
                """.trimIndent()

            AllBuiltInFunction.MAX -> """
                    `最大値(num1, num2, ...)`  
                    複数の数値の中から最大値を返す関数です。  
                    使用例
                    ```dncl
                    m = 最大値(1, 3, 2)  # 3
                    ```
                """.trimIndent()

            AllBuiltInFunction.MIN -> """
                    `最小値(num1, num2, ...)`  
                    複数の数値の中から最小値を返す関数です。  
                    使用例
                    ```dncl
                    m = 最小値(1, 3, 2)  # 1
                    ```
                """.trimIndent()

            AllBuiltInFunction.IS_INT -> """
                    `整数判定(value)`  
                    値が整数かどうかを判定し、真偽値を返す関数です。  
                    使用例
                    ```dncl
                    b = 整数判定(3)    # 真
                    b = 整数判定(3.5)  # 偽
                    ```
                """.trimIndent()

            AllBuiltInFunction.IS_FLOAT -> """
                    `浮動小数点判定(value)`  
                    値が浮動小数点数かどうかを判定し、真偽値を返す関数です。  
                    使用例
                    ```dncl
                    b = 浮動小数点判定(3.5)  # 真
                    b = 浮動小数点判定(3)    # 偽
                    ```
                """.trimIndent()

            AllBuiltInFunction.IS_STRING -> """
                    `文字列判定(value)`  
                    値が文字列かどうかを判定し、真偽値を返す関数です。  
                    使用例
                    ```dncl
                    b = 文字列判定("試験")  # 真
                    b = 文字列判定(123)     # 偽
                    ```
                """.trimIndent()

            AllBuiltInFunction.IS_ARRAY -> """
                    `配列判定(value)`  
                    値が配列かどうかを判定し、真偽値を返す関数です。  
                    使用例
                    ```dncl
                    b = 配列判定([1,2])  # 真
                    b = 配列判定("a")  # 偽
                    ```
                """.trimIndent()

            AllBuiltInFunction.IS_BOOLEAN -> """
                    `真偽値判定(value)`  
                    値が真偽値かどうかを判定し、真偽値を返す関数です。  
                    使用例
                    ```dncl
                    b = 真偽値判定(真)  # 真
                    b = 真偽値判定(1)  # 偽
                    ```
                """.trimIndent()

            AllBuiltInFunction.CLEAR -> """
                    `出力消去()`  
                    出力を消去する関数です。  
                    使用例
                    ```dncl
                    出力消去()
                    ```
                """.trimIndent()

            AllBuiltInFunction.SLEEP -> """
                    `待機(milliseconds)`  
                    指定したミリ秒だけ処理を停止する関数です。  
                    使用例
                    ```dncl
                    待機(1000)  # 1秒停止
                    ```
                """.trimIndent()

            AllBuiltInFunction.STRING_REPLACE -> """
                    `置換(string, target, replacement)`  
                    文字列中の指定部分を別の文字列に置換して返す関数です。  
                    使用例
                    ```dncl
                    r = 置換("abcabc", "b", "x")  # "axcaxc"
                    ```
            """.trimIndent()

            AllBuiltInFunction.APPEND -> """
                    `追加(text)`  
                    出力にテキストを追加する関数です。  
                    使用例
                    ```dncl
                    追加("Hello, ")
                    追加("world!")
                    ```
            """.trimIndent()

            AllBuiltInFunction.FLUSH -> """
                    `フラッシュ()`  
                    出力をフラッシュ（強制表示）する関数です。  
                    使用例
                    ```dncl
                    追加("Loading...")
                    フラッシュ()
                    ```
            """.trimIndent()

            AllBuiltInFunction.COMMIT_FRAME -> """
                    `フレーム確定()`  
                    現在の出力フレームを確定し、新しいフレームを開始する関数です。  
                    使用例
                    ```dncl
                    追加("Frame 1")
                    フレーム確定()
                    追加("Frame 2")
                    ```
            """.trimIndent()
        }
    }
}
