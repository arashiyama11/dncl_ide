package io.github.arashiyama11.dncl_ide.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Card
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import io.github.arashiyama11.dncl_ide.interpreter.model.AllBuiltInFunction
import io.github.arashiyama11.dncl_ide.interpreter.model.Environment

@Composable
fun EnvironmentDebugView(
    environment: Environment,
    modifier: Modifier = Modifier
) = Box(modifier.background(MaterialTheme.colorScheme.background)) {
    Column(
        modifier = Modifier.fillMaxWidth()
            .verticalScroll(rememberScrollState())
    ) {
        Card(
            modifier = Modifier.fillMaxWidth().horizontalScroll(rememberScrollState())
                .padding(top = 4.dp, bottom = 4.dp, end = 4.dp),
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
                        style = MaterialTheme.typography.titleSmall
                    )
                }

                Spacer(modifier = Modifier.height(4.dp))

                environment.allKeys().filter { it !in AllBuiltInFunction.allIdentifiers() }
                    .forEach { key ->
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(start = 32.dp, top = 4.dp)
                        ) {
                            Text(
                                text = "${key}: ",
                                style = MaterialTheme.typography.bodyMedium,
                                modifier = Modifier.width(100.dp)
                            )
                            Text(
                                text = environment.get(key).toString(),
                                style = MaterialTheme.typography.bodyMedium,
                                softWrap = false
                            )
                        }
                    }
            }
        }
    }
}