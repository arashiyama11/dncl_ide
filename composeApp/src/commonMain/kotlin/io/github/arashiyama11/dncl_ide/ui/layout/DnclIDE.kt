package io.github.arashiyama11.dncl_ide.ui.layout

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalWindowInfo
import org.koin.compose.viewmodel.koinViewModel
import io.github.arashiyama11.dncl_ide.adapter.IdeViewModel

@Composable
fun DnclIDE(modifier: Modifier = Modifier, viewModel: IdeViewModel = koinViewModel()) {
    val windowInfo = LocalWindowInfo.current
    val isLandscape = windowInfo.containerSize.width > windowInfo.containerSize.height
    if (isLandscape) {
        DnclIDEHorizontal(modifier, viewModel)
    } else {
        DnclIDEVertical(modifier, viewModel)
    }
}

