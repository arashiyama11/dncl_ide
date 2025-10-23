package io.github.arashiyama11.dncl_ide.ui.components

import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.gestures.waitForUpOrCancellation
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.input.pointer.consumeDownChange
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.composed
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import io.github.arashiyama11.dncl_ide.domain.model.Definition
import kotlin.math.min
import kotlin.math.roundToInt

data class InlineSuggestionPopupStyle(
    val minWidth: Dp = 150.dp,
    val maxWidth: Dp = 250.dp,
    val preferredContentHeight: Dp = 240.dp,
    val anchorMargin: Dp = 8.dp,
    val tonalElevation: Dp = 6.dp,
    val shadowElevation: Dp = 12.dp,
    val emptyStateMessage: String = "No Suggestions",
    val itemHorizontalPadding: Dp = 12.dp,
    val itemVerticalPadding: Dp = 8.dp,
    val dividerHorizontalPadding: Dp = 12.dp
)

@Composable
fun InlineSuggestionPopup(
    suggestions: List<Definition>,
    cursorAnchor: Offset?,
    lineHeightPx: Float?,
    modifier: Modifier = Modifier,
    onConfirmTextSuggestion: (String) -> Unit,
    style: InlineSuggestionPopupStyle = InlineSuggestionPopupStyle(),
    onRequestEditorFocus: () -> Unit = {}
) {
    val anchor = cursorAnchor
    val lineHeight = lineHeightPx

    if (anchor == null || lineHeight == null) {
        return
    }

    BoxWithConstraints(modifier = modifier.fillMaxSize(), contentAlignment = Alignment.TopStart) {
        val density = LocalDensity.current
        val containerWidthPx = with(density) { maxWidth.toPx() }
        var popupWidthPx by remember { mutableStateOf<Float?>(null) }
        var popupHeightPx by remember { mutableStateOf<Float?>(null) }

        val tentativeX = anchor.x
        val widthForClamp = popupWidthPx ?: with(density) { style.minWidth.toPx() }
        val maxX = (containerWidthPx - widthForClamp).coerceAtLeast(0f)
        val clampedX = tentativeX.coerceIn(0f, maxX)
        val marginDp = style.anchorMargin
        val marginPx = with(density) { marginDp.toPx() }
        val containerHeightPx = with(density) { maxHeight.toPx() }
        val anchorBottom = anchor.y + lineHeight * 2
        val spaceBelowPx = (containerHeightPx - (anchorBottom + marginPx)).coerceAtLeast(0f)
        val spaceAbovePx = (anchor.y - marginPx).coerceAtLeast(0f)
        val desiredHeightPx = popupHeightPx ?: with(density) { style.preferredContentHeight.toPx() }
        val placeBelow = when {
            spaceBelowPx >= desiredHeightPx -> true
            spaceAbovePx >= desiredHeightPx -> false
            else -> spaceBelowPx >= spaceAbovePx
        }
        val availableSpacePx = if (placeBelow) spaceBelowPx else spaceAbovePx
        val constrainedHeightPx = when {
            availableSpacePx > 0f -> min(desiredHeightPx, availableSpacePx)
            popupHeightPx != null -> popupHeightPx!!
            else -> desiredHeightPx.coerceAtMost(containerHeightPx)
        }
        val adjustedHeightPx = constrainedHeightPx.coerceAtLeast(0f)
        val rawY = if (placeBelow) {
            anchorBottom + marginPx
        } else {
            anchor.y - marginPx - adjustedHeightPx
        }
        val clampedY = rawY
            .coerceAtLeast(0f)
            .coerceAtMost((containerHeightPx - adjustedHeightPx).coerceAtLeast(0f))
        val maxHeightDp: Dp = minOf(
            with(density) { adjustedHeightPx.toDp() },
            style.preferredContentHeight
        )
        val popupOffset = IntOffset(
            x = clampedX.roundToInt(),
            y = clampedY.roundToInt()
        )

        Surface(
            tonalElevation = style.tonalElevation,
            shadowElevation = style.shadowElevation,
            shape = MaterialTheme.shapes.medium,
            color = MaterialTheme.colorScheme.surface,
            modifier = Modifier
                .offset { popupOffset }
                .widthIn(min = style.minWidth, max = style.maxWidth)
                .onGloballyPositioned {
                    popupWidthPx = it.size.width.toFloat()
                    popupHeightPx = it.size.height.toFloat()
                }
        ) {
            if (suggestions.isEmpty()) {
                Text(
                    text = style.emptyStateMessage,
                    modifier = Modifier.padding(
                        horizontal = style.itemHorizontalPadding,
                        vertical = style.itemVerticalPadding
                    ),
                    style = MaterialTheme.typography.bodyMedium
                )
            } else {
                LazyColumn(
                    modifier = Modifier
                        .heightIn(max = maxHeightDp)
                ) {
                    itemsIndexed(suggestions) { index, definition ->
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .inlineSuggestionClickHandler(
                                    onRequestEditorFocus = onRequestEditorFocus,
                                    onConfirm = { onConfirmTextSuggestion(definition.insertText) }
                                )
                                .padding(
                                    horizontal = style.itemHorizontalPadding,
                                    vertical = style.itemVerticalPadding
                                )
                        ) {
                            Text(
                                text = definition.literal,
                                style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.SemiBold),
                                color = MaterialTheme.colorScheme.onSurface,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                            definition.displayDetail()?.let { detail ->
                                Text(
                                    text = detail,
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                            }
                        }

                        if (index != suggestions.lastIndex) {
                            HorizontalDivider(
                                modifier = Modifier.padding(horizontal = style.dividerHorizontalPadding),
                                color = MaterialTheme.colorScheme.outline
                            )
                        }
                    }
                }
            }
        }
    }
}

private fun Definition.displayDetail(): String? =
    detail?.takeIf { it.isNotBlank() && it != literal }

private fun Modifier.inlineSuggestionClickHandler(
    onRequestEditorFocus: () -> Unit,
    onConfirm: () -> Unit
): Modifier = composed {
    val currentOnRequestFocus by rememberUpdatedState(onRequestEditorFocus)
    val currentOnConfirm by rememberUpdatedState(onConfirm)
    pointerInput(Unit) {
        @Suppress("DEPRECATION")
        awaitEachGesture {
            val down = awaitFirstDown(requireUnconsumed = false)
            down.consumeDownChange()
            currentOnConfirm()
            currentOnRequestFocus()
            waitForUpOrCancellation()
        }
    }
}
