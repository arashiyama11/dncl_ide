package io.github.arashiyama11.dncl_interpreter.usecase

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
import io.github.arashiyama11.dncl.model.ParserError
import io.github.arashiyama11.dncl.model.Token
import io.github.arashiyama11.dncl.parser.Parser
import org.koin.core.annotation.Single

@Single(binds = [ISyntaxHighLightUseCase::class])
class SyntaxHighLightUseCase : ISyntaxHighLightUseCase {
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
        errorRange: IntRange? = null
    ): Pair<AnnotatedString, DnclError?> {
        var error: DnclError? = null
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
                            if (BuiltInFunction.from(t.literal) == null)
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

                        is Token.EOF, is Token.Indent, is Token.NewLine -> {}
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
        val parser = Parser(Lexer(text)).getOrNull() ?: return annotateByLex(text, isDarkTheme)
        val err = parser.parseProgram().leftOrNull() ?: return annotateByLex(text, isDarkTheme)
        if (err !is ParserError) return annotateByLex(text, isDarkTheme)
        return annotateByLex(text, isDarkTheme, err.failToken.range)
    }


    companion object {
        private val lightStyles = Styles(
            syntaxStyle = SpanStyle(color = Color.Blue, fontWeight = FontWeight.Bold),
            stringStyle = SpanStyle(color = Color.Red),
            numberStyle = SpanStyle(color = Color.Magenta),
            commentStyle = SpanStyle(color = Color.Green),
            identifierStyle = SpanStyle(color = Color.Black),
            buildInFunctionStyle = SpanStyle(color = Color.Blue),
            bracketStyle = SpanStyle(color = Color.Black),
            errorStyle = SpanStyle(color = Color.Red),
            otherStyle = SpanStyle(color = Color.Black)
        )

        private val darkStyles = Styles(
            syntaxStyle = SpanStyle(color = Color(0xFF569CD6), fontWeight = FontWeight.Bold),
            stringStyle = SpanStyle(color = Color(0xFFCE9178)),
            numberStyle = SpanStyle(color = Color(0xFFB5CEA8)),
            commentStyle = SpanStyle(color = Color(0xFF6A9955)),
            identifierStyle = SpanStyle(color = Color(0xFF9CDCFE)),
            buildInFunctionStyle = SpanStyle(color = Color(0xFFDCDCAA)),
            bracketStyle = SpanStyle(color = Color(0xFFFFD700)),
            errorStyle = SpanStyle(color = Color.Red, textDecoration = TextDecoration.Underline),
            otherStyle = SpanStyle(color = Color.White)
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