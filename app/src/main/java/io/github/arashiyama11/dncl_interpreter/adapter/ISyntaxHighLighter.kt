package io.github.arashiyama11.dncl_interpreter.adapter

import androidx.compose.ui.text.AnnotatedString
import io.github.arashiyama11.dncl.model.DnclError

interface ISyntaxHighLighter {
    operator fun invoke(
        text: String,
        isDarkTheme: Boolean,
        errorRange: IntRange?
    ): Pair<AnnotatedString, DnclError?>
}