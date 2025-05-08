package io.github.arashiyama11.dncl_ide.ui.components

import androidx.compose.foundation.border
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.indication
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.PressInteraction
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.padding
import androidx.compose.material.ButtonDefaults
import androidx.compose.material.MaterialTheme
import androidx.compose.material.ripple
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.cancelAndJoin
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch

@Composable
fun TapOrRepeatButton(
    modifier: Modifier = Modifier,
    onClick: () -> Unit,
    onRepeat: () -> Unit,
    intervalMillis: Long = 10L,
    longPressThresholdMillis: Long = 500L,
    content: @Composable BoxScope.() -> Unit
) {
    val interactionSource = remember { MutableInteractionSource() }

    Box(
        modifier = modifier
            .indication(
                interactionSource,
                ripple(bounded = true)
            )
            .pointerInput(Unit) {
                detectTapGestures(
                    onPress = { offset ->
                        coroutineScope {
                            val press = PressInteraction.Press(offset)
                            interactionSource.emit(press)

                            var longPress = false
                            val repeatJob = launch(Dispatchers.Default) {
                                delay(longPressThresholdMillis)
                                longPress = true
                                while (isActive) {
                                    onRepeat()
                                    delay(intervalMillis)
                                }
                            }

                            val released = tryAwaitRelease()

                            if (released) {
                                interactionSource.emit(PressInteraction.Release(press))
                            } else {
                                interactionSource.emit(PressInteraction.Cancel(press))
                            }

                            repeatJob.cancelAndJoin()

                            if (!longPress && released) {
                                onClick()
                            }
                        }
                    }
                )
            }
            .clip(MaterialTheme.shapes.small)
            .border(ButtonDefaults.outlinedBorder, MaterialTheme.shapes.small)
            .padding(horizontal = 16.dp, vertical = 8.dp),
        contentAlignment = Alignment.Center
    ) {
        content()
    }
}
