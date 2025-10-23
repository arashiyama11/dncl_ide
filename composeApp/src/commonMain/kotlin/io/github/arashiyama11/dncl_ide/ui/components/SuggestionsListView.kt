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
import io.github.arashiyama11.dncl_ide.domain.model.Definition

private val Definition.displayInsertText: String
    get() = insertText

private val Definition.displayLabelSuffix: String
    get() = detail?.takeIf { it.isNotBlank() && it != literal }?.let { " · $it" } ?: ""

@Composable
fun SuggestionStripView(
    textSuggestions: List<Definition>,
    modifier: Modifier = Modifier,
    onConfirmTextSuggestion: (String) -> Unit
) {
    if (textSuggestions.isEmpty()) {
        Text("No Suggestions", modifier = modifier.padding(8.dp))
        return
    }
    LazyRow(modifier.height(48.dp)) {
        itemsIndexed(textSuggestions) { index, def ->
            Box(
                Modifier.widthIn(min = 32.dp).fillMaxHeight()
                    .clickable { onConfirmTextSuggestion(def.displayInsertText) },
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = buildString {
                        append(def.literal)
                        append(def.displayLabelSuffix)
                    },
                    color = MaterialTheme.colorScheme.onSurface,
                    modifier = Modifier.padding(horizontal = 8.dp), textAlign = TextAlign.Center
                )
            }

            if (index != textSuggestions.lastIndex) {
                VerticalDivider(modifier = Modifier.padding(vertical = 12.dp))
            }
        }
    }
}

@Composable
fun SuggestionListView(
    textSuggestions: List<Definition>,
    modifier: Modifier = Modifier,
    onConfirmTextSuggestion: (String) -> Unit
) = SuggestionStripView(textSuggestions, modifier, onConfirmTextSuggestion)
