package io.github.arashiyama11.dncl_interpreter.ui

import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import io.github.arashiyama11.dncl_interpreter.ui.theme.Dncl_interpreterTheme

@Composable
fun App() {
    Dncl_interpreterTheme {
        Scaffold() { contentPadding ->
            DnclIDE(
                modifier = Modifier.padding(contentPadding)
            )
        }
    }
}


