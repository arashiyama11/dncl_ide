package io.github.arashiyama11.dncl_ide.ui.components

import androidx.compose.foundation.layout.height
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp


@Composable
fun SuggestionListView(
    textSuggestions: List<String>,
    modifier: Modifier = Modifier,
    onConfirmTextSuggestion: (String) -> Unit
) {
    LazyRow(modifier.height(48.dp)) {
        items(textSuggestions) {
            OutlinedButton(
                onClick = { onConfirmTextSuggestion(it) },
                modifier = Modifier.height(48.dp),
                colors = ButtonDefaults.outlinedButtonColors()
                    .copy(containerColor = MaterialTheme.colorScheme.outlineVariant)
            ) {
                Text(it, color = MaterialTheme.colorScheme.onBackground)
            }
        }
    }
}