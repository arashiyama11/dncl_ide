package io.github.arashiyama11.dncl

import io.github.arashiyama11.dncl.lexer.ILexer
import io.github.arashiyama11.dncl.lexer.Lexer
import io.github.arashiyama11.dncl.model.Token
import kotlin.test.Test
import kotlin.test.assertTrue

class LexerTest {
    @Test
    fun a() {
        println('('.isLetterOrDigit())
    }


    @Test
    fun test1() {
        val input = """100
99.999
1
3.13
"こんにちは"
"Hello"
表示する(x)
f(x)
kosu ← 3
tensu = 100
Tokuten[4] ← 100
Tokuten のすべての要素に 0 を代入する
Tokuten ← {87, 45, 72, 100}
kosu_gokei ← kosu，tokuten ← kosu×(kosu＋1)
kosu を 1 増やす
x ← 【外部からの入力】
1+2
7-2
3*4
8/2
7÷2
5%2
x=2
x≠2
x>2
x<2
x≧2
x≦2
kosu ≧12かつ kosu≦27
kosu ＞75でない
kosu ＞12かつkosu＜27でない
    """
        val lexer: ILexer = Lexer(input)
        var token = lexer.nextToken()
        while (token.isRight() && token.getOrNull() != Token.EOF) {
            println(token)
            token = lexer.nextToken()
        }
        assertTrue(token.isRight(), token.leftOrNull()?.message)
    }

    fun test2() {
        val input = """
もしx＜3ならば
 x ←x＋1
 y ←y－1
を実行する

もしx＝3ならば
  x ←x＋1
を実行し，そうでなくもしy＞2ならば
  y ←y＋1
を実行し，そうでなければ
  y ←y－1
を実行する

x＜10の間，
  gokei ← gokei＋x
  x ←x＋1
を繰り返す

繰り返し，
 gokei ← gokei＋x
 x ←x＋1
を，x≧10になるまで実行する

xを1から10まで1ずつ増やしながら，
  gokei ← gokei＋x
を繰り返す

xを10から1まで1ずつ減らしながら，
  gokei ← gokei＋x
を繰り返す
        """
    }
}