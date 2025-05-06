package io.github.arashiyama11.dncl_ide.ui.components

import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.Card
import androidx.compose.material.Icon
import androidx.compose.material.IconButton
import androidx.compose.material.MaterialTheme
import androidx.compose.material.Text
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.KeyboardArrowRight
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import io.github.arashiyama11.dncl_ide.interpreter.model.Environment
import io.github.arashiyama11.dncl_ide.interpreter.model.DnclObject

@Composable
fun EnvironmentDebugView(
    environment: Environment,
    modifier: Modifier = Modifier
) = Box(modifier) {
    Column(
        modifier = Modifier.fillMaxWidth()
            .verticalScroll(rememberScrollState())
    ) {
        Card(
            modifier = Modifier.fillMaxWidth().horizontalScroll(rememberScrollState())
                .padding(top = 4.dp, bottom = 4.dp, end = 4.dp),
            elevation = 2.dp
        ) {
            Column(modifier = Modifier.fillMaxSize().padding(8.dp)) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(
                        text = if (environment.allKeys()
                                .isNotEmpty()
                        ) "Variables:" else "Empty environment",
                        style = MaterialTheme.typography.subtitle1
                    )
                }

                Spacer(modifier = Modifier.height(4.dp))

                environment.allKeys().forEach { key ->
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(start = 32.dp, top = 4.dp)
                    ) {
                        Text(
                            text = "${key}: ",
                            style = MaterialTheme.typography.body2,
                            modifier = Modifier.width(100.dp)
                        )
                        Text(
                            text = environment.get(key).toString(),
                            style = MaterialTheme.typography.body2,
                            softWrap = false
                        )
                    }
                }
            }
        }
    }
}