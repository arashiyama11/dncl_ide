package io.github.arashiyama11.dncl_ide.ui.components

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.FilterChip
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Tab
import androidx.compose.material3.TabRow
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.FilterQuality
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import io.github.arashiyama11.dncl_ide.adapter.CanvasSurfaceState
import io.github.arashiyama11.dncl_ide.adapter.IdeUiState
import io.github.arashiyama11.dncl_ide.adapter.OutputPane
import io.github.arashiyama11.dncl_ide.ui.LocalCodeTypography

@Composable
fun CanvasAwareOutputField(
    uiState: IdeUiState,
    onSelectPane: (OutputPane) -> Unit,
    onSelectCanvas: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    val availablePanes = buildList {
        add(OutputPane.STDOUT)
        if (uiState.dnclError != null) add(OutputPane.ERROR)
        if (uiState.canvasSurfaces.isNotEmpty()) add(OutputPane.CANVAS)
    }
    val selectedPane = uiState.outputPane.takeIf { it in availablePanes } ?: OutputPane.STDOUT

    Column(modifier = modifier.fillMaxHeight()) {
        if (availablePanes.size > 1) {
            CanvasOutputTabs(
                panes = availablePanes,
                selectedPane = selectedPane,
                onSelectPane = onSelectPane
            )
            Spacer(Modifier.height(8.dp))
        }
        when (selectedPane) {
            OutputPane.STDOUT -> {
                val textFieldDesc = "出力"
                OutlinedTextField(
                    value = uiState.output,
                    onValueChange = {},
                    modifier = Modifier.weight(1f).fillMaxWidth(),
                    textStyle = LocalCodeTypography.current.bodyLarge,
                    label = { Text(textFieldDesc) },
                    readOnly = true,
                )
            }

            OutputPane.ERROR -> {
                val textFieldDesc = "エラー"
                val errorText = uiState.errorOutput
                OutlinedTextField(
                    value = errorText,
                    onValueChange = {},
                    modifier = Modifier.weight(1f).fillMaxWidth(),
                    textStyle = LocalCodeTypography.current.bodyLarge,
                    label = { Text(textFieldDesc) },
                    readOnly = true,
                )
            }

            OutputPane.CANVAS -> {
                CanvasPanel(
                    surfaces = uiState.canvasSurfaces,
                    selectedPath = uiState.selectedCanvasPath,
                    onSelectCanvas = onSelectCanvas,
                    modifier = Modifier.weight(1f).fillMaxWidth()
                        .verticalScroll(rememberScrollState())
                )
            }
        }
    }
}

@Composable
private fun CanvasOutputTabs(
    panes: List<OutputPane>,
    selectedPane: OutputPane,
    onSelectPane: (OutputPane) -> Unit
) {
    val selectedIndex = panes.indexOf(selectedPane).coerceAtLeast(0)
    TabRow(selectedTabIndex = selectedIndex, containerColor = Color.Transparent) {
        panes.forEach { pane ->
            Tab(
                selected = pane == selectedPane,
                onClick = { onSelectPane(pane) },
                text = { Text(pane.label()) }
            )
        }
    }
}

private fun OutputPane.label(): String = when (this) {
    OutputPane.STDOUT -> "出力"
    OutputPane.CANVAS -> "キャンバス"
    OutputPane.ERROR -> "エラー"
}

@Composable
private fun CanvasPanel(
    surfaces: List<CanvasSurfaceState>,
    selectedPath: String?,
    onSelectCanvas: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    if (surfaces.isEmpty()) {
        Box(modifier = modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            Text("キャンバス出力はまだありません", textAlign = TextAlign.Center)
        }
        return
    }

    val currentSurface = surfaces.firstOrNull { it.path == selectedPath } ?: surfaces.first()


    Column(modifier = modifier.fillMaxSize(), verticalArrangement = Arrangement.spacedBy(12.dp)) {
        LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            items(surfaces, key = { it.path }) { surface ->
                FilterChip(
                    selected = surface.path == currentSurface.path,
                    onClick = { onSelectCanvas(surface.path) },
                    label = { Text(surface.path) }
                )
            }
        }

        Box(
            modifier = Modifier
                //.weight(1f)
                .fillMaxWidth()
                .background(MaterialTheme.colorScheme.surfaceVariant),
            contentAlignment = Alignment.Center
        ) {
            val bitmap = currentSurface.bitmap
            if (bitmap != null) {
                Image(
                    bitmap = bitmap,
                    contentDescription = currentSurface.path,
                    modifier = Modifier.fillMaxSize().padding(8.dp).border(3.dp, Color.Red),
                    contentScale = ContentScale.FillWidth,
                    filterQuality = FilterQuality.None
                )
            } else {
                Text(
                    text = "このプラットフォームではキャンバス画像の描画に未対応です",
                    modifier = Modifier.padding(16.dp),
                    textAlign = TextAlign.Center,
                    style = MaterialTheme.typography.bodyMedium
                )
            }
        }

        CanvasMetadata(surface = currentSurface)
    }
}

@Composable
private fun CanvasMetadata(surface: CanvasSurfaceState) {
    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
        Text("パス: ${surface.path}", style = MaterialTheme.typography.bodySmall)
        Text(
            "サイズ: ${surface.width}×${surface.height} / フレーム#${surface.frameNumber}",
            style = MaterialTheme.typography.bodySmall
        )
        Text(
            "色空間: ${colorSpaceLabel(surface.colorSpace)} / 背景色: ${backgroundColorHex(surface.backgroundColor)}",
            style = MaterialTheme.typography.bodySmall
        )
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(24.dp)
                .background(backgroundColor(surface.backgroundColor))
        )
    }
}

private fun colorSpaceLabel(colorSpace: Int): String = when (colorSpace) {
    0 -> "RGBA8888"
    1 -> "RGB565"
    2 -> "CommandStream"
    else -> "Unknown($colorSpace)"
}

private fun backgroundColorHex(color: Int): String {
    val hex = color.toUInt().toString(16).padStart(8, '0')
    return "#${hex.uppercase()}"
}

private fun backgroundColor(value: Int): Color {
    val rgba = value.toUInt()
    val r = ((rgba shr 24) and 0xFFu).toInt()
    val g = ((rgba shr 16) and 0xFFu).toInt()
    val b = ((rgba shr 8) and 0xFFu).toInt()
    val a = (rgba and 0xFFu).toInt()
    return Color(r / 255f, g / 255f, b / 255f, a / 255f)
}
