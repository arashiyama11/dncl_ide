package io.github.arashiyama11.dncl_ide.ui.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.Backspace
import androidx.compose.material.icons.automirrored.outlined.KeyboardReturn
import androidx.compose.material.icons.outlined.Code
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
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import io.github.arashiyama11.dncl_ide.adapter.CustomImeController
import io.github.arashiyama11.dncl_ide.adapter.CustomImeKeyword
import io.github.arashiyama11.dncl_ide.adapter.CustomImePanelMode
import io.github.arashiyama11.dncl_ide.adapter.CustomImeSnippet
import org.jetbrains.compose.ui.tooling.preview.Preview
import kotlin.random.Random

data class CustomImePanelStyle(
    val surfaceTonalElevation: Dp = 6.dp,
    val surfaceShadowElevation: Dp = 4.dp,
    val contentPadding: PaddingValues = PaddingValues(start = 16.dp, end = 16.dp, top = 12.dp),
    val sectionSpacing: Dp = 16.dp,
    val quickKeyHorizontalSpacing: Dp = 4.dp,
    val quickKeyVerticalSpacing: Dp = 4.dp,
    val quickRowSpacing: Dp = 12.dp,
    val quickActionButtonSpacing: Dp = 8.dp,
    val snippetListSpacing: Dp = 12.dp,
    val snippetCardPadding: PaddingValues = PaddingValues(horizontal = 16.dp, vertical = 12.dp),
    val snippetCardContentSpacing: Dp = 4.dp,
    val snippetCardBottomPadding: Dp = 4.dp
)

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun CustomImePanel(
    snippets: List<CustomImeSnippet>,
    quickKeys: List<String>,
    keywords: List<CustomImeKeyword>,
    panelMode: CustomImePanelMode,
    onModeChange: (CustomImePanelMode) -> Unit,
    onQuickKeyClick: (String) -> Unit,
    onKeywordClick: (CustomImeKeyword) -> Unit,
    onSnippetClick: (CustomImeSnippet) -> Unit,
    onInsertNewLine: () -> Unit,
    onDeleteBackward: () -> Unit,
    modifier: Modifier = Modifier,
    style: CustomImePanelStyle = CustomImePanelStyle()
) {
    Surface(
        tonalElevation = style.surfaceTonalElevation,
        shadowElevation = style.surfaceShadowElevation,
        modifier = modifier.fillMaxWidth()
    ) {
        Column(
            modifier = Modifier
                .navigationBarsPadding()
                .padding(style.contentPadding),
            verticalArrangement = Arrangement.spacedBy(style.sectionSpacing)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth().weight(1f, fill = true),
                verticalAlignment = Alignment.Top,
                horizontalArrangement = Arrangement.spacedBy(style.quickRowSpacing)
            ) {
                when (panelMode) {
                    CustomImePanelMode.QUICK_KEYS -> {
                        if (quickKeys.isNotEmpty()) {
                            FlowRow(
                                modifier = Modifier
                                    .weight(1f, fill = true)
                                    .verticalScroll(rememberScrollState()),
                                horizontalArrangement = Arrangement.spacedBy(style.quickKeyHorizontalSpacing),
                                verticalArrangement = Arrangement.spacedBy(style.quickKeyVerticalSpacing)
                            ) {
                                quickKeys.forEach { key ->
                                    AssistChip(
                                        onClick = { onQuickKeyClick(key) },
                                        label = { Text(key) },
                                    )
                                }
                            }
                        } else {
                            Text(
                                text = "登録された記号がありません",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier.weight(1f, fill = true)
                            )
                        }
                    }

                    CustomImePanelMode.KEYWORDS -> {
                        if (keywords.isNotEmpty()) {
                            LazyVerticalGrid(
                                modifier = Modifier.weight(1f, fill = true),
                                columns = GridCells.Adaptive(88.dp),
                                contentPadding = PaddingValues(horizontal = 2.dp, vertical = 4.dp)
                            ) {
                                items(keywords) {
                                    AssistChip(
                                        onClick = { onKeywordClick(it) },
                                        label = {
                                            Box(
                                                contentAlignment = Alignment.Center,
                                                modifier = Modifier.fillMaxSize()
                                            ) {
                                                Text(it.label, maxLines = 2)
                                            }
                                        },
                                        modifier = Modifier.heightIn(min = 48.dp)
                                    )
                                }
                            }
                        } else {
                            Text(
                                text = "登録されたキーワードがありません",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier.weight(1f, fill = true)
                            )
                        }
                    }
                }

                Column(
                    verticalArrangement = Arrangement.spacedBy(style.quickActionButtonSpacing),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    FilledTonalButton(
                        onClick = onDeleteBackward,
                        shape = RoundedCornerShape(4.dp)
                    ) {
                        Icon(
                            Icons.AutoMirrored.Outlined.Backspace,
                            contentDescription = "削除"
                        )
                    }
                    FilledTonalButton(onClick = onInsertNewLine, shape = RoundedCornerShape(4.dp)) {
                        Icon(
                            Icons.AutoMirrored.Outlined.KeyboardReturn,
                            contentDescription = "改行"
                        )
                    }
                    FilledTonalButton(
                        onClick = {
                            val nextMode = when (panelMode) {
                                CustomImePanelMode.QUICK_KEYS -> CustomImePanelMode.KEYWORDS
                                CustomImePanelMode.KEYWORDS -> CustomImePanelMode.QUICK_KEYS
                            }
                            onModeChange(nextMode)
                        },
                        shape = RoundedCornerShape(4.dp)
                    ) {

                        when (panelMode) {
                            CustomImePanelMode.KEYWORDS -> Text("記号")
                            CustomImePanelMode.QUICK_KEYS -> Icon(Icons.Outlined.Code, null)
                        }

                    }
                }
            }

            if (snippets.isNotEmpty()) {
                LazyRow(
                    horizontalArrangement = Arrangement.spacedBy(style.snippetListSpacing)
                ) {
                    items(snippets, key = { it.id }) { snippet ->
                        ElevatedCard(
                            onClick = { onSnippetClick(snippet) },
                            colors = CardDefaults.elevatedCardColors(),
                            modifier = Modifier
                                .padding(bottom = style.snippetCardBottomPadding)
                        ) {
                            Column(
                                modifier = Modifier.padding(style.snippetCardPadding),
                                verticalArrangement = Arrangement.spacedBy(style.snippetCardContentSpacing)
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
            } else if (quickKeys.isEmpty() && keywords.isEmpty()) {
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
        quickKeys = CustomImeController.DEFAULT_QUICK_KEYS,
        keywords = CustomImeController.DEFAULT_KEYWORDS,
        panelMode = CustomImePanelMode.KEYWORDS,
        onModeChange = {},
        onQuickKeyClick = {},
        onKeywordClick = {},
        onSnippetClick = {},
        onInsertNewLine = {},
        onDeleteBackward = {}
    )
}
