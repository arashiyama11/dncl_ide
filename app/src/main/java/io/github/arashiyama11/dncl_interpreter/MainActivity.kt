package io.github.arashiyama11.dncl_interpreter

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import io.github.arashiyama11.dncl_interpreter.adapter.IdeViewModel
import io.github.arashiyama11.dncl_interpreter.ui.App
import org.koin.compose.KoinContext

class MainActivity : ComponentActivity() {
    val ideViewModel by viewModels<IdeViewModel>()


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            KoinContext {
                App()
            }
        }
    }

    override fun onPause() {
        super.onPause()
        ideViewModel.onPause()
    }
}