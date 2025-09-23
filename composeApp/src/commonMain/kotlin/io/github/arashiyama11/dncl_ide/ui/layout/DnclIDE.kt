package io.github.arashiyama11.dncl_ide.ui.layout

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalWindowInfo
import org.koin.compose.viewmodel.koinViewModel
import io.github.arashiyama11.dncl_ide.adapter.IdeViewModel
import io.github.arashiyama11.dncl_ide.ui.ScreenOrientation
import io.github.arashiyama11.dncl_ide.ui.orientation

@Composable
fun DnclIDE(modifier: Modifier = Modifier, viewModel: IdeViewModel = koinViewModel()) {
    when (LocalWindowInfo.current.orientation) {
        ScreenOrientation.PORTRAIT -> DnclIDEVertical(modifier, viewModel)
        ScreenOrientation.LANDSCAPE -> DnclIDEHorizontal(modifier, viewModel)
    }
}

