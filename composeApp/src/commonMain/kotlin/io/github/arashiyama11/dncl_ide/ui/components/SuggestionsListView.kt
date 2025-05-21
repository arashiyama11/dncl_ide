package io.github.arashiyama11.dncl_ide.ui.components

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.VerticalDivider
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import io.github.arashiyama11.dncl_ide.util.TextSuggestions

private val TextSuggestions.Definition.insertText
    get() = literal + if (isFunction) "()" else ""

@Composable
fun SuggestionListView(
    textSuggestions: List<TextSuggestions.Definition>,
    modifier: Modifier = Modifier,
    onConfirmTextSuggestion: (String) -> Unit
) {
    LazyRow(modifier.height(48.dp)) {
        itemsIndexed(textSuggestions) { index, def ->
            Box(
                Modifier.widthIn(min = 32.dp).fillMaxHeight()
                    .clickable { onConfirmTextSuggestion(def.insertText) },
                contentAlignment = Alignment.Center
            ) {
                Text(
                    def.literal, color = MaterialTheme.colorScheme.onBackground,
                    modifier = Modifier.padding(horizontal = 8.dp), textAlign = TextAlign.Center
                )
            }

            if (index != textSuggestions.lastIndex) {
                VerticalDivider(modifier = Modifier.padding(vertical = 12.dp))
            }
        }
    }
}