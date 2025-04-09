package io.github.arashiyama11.dncl_ide

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.compose.runtime.Composable
import androidx.compose.ui.tooling.preview.Preview
import io.github.arashiyama11.dncl_ide.adapter.IdeViewModel
import io.github.arashiyama11.dncl_ide.ui.App

class MainActivity : ComponentActivity() {
    val ideViewModel by viewModels<IdeViewModel>()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        setContent {
            App()
        }
    }

    override fun onPause() {
        super.onPause()
        ideViewModel.onPause()
    }
}

@Preview
@Composable
fun AppAndroidPreview() {
    App()
}