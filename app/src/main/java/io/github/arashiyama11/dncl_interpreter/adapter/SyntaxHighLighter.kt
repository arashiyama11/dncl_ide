package io.github.arashiyama11.dncl_interpreter.adapter

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.text.withStyle
import io.github.arashiyama11.dncl.lexer.Lexer
import io.github.arashiyama11.dncl.model.BuiltInFunction
import io.github.arashiyama11.dncl.model.DnclError
import io.github.arashiyama11.dncl.model.InternalError
import io.github.arashiyama11.dncl.model.Token
import io.github.arashiyama11.dncl.parser.Parser
import org.koin.core.annotation.Single

@Single(binds = [ISyntaxHighLighter::class])
class SyntaxHighLighter : ISyntaxHighLighter {
    override operator fun invoke(
        text: String,
        isDarkTheme: Boolean,
        errorRange: IntRange?
    ): Pair<AnnotatedString, DnclError?> {
        if (text.isEmpty()) return AnnotatedString(text) to null
        if (errorRange != null) return annotateByLex(text, isDarkTheme, errorRange)
        return if (Lexer(text).all { it.isRight() }) annotateByParse(
            text,
            isDarkTheme
        ) else annotateByLex(
            text,
            isDarkTheme
        )
    }

    private fun IntRange.includes(other: IntRange): Boolean {
        return this.first <= other.first && other.last <= this.last
    }

    private fun annotateByLex(
        text: String,
        isDarkTheme: Boolean,
        errorRange: IntRange? = null,
        err: DnclError? = null
    ): Pair<AnnotatedString, DnclError?> {
        var error: DnclError? = err
        val str = buildAnnotatedString {
            for (token in Lexer(text)) {
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
                            withStyle(styles.identifierStyle) {
                                append(
                                    text.substring(
                                        t.range
                                    )
                                )
                            }
                        }

                        is Token.Japanese -> {
                            if (BuiltInFunction.Companion.from(t.literal) == null)
                                withStyle(styles.identifierStyle) {
                                    append(
                                        text.substring(
                                            t.range
                                        )
                                    )
                                }
                            else
                                withStyle(styles.buildInFunctionStyle) {
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

    private fun annotateByParse(
        text: String,
        isDarkTheme: Boolean
    ): Pair<AnnotatedString, DnclError?> {
        val parser =
            Parser.Companion(Lexer(text)).getOrNull() ?: return annotateByLex(text, isDarkTheme)
        val err = parser.parseProgram().leftOrNull() ?: return annotateByLex(text, isDarkTheme)
        err.errorRange?.let { return annotateByLex(text, isDarkTheme, it, err) }
        return annotateByLex(text, isDarkTheme)
    }


    companion object {
        private val lightStyles = Styles(
            syntaxStyle = SpanStyle(
                color = Color.Companion.Blue,
                fontWeight = FontWeight.Companion.Bold
            ),
            stringStyle = SpanStyle(color = Color.Companion.Red),
            numberStyle = SpanStyle(color = Color.Companion.Magenta),
            commentStyle = SpanStyle(color = Color.Companion.Green),
            identifierStyle = SpanStyle(color = Color.Companion.Black),
            buildInFunctionStyle = SpanStyle(color = Color.Companion.Blue),
            bracketStyle = SpanStyle(color = Color.Companion.Black),
            errorStyle = SpanStyle(color = Color.Companion.Red),
            otherStyle = SpanStyle(color = Color.Companion.Black)
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