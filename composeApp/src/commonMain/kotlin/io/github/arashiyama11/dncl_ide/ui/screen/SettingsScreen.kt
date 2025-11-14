package io.github.arashiyama11.dncl_ide.ui.screen

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.wrapContentHeight
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Settings
import androidx.compose.material3.Card
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import io.github.arashiyama11.dncl_ide.adapter.SettingsScreenViewModel
import io.github.arashiyama11.dncl_ide.domain.model.DebugRunningMode
import io.github.arashiyama11.dncl_ide.domain.model.SuggestionPanelStyle
import dncl_ide.composeApp.BuildConfig
import org.koin.compose.viewmodel.koinViewModel

@Composable
fun SettingsScreen(
    viewModel: SettingsScreenViewModel = koinViewModel(),
    navigateToLicenses: () -> Unit
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()


    Column(
        modifier = Modifier.padding(16.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Outlined.Settings, contentDescription = null)
                Text(
                    text = "Settings",
                    style = MaterialTheme.typography.titleLarge,
                    modifier = Modifier.padding(start = 8.dp)
                )
            }

        }

        Spacer(modifier = Modifier.height(12.dp))


        Column(
            modifier = Modifier.wrapContentHeight()
        ) {
            SettingsItem(
                title = "配列の最初の要素の添字を1にする",
                control = {
                    Switch(
                        checked = uiState.list1IndexSwitchEnabled,
                        onCheckedChange = { viewModel.onList1IndexSwitchClicked(it) }
                    )
                }
            )

            var fontSizeText by remember { mutableStateOf(uiState.fontSize.toString()) }

            SettingsItem(
                title = "フォントサイズ",
                control = {
                    OutlinedTextField(
                        value = fontSizeText,
                        onValueChange = {
                            fontSizeText = it
                            it.toIntOrNull()?.let { size ->
                                viewModel.onFontSizeChanged(size)
                            }
                        },
                        modifier = Modifier
                            .width(80.dp)
                            .padding(vertical = 8.dp),
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        singleLine = true,
                    )
                }
            )

            SettingsItem(
                title = "補完候補の表示方式",
                control = {
                    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                        SuggestionPanelStyleSelector(
                            label = "画面下部に横並び",
                            selected = uiState.suggestionPanelStyle == SuggestionPanelStyle.BOTTOM_STRIP,
                            onClick = {
                                viewModel.onSuggestionPanelStyleChanged(SuggestionPanelStyle.BOTTOM_STRIP)
                            }
                        )
                        SuggestionPanelStyleSelector(
                            label = "カーソル直下にポップアップ",
                            selected = uiState.suggestionPanelStyle == SuggestionPanelStyle.INLINE_DROPDOWN,
                            onClick = {
                                viewModel.onSuggestionPanelStyleChanged(SuggestionPanelStyle.INLINE_DROPDOWN)
                            }
                        )
                    }
                }
            )

            SettingsItem(
                title = "デバッグモード（実行中の行を表示）",
                control = {
                    Switch(
                        checked = uiState.debugModeEnabled,
                        onCheckedChange = { viewModel.onDebugModeChanged(it) }
                    )
                }
            )

            AnimatedVisibility(uiState.debugModeEnabled) {
                Column(modifier = Modifier.padding(start = 16.dp)) {
                    SettingsItem(
                        title = "ボタンを押して実行",
                        control = {
                            Switch(
                                checked = uiState.debugRunningMode == DebugRunningMode.BUTTON,
                                onCheckedChange = {
                                    if (it) viewModel.onDebugRunByButtonClicked()
                                }
                            )
                        }
                    )

                    SettingsItem(
                        title = "自動実行",
                        control = {
                            Switch(
                                checked = uiState.debugRunningMode == DebugRunningMode.NON_BLOCKING,
                                onCheckedChange = {
                                    if (it) viewModel.onDebugRunNonBlockingClicked()
                                }
                            )
                        }
                    )

                    AnimatedVisibility(uiState.debugRunningMode == DebugRunningMode.NON_BLOCKING) {
                        var onEvalDelayText by remember { mutableStateOf(uiState.onEvalDelay.toString()) }

                        SettingsItem(
                            title = "ゆっくり実行",
                            control = {
                                OutlinedTextField(
                                    value = onEvalDelayText,
                                    onValueChange = {
                                        onEvalDelayText = it
                                        it.toIntOrNull()?.let { delay ->
                                            viewModel.onOnEvalDelayChanged(delay)
                                        }
                                    },
                                    modifier = Modifier
                                        .width(80.dp)
                                        .padding(vertical = 8.dp),
                                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                                    singleLine = true,
                                )
                            }
                        )
                    }
                }
            }

            // License button
            SettingsItem(
                title = "アプリバージョン",
                control = {
                    Text(text = BuildConfig.APP_VERSION)
                }
            )

            SettingsItem(
                title = "DNCLバージョン",
                control = {
                    Text(text = BuildConfig.DNCL_VERSION)
                }
            )

            TextButton(
                onClick = navigateToLicenses,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 16.dp)
            ) {
                Text("ライセンス表示")
            }

            val uriHandler = LocalUriHandler.current
            TextButton(
                onClick = {
                    uriHandler.openUri("https://github.com/arashiyama11/dncl_ide/issues")
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 16.dp)
            ) {
                Text("改善報告・バグ報告")
            }
        }
    }
}

@Composable
private fun SettingsItem(
    title: String,
    control: @Composable () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = title,
                style = MaterialTheme.typography.bodyMedium
            )
            control()
        }
    }
}

@Composable
private fun SuggestionPanelStyleSelector(
    label: String,
    selected: Boolean,
    onClick: () -> Unit
) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        RadioButton(
            selected = selected,
            onClick = onClick
        )
        Text(
            text = label,
            style = MaterialTheme.typography.bodyMedium
        )
    }
}
