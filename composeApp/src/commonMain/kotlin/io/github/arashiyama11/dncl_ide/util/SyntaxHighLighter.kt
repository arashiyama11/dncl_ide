package io.github.arashiyama11.dncl_ide.util

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.text.withStyle
import arrow.core.Either
import io.github.arashiyama11.dncl_ide.interpreter.model.AllBuiltInFunction
import io.github.arashiyama11.dncl_ide.interpreter.model.DnclError
import io.github.arashiyama11.dncl_ide.interpreter.model.InternalError
import io.github.arashiyama11.dncl_ide.interpreter.model.Token
import io.github.arashiyama11.dncl_ide.interpreter.model.LexerError

class SyntaxHighLighter {
    fun highlightWithParsedData(
        text: String,
        isDarkTheme: Boolean,
        errorRange: IntRange?,
        tokens: List<Either<LexerError, Token>>,
    ): Pair<AnnotatedString, DnclError?> {
        if (text.isEmpty()) return AnnotatedString(text) to null
        return annotateWithTokens(text, isDarkTheme, errorRange, tokens)
    }

    private fun IntRange.includes(other: IntRange): Boolean {
        return this.first <= other.first && other.last <= this.last
    }

    private fun annotateWithTokens(
        text: String,
        isDarkTheme: Boolean,
        errorRange: IntRange? = null,
        tokens: List<Either<LexerError, Token>>,
        err: DnclError? = null
    ): Pair<AnnotatedString, DnclError?> {
        var error: DnclError? = err
        val str = buildAnnotatedString {
            for ((token, next) in tokens.windowed(2)) {
                if (token.isLeft()) {
                    error = token.leftOrNull()
                    val i = token.leftOrNull()!!.index
                    if (i < text.length)
                        withStyle(if (isDarkTheme) darkStyles.errorStyle else lightStyles.errorStyle) {
                            append(text[token.leftOrNull()!!.index])
                        }
                    break
                }
                try {
                    val t = token.getOrNull()!!

                    if (length < t.range.first) {
                        append(text.substring(length, t.range.first))
                    }

                    if (errorRange != null && errorRange.includes(t.range)) {
                        if (t !is Token.NewLine && t !is Token.EOF)
                            withStyle(if (isDarkTheme) darkStyles.errorStyle else lightStyles.errorStyle) {
                                append(text.substring(t.range))
                            }
                        continue
                    }
                    val styles = if (isDarkTheme) darkStyles else lightStyles
                    when (t) {
                        is Token.If, is Token.Elif, is Token.Else, is Token.Wo, is Token.Then, is Token.Kara, is Token.Made, is Token.While, is Token.UpTo, is Token.DownTo, is Token.Function, is Token.Define -> withStyle(
                            styles.syntaxStyle
                        ) { append(text.substring(t.range)) }

                        is Token.Float, is Token.Int -> withStyle(styles.numberStyle) {
                            append(
                                text.substring(
                                    t.range
                                )
                            )
                        }

                        is Token.Identifier -> {
                            withStyle(if (next.getOrNull() is Token.ParenOpen) styles.buildInFunctionStyle else styles.identifierStyle) {
                                append(
                                    text.substring(
                                        t.range
                                    )
                                )
                            }
                        }

                        is Token.Japanese -> {
                            val style = when {
                                next.getOrNull() is Token.ParenOpen || AllBuiltInFunction.from(t.literal) != null -> styles.buildInFunctionStyle
                                else -> styles.identifierStyle
                            }
                            withStyle(style) {
                                append(
                                    text.substring(
                                        t.range
                                    )
                                )
                            }
                        }

                        is Token.String -> withStyle(styles.stringStyle) {
                            append(
                                text.substring(
                                    t.range
                                )
                            )
                        }

                        is Token.BracketOpen, is Token.BracketClose, is Token.ParenOpen, is Token.ParenClose, is Token.BraceOpen, is Token.BraceClose, is Token.LenticularOpen, is Token.LenticularClose -> {
                            withStyle(styles.bracketStyle) {
                                append(
                                    text.substring(
                                        t.range
                                    )
                                )
                            }
                        }

                        is Token.Indent -> {
                            withStyle(
                                SpanStyle(
                                    color = Color.Companion.Gray,
                                    textDecoration = TextDecoration.Companion.LineThrough
                                )
                            ) {
                                if (t.depth > 0)
                                    append(
                                        text.substring(
                                            t.range
                                        )
                                    )
                            }
                        }

                        is Token.EOF, is Token.NewLine -> {}
                        is Token.Comment -> withStyle(styles.commentStyle) {
                            append(
                                text.substring(
                                    t.range
                                )
                            )
                        }

                        else -> withStyle(styles.otherStyle) { append(text.substring(t.range)) }
                    }
                } catch (e: Throwable) {
                    error = InternalError(e.message ?: "")
                    e.printStackTrace()
                }
            }

            if (length < text.length) {
                append(text.substring(length))
            }
        }
        return str to error
    }


    companion object {
        private val lightStyles = Styles(
            syntaxStyle = SpanStyle(
                color = Color(0xFF0000FF), // 青色: キーワードなどの構文要素
                fontWeight = FontWeight.Bold
            ),
            stringStyle = SpanStyle(color = Color(0xFF008000)), // 緑色: 文字列リテラル
            numberStyle = SpanStyle(color = Color(0xFF0000FF)), // 青色: 数値リテラル
            commentStyle = SpanStyle(color = Color(0xFF808080)), // 灰色: コメント
            identifierStyle = SpanStyle(color = Color(0xFF000000)), // 黒色: 識別子
            buildInFunctionStyle = SpanStyle(color = Color(0xFF0000FF)), // 青色: 組み込み関数
            bracketStyle = SpanStyle(color = Color(0xFF000000)), // 黒色: 括弧
            errorStyle = SpanStyle(
                color = Color(0xFFFF0000), // 赤色: エラー
                textDecoration = TextDecoration.Underline
            ),
            otherStyle = SpanStyle(color = Color(0xFF000000)) // 黒色: その他
        )
        private val darkStyles = Styles(
            syntaxStyle = SpanStyle(
                color = Color(0xFF569CD6),
                fontWeight = FontWeight.Companion.Bold
            ),
            stringStyle = SpanStyle(color = Color(0xFFCE9178)),
            numberStyle = SpanStyle(color = Color(0xFFB5CEA8)),
            commentStyle = SpanStyle(color = Color(0xFF6A9955)),
            identifierStyle = SpanStyle(color = Color(0xFF9CDCFE)),
            buildInFunctionStyle = SpanStyle(color = Color(0xFFDCDCAA)),
            bracketStyle = SpanStyle(color = Color(0xFFFFD700)),
            errorStyle = SpanStyle(
                color = Color.Companion.Red,
                textDecoration = TextDecoration.Companion.Underline
            ),
            otherStyle = SpanStyle(color = Color.Companion.White)
        )
    }

    private data class Styles(
        val syntaxStyle: SpanStyle,
        val stringStyle: SpanStyle,
        val numberStyle: SpanStyle,
        val commentStyle: SpanStyle,
        val identifierStyle: SpanStyle,
        val buildInFunctionStyle: SpanStyle,
        val bracketStyle: SpanStyle,
        val errorStyle: SpanStyle,
        val otherStyle: SpanStyle
    )
}