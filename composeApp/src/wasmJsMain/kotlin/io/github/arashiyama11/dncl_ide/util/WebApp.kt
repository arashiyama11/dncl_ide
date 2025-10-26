package io.github.arashiyama11.dncl_ide.util

import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectHorizontalDragGestures
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.pointer.PointerEventType
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.input.pointer.onPointerEvent
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.ComposeViewport
import androidx.compose.ui.platform.LocalDensity
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import io.github.arashiyama11.dncl_ide.adapter.IdeUiState
import io.github.arashiyama11.dncl_ide.adapter.IdeViewModel
import io.github.arashiyama11.dncl_ide.adapter.TextFieldType
import io.github.arashiyama11.dncl_ide.ui.DnclIdeTheme
import io.github.arashiyama11.dncl_ide.ui.LocalCodeTypography
import io.github.arashiyama11.dncl_ide.ui.components.EnvironmentDebugView
import io.github.arashiyama11.dncl_ide.ui.components.IdeSideButtons
import io.github.arashiyama11.dncl_ide.ui.layout.Editor
import io.github.arashiyama11.dncl_ide.domain.model.SuggestionPanelStyle
import kotlinx.browser.document
import kotlinx.browser.window
import kotlinx.coroutines.flow.MutableStateFlow
import org.koin.compose.koinInject
import org.koin.core.Koin
import org.w3c.dom.HTMLElement
import org.w3c.dom.events.Event
import org.w3c.dom.pointerevents.PointerEvent
import kotlin.js.unsafeCast
import kotlin.math.roundToInt

private const val DEFAULT_CONTROLLER_WIDTH_PX = 320
private const val MIN_CONTROLLER_WIDTH_PX = 240
private const val MAX_CONTROLLER_WIDTH_PX = 480

@OptIn(ExperimentalComposeUiApi::class, ExperimentalWasmJsInterop::class)
context(koin: Koin)
fun webApp() {
    val composeWrapper = document.getElementById("compose-wrapper")!! as HTMLElement
    val monacoWrapper = document.getElementById("monaco-wrapper")!! as HTMLElement
    val monacoControllerElem = document.getElementById("monaco-controller")!! as HTMLElement
    val monacoHandleElem = document.getElementById("monaco-resize-handle")!! as HTMLElement
    val monacoComposeContainer = document.createElement("div") as HTMLElement
    monacoComposeContainer.style.width = "100%"
    monacoComposeContainer.style.height = "100%"
    monacoComposeContainer.style.display = "flex"
    monacoComposeContainer.style.flexDirection = "column"
    monacoComposeContainer.style.flex = "1 1 auto"
    monacoComposeContainer.style.position = "relative"
    monacoControllerElem.appendChild(monacoComposeContainer)

    val isMonacoEditorState = MutableStateFlow(false)
    val composeControllerWidthState = MutableStateFlow(DEFAULT_CONTROLLER_WIDTH_PX)
    val monacoControllerWidthState = MutableStateFlow(DEFAULT_CONTROLLER_WIDTH_PX)
    val viewModel by koin.inject<IdeViewModel>()

    var composeCursorActive = false
    var monacoCursorActive = false
    var monacoHandleHover = false

    fun applyGlobalResizeCursor() {
        val bodyStyle = document.body?.style ?: return
        if (composeCursorActive || monacoCursorActive) {
            bodyStyle.cursor = "col-resize"
        } else {
            bodyStyle.cursor = ""
        }
    }

    composeWrapper.hidden = false
    monacoWrapper.hidden = true
    monacoControllerElem.style.width = "${monacoControllerWidthState.value}px"

    onMonacoLoaded {
        it.onDidChangeModelContent {
            if (isMonacoEditorState.value) return@onDidChangeModelContent
            viewModel.onTextChanged(TextFieldValue(monaco.getValue()))
            println("Content changed: ${monaco.getValue()}")
        }
    }

    fun clampWidth(width: Int): Int =
        width.coerceIn(MIN_CONTROLLER_WIDTH_PX, MAX_CONTROLLER_WIDTH_PX)

    fun applyMonacoWidth(width: Int) {
        val clamped = clampWidth(width)
        if (monacoControllerWidthState.value != clamped) {
            monacoControllerWidthState.value = clamped
        }
        monacoControllerElem.style.width = "${clamped}px"
    }

    var monacoDragging = false
    var monacoDragStartX = 0
    var monacoStartWidth = monacoControllerWidthState.value

    monacoHandleElem.addEventListener("pointerenter", {
        monacoHandleHover = true
        monacoCursorActive = true
        applyGlobalResizeCursor()
    })

    monacoHandleElem.addEventListener("pointerleave", {
        monacoHandleHover = false
        if (!monacoDragging) {
            monacoCursorActive = false
            applyGlobalResizeCursor()
        }
    })

    monacoHandleElem.addEventListener("pointerdown", { event ->
        val pointerEvent = event.unsafeCast<PointerEvent>()
        monacoDragging = true
        monacoHandleHover = true
        monacoCursorActive = true
        applyGlobalResizeCursor()
        monacoDragStartX = pointerEvent.clientX
        monacoStartWidth = monacoControllerWidthState.value
        pointerEvent.preventDefault()
    })

    document.addEventListener("pointermove", { event ->
        if (!monacoDragging) return@addEventListener
        val pointerEvent = event.unsafeCast<PointerEvent>()
        val delta = monacoDragStartX - pointerEvent.clientX
        applyMonacoWidth(monacoStartWidth + delta)
    })

    fun finishMonacoDrag(pointerEvent: PointerEvent) {
        if (!monacoDragging) return
        monacoDragging = false
        monacoCursorActive = monacoHandleHover
        applyGlobalResizeCursor()
    }

    document.addEventListener("pointerup", { event ->
        finishMonacoDrag(event.unsafeCast<PointerEvent>())
    })

    document.addEventListener("pointercancel", { event ->
        finishMonacoDrag(event.unsafeCast<PointerEvent>())
    })

    ComposeViewport(composeWrapper) {
        val isMonacoEditor by isMonacoEditorState.collectAsState()
        val uiState by viewModel.uiState.collectAsState()
        val controllerWidthPx by composeControllerWidthState.collectAsState()

        LaunchedEffect(isMonacoEditor) {
            if (isMonacoEditor) {
                composeWrapper.hidden = true
                monacoWrapper.hidden = false
                monacoControllerElem.style.width = "${monacoControllerWidthState.value}px"
                composeCursorActive = false
                applyGlobalResizeCursor()
                window.requestAnimationFrame {
                    monaco.layout()
                    window.dispatchEvent(Event("resize"))
                }
            } else {
                composeWrapper.hidden = false
                monacoWrapper.hidden = true
                monacoHandleHover = false
                monacoCursorActive = false
                applyGlobalResizeCursor()
                window.requestAnimationFrame {
                    window.dispatchEvent(Event("resize"))
                }
            }
        }

        DnclIdeTheme {
            LaunchedEffect(Unit) {
                viewModel.onStart(MutableStateFlow(true))
            }

            if (!isMonacoEditor) {
                val density = LocalDensity.current
                val controllerWidthDp = with(density) { controllerWidthPx.toDp() }
                val latestWidth = rememberUpdatedState(controllerWidthPx)
                var composeHandleHover by remember { mutableStateOf(false) }
                var composeHandleDragging by remember { mutableStateOf(false) }

                LaunchedEffect(composeHandleHover, composeHandleDragging) {
                    composeCursorActive = composeHandleHover || composeHandleDragging
                    applyGlobalResizeCursor()
                }

                DisposableEffect(Unit) {
                    onDispose {
                        composeCursorActive = false
                        applyGlobalResizeCursor()
                    }
                }

                Row(Modifier.fillMaxSize()) {
                    Box(
                        Modifier
                            .weight(1f)
                            .fillMaxHeight()
                    ) {
                        Editor(
                            uiState = uiState,
                            viewModel = viewModel,
                            modifier = Modifier.fillMaxSize()
                        )
                    }

                    Box(
                        Modifier
                            .fillMaxHeight()
                            .width(8.dp)
                            .background(MaterialTheme.colorScheme.surfaceVariant)
                            .onPointerEvent(PointerEventType.Enter) {
                                composeHandleHover = true
                            }
                            .onPointerEvent(PointerEventType.Exit) {
                                composeHandleHover = false
                            }
                            .pointerInput(Unit) {
                                var currentWidth = latestWidth.value.toFloat()
                                detectHorizontalDragGestures(
                                    onDragStart = {
                                        composeHandleDragging = true
                                        currentWidth = latestWidth.value.toFloat()
                                    },
                                    onHorizontalDrag = { _, dragAmount ->
                                        currentWidth -= dragAmount
                                        val clamped = clampWidth(currentWidth.roundToInt())
                                        composeControllerWidthState.value = clamped
                                        currentWidth = clamped.toFloat()
                                    },
                                    onDragEnd = {
                                        composeHandleDragging = false
                                    },
                                    onDragCancel = {
                                        composeHandleDragging = false
                                    }
                                )
                            }
                    )

                    ControllerUi(
                        isMonacoEditor,
                        { isMonacoEditorState.value = it },
                        viewModel,
                        uiState,
                        Modifier
                            .fillMaxHeight()
                            .width(controllerWidthDp)
                            .background(MaterialTheme.colorScheme.background)
                    )
                }
            }
        }
    }

    ComposeViewport(monacoComposeContainer) {
        val isMonacoEditor by isMonacoEditorState.collectAsState()
        val uiState by viewModel.uiState.collectAsState()

        LaunchedEffect(isMonacoEditor) {
            if (!isMonacoEditor) {
                monacoCursorActive = false
                applyGlobalResizeCursor()
            }
        }

        DnclIdeTheme {
            if (isMonacoEditor) {
                ControllerUi(
                    isMonacoEditor = isMonacoEditor,
                    setIsMonacoEditor = { isMonacoEditorState.value = it },
                    viewModel = viewModel,
                    uiState = uiState,
                    modifier = Modifier
                        .fillMaxSize()
                        .background(MaterialTheme.colorScheme.background)
                        .padding(16.dp)
                )
            } else {
                Box(Modifier.fillMaxSize())
            }
        }
    }
}


@Composable
fun ControllerUi(
    isMonacoEditor: Boolean,
    setIsMonacoEditor: (Boolean) -> Unit,
    viewModel: IdeViewModel,
    uiState: IdeUiState,
    modifier: Modifier = Modifier
) {
    Column(modifier) {
        Row {
            Text(
                "Monaco Editor (WIP)",
                color = MaterialTheme.colorScheme.onBackground
            )
            Switch(isMonacoEditor, setIsMonacoEditor)
        }

        SettingsUi(Modifier)

        Row(
            modifier = Modifier
                .fillMaxHeight()
        ) {
            when (uiState.textFieldType) {
                TextFieldType.DEBUG_OUTPUT -> {
                    uiState.currentEnvironment?.let { environment ->
                        EnvironmentDebugView(
                            environment = environment,
                            modifier = Modifier
                                .weight(1f)
                                .fillMaxHeight()
                        )
                    } ?: run {
                        val textFieldDesc = "デバッグ出力"
                        OutlinedTextField(
                            value = uiState.output,
                            onValueChange = { },
                            modifier = Modifier.weight(1f).fillMaxHeight(),
                            textStyle = MaterialTheme.typography.bodyLarge,
                            label = { Text(textFieldDesc) },
                            readOnly = true
                        )
                    }
                }

                TextFieldType.OUTPUT -> {
                    val textFieldDesc = "出力"
                    OutlinedTextField(
                        value = uiState.output,
                        onValueChange = { },
                        modifier = Modifier.weight(1f).fillMaxHeight(),
                        textStyle = LocalCodeTypography.current.bodyLarge,
                        label = { Text(textFieldDesc) },
                        readOnly = true,
                    )
                }
            }
            with(viewModel) {
                IdeSideButtons(Modifier.fillMaxHeight())
            }
        }
    }
}

@Composable
fun SettingsUi(modifier: Modifier) {
    val usecase = koinInject<io.github.arashiyama11.dncl_ide.domain.usecase.SettingsUseCase>()
    val uiState by
    koinInject<io.github.arashiyama11.dncl_ide.common.AppStateStore<io.github.arashiyama11.dncl_ide.common.StatePermission.Read>>().state.collectAsStateWithLifecycle()

    Column(modifier) {
        Row {
            Text(
                "Suggestion Panel Style:\n ${uiState.uiConfig.suggestionPanelStyle}",
                color = MaterialTheme.colorScheme.onBackground
            )
            Switch(uiState.uiConfig.suggestionPanelStyle == SuggestionPanelStyle.BOTTOM_STRIP, {
                val newStyle =
                    if (uiState.uiConfig.suggestionPanelStyle == SuggestionPanelStyle.BOTTOM_STRIP) {
                        SuggestionPanelStyle.INLINE_DROPDOWN
                    } else {
                        SuggestionPanelStyle.BOTTOM_STRIP
                    }
                usecase.setSuggestionPanelStyle(newStyle)
            })
        }
    }
}
