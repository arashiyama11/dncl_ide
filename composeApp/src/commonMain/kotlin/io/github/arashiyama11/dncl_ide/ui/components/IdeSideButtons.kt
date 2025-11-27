package io.github.arashiyama11.dncl_ide.ui.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.BugReport
import androidx.compose.material.icons.outlined.Close
import androidx.compose.material.icons.outlined.FileDownload
import androidx.compose.material.icons.outlined.FileUpload
import androidx.compose.material.icons.outlined.PlayArrow
import androidx.compose.material.icons.outlined.Info
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import io.github.arashiyama11.dncl_ide.adapter.IdeViewModel
import io.github.arashiyama11.dncl_ide.adapter.TextFieldType
import io.github.arashiyama11.dncl_ide.domain.model.DebugRunningMode
import io.github.arashiyama11.dncl_ide.ui.components.TapOrRepeatButton

@Composable
fun IdeViewModel.IdeSideButtons(
    modifier: Modifier = Modifier
) {
    val uiState by uiState.collectAsState()
    val fontSize5 = 11.sp
    Row(modifier = modifier.fillMaxHeight()) {
        if (uiState.debugMode && uiState.debugRunningMode != DebugRunningMode.NON_BLOCKING) {
            Column(
                modifier = Modifier
                    .fillMaxHeight()
                    .padding(top = 8.dp, end = 8.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                TapOrRepeatButton(
                    modifier = Modifier
                        .width(104.dp)
                        .height(40.dp),
                    onClick = { onStepButtonClicked() },
                    onRepeat = { onStepButtonClicked() },
                ) {
                    Text(
                        "Next",
                        color = MaterialTheme.colorScheme.onBackground,
                        fontWeight = FontWeight.ExtraBold,
                        fontSize = fontSize5
                    )
                }
                TapOrRepeatButton(
                    onClick = {
                        onLineButtonClicked()
                    },
                    onRepeat = {
                        onLineButtonClicked()
                    },
                    modifier = Modifier
                        .width(104.dp)
                        .height(40.dp)
                ) {
                    Text(
                        "Next Line",
                        color = MaterialTheme.colorScheme.onBackground,
                        fontWeight = FontWeight.ExtraBold,
                        fontSize = fontSize5
                    )
                }
            }
        }

        Column(
            modifier = Modifier
                .fillMaxHeight()
                .width(56.dp)
                .padding(end = 8.dp)
                .alpha(0.7f)
                .verticalScroll(rememberScrollState()),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            IconButton(
                onClick = ::onRunButtonClicked,
                modifier = Modifier.fillMaxWidth(),
            ) {
                Icon(
                    Icons.Outlined.PlayArrow,
                    contentDescription = "Run",
                    tint = MaterialTheme.colorScheme.onBackground
                )
            }

            IconButton(
                onClick = { onCancelButtonClicked() },
                modifier = Modifier.fillMaxWidth(),
                enabled = uiState.running // Disable Stop when not running
            ) {
                Icon(
                    Icons.Outlined.Close,
                    contentDescription = "Stop",
                    tint = MaterialTheme.colorScheme.onBackground
                )
            }

            IconButton(
                onClick = { onChangeIOButtonClicked() },
                modifier = Modifier.fillMaxWidth()
            ) {
                Icon(
                    Icons.Outlined.BugReport,
                    contentDescription = "Change IO",
                    tint = MaterialTheme.colorScheme.onBackground
                )
            }

            IconButton(
                onClick = { toggleHoverHintMode() },
                modifier = Modifier.fillMaxWidth()
            ) {
                Icon(
                    imageVector = Icons.Outlined.Info,
                    contentDescription = "Hoverヒント切替",
                    tint = MaterialTheme.colorScheme.onBackground
                )
            }

        }
    }
}
