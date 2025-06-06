package io.github.arashiyama11.dncl_ide.ui

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.wrapContentHeight
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Settings
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationDrawerItem
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import io.github.arashiyama11.dncl_ide.adapter.SettingsScreenViewModel
import io.github.arashiyama11.dncl_ide.domain.model.DebugRunningMode
import org.koin.compose.viewmodel.koinViewModel


@Composable
fun SettingsScreen(
    viewModel: SettingsScreenViewModel = koinViewModel(),
    navigateToLicenses: () -> Unit
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()

    var isShowSettings by remember { mutableStateOf(true) }

    NavigationDrawerItem(label = {
        Text(text = "Settings")
    }, selected = false, icon = {
        Icon(Icons.Outlined.Settings, contentDescription = null)
    }, onClick = {
        isShowSettings = !isShowSettings
    })

    Column(
        modifier = Modifier
            .padding(start = 16.dp)
            .wrapContentHeight()
    ) {
        AnimatedVisibility(isShowSettings) {
            Column {
                NavigationDrawerItem(label = {
                    Text(
                        text = "配列の最初の要素の添字を1にする",
                        style = MaterialTheme.typography.bodyMedium
                    )
                }, selected = false, onClick = {}, badge = {
                    Switch(
                        checked = uiState.list1IndexSwitchEnabled,
                        onCheckedChange = {
                            //drawerViewModel.onList1IndexSwitchClicked(it)
                        },
                    )
                })

                var fontSizeText by remember { mutableStateOf(uiState.fontSize.toString()) }

                NavigationDrawerItem(
                    label = {
                        Text(
                            "フォントサイズ",
                            style = MaterialTheme.typography.bodyMedium
                        )
                    }, selected = false, onClick = {}, badge = {
                        OutlinedTextField(
                            value = fontSizeText,
                            onValueChange = {
                                fontSizeText = it
                                it.toIntOrNull()?.let {
                                    // drawerViewModel.onFontSizeChanged(it)
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

                var onEvalDelayText by remember { mutableStateOf(uiState.onEvalDelay.toString()) }

                NavigationDrawerItem(label = {
                    Text(
                        text = "デバッグモード（実行中の行を表示）",
                        style = MaterialTheme.typography.bodyMedium
                    )
                }, selected = false, onClick = {}, badge = {
                    Switch(
                        checked = uiState.debugModeEnabled,
                        onCheckedChange = {
                            //drawerViewModel.onDebugModeChanged(it)
                        }
                    )
                })

                AnimatedVisibility(
                    uiState.debugModeEnabled,
                    modifier = Modifier.padding(start = 16.dp)
                ) {
                    Column {


                        NavigationDrawerItem(
                            selected = false,
                            onClick = {},
                            label = {
                                Text("ボタンを押して実行")
                            },
                            badge = {
                                Switch(
                                    checked = uiState.debugRunningMode == DebugRunningMode.BUTTON,
                                    onCheckedChange = {
                                        //drawerViewModel.onDebugRunByButtonClicked()
                                    },
                                )
                            }
                        )

                        NavigationDrawerItem(
                            selected = false,
                            onClick = {},
                            label = {
                                Text("自動実行")
                            }, badge = {
                                Switch(
                                    checked = uiState.debugRunningMode == DebugRunningMode.NON_BLOCKING,
                                    onCheckedChange = {
                                        // drawerViewModel.onDebugRunNonBlockingClicked()
                                    },
                                )
                            }
                        )

                        AnimatedVisibility(uiState.debugRunningMode == DebugRunningMode.NON_BLOCKING) {

                            NavigationDrawerItem(

                                label = {
                                    Text(
                                        "ゆっくり実行",
                                        style = MaterialTheme.typography.bodyMedium
                                    )
                                }, selected = false, onClick = {}, badge = {
                                    OutlinedTextField(
                                        value = onEvalDelayText,
                                        onValueChange = {
                                            onEvalDelayText = it
                                            it.toIntOrNull()
                                                ?.let {
                                                    // drawerViewModel.onOnEvalDelayChanged(it)
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

                NavigationDrawerItem(
                    label = {
                        Text(
                            text = "ライセンス表示",
                            style = MaterialTheme.typography.bodyMedium
                        )
                    }, selected = false, onClick = navigateToLicenses
                )
            }
        }
    }
}