package io.github.arashiyama11.dncl_ide.ui.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.Backspace
import androidx.compose.material.icons.automirrored.outlined.KeyboardBackspace
import androidx.compose.material.icons.automirrored.outlined.KeyboardReturn
import androidx.compose.material3.AssistChip
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ElevatedCard
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import io.github.arashiyama11.dncl_ide.adapter.CustomImeSnippet
import org.jetbrains.compose.ui.tooling.preview.Preview
import kotlin.random.Random

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun CustomImePanel(
    snippets: List<CustomImeSnippet>,
    quickKeys: List<String>,
    onQuickKeyClick: (String) -> Unit,
    onSnippetClick: (CustomImeSnippet) -> Unit,
    onInsertNewLine: () -> Unit,
    onDeleteBackward: () -> Unit,
    modifier: Modifier = Modifier
) {
    Surface(
        tonalElevation = 6.dp,
        shadowElevation = 4.dp,
        modifier = modifier.fillMaxWidth()
    ) {
        Column(
            modifier = Modifier
                .navigationBarsPadding()
                .padding(horizontal = 16.dp, vertical = 12.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                if (quickKeys.isNotEmpty()) {
                    FlowRow(
                        modifier = Modifier.weight(1f, fill = true),
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        quickKeys.forEach { key ->
                            AssistChip(
                                onClick = { onQuickKeyClick(key) },
                                label = { Text(key) }
                            )
                        }
                    }
                } else {
                    Spacer(modifier = Modifier.weight(1f))
                }
                Column(
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    FilledTonalButton(onClick = onDeleteBackward) {
                        Icon(
                            Icons.AutoMirrored.Outlined.Backspace,
                            contentDescription = "削除"
                        )
                    }
                    FilledTonalButton(onClick = onInsertNewLine) {
                        Icon(
                            Icons.AutoMirrored.Outlined.KeyboardReturn,
                            contentDescription = "改行"
                        )
                    }
                }
            }

            if (snippets.isNotEmpty()) {
                LazyRow(
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    items(snippets, key = { it.id }) { snippet ->
                        ElevatedCard(
                            onClick = { onSnippetClick(snippet) },
                            colors = CardDefaults.elevatedCardColors(),
                            modifier = Modifier
                                .padding(bottom = 4.dp)
                        ) {
                            Column(
                                modifier = Modifier.padding(horizontal = 16.dp, vertical = 12.dp),
                                verticalArrangement = Arrangement.spacedBy(4.dp)
                            ) {
                                Text(
                                    text = snippet.title,
                                    style = MaterialTheme.typography.titleMedium
                                )
                                Text(
                                    text = snippet.description,
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                    }
                }
            } else if (quickKeys.isEmpty()) {
                Text(
                    text = "専用IMEパネルに表示する項目がありません",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}


@Preview
@Composable
private fun CustomImePanelPreview() {
    val snippets = List(3) { index ->
        CustomImeSnippet(
            id = "demo-$index",
            title = "スニペット${index + 1}",
            body = "",
            description = "プレビュー用ダミー ${Random.nextInt(0, 100)}"
        )
    }
    CustomImePanel(
        snippets = snippets,
        quickKeys = listOf("(", ")", "==", "=>"),
        onQuickKeyClick = {},
        onSnippetClick = {},
        onInsertNewLine = {},
        onDeleteBackward = {}
    )
}
