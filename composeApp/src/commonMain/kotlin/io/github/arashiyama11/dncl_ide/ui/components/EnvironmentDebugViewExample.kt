package io.github.arashiyama11.dncl_ide.ui.components

import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material.MaterialTheme
import androidx.compose.material.Surface
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import io.github.arashiyama11.dncl_ide.interpreter.model.Environment
import io.github.arashiyama11.dncl_ide.interpreter.model.DnclObject
import io.github.arashiyama11.dncl_ide.interpreter.model.AstNode
import kotlinx.coroutines.runBlocking

/**
 * Example usage of the EnvironmentDebugView Composable.
 * This Composable creates a sample Environment instance and displays it using the EnvironmentDebugView.
 */
@Composable
fun EnvironmentDebugViewExample() {
    // Create a sample Environment instance
    val sampleEnvironment = createSampleEnvironment()

    // Display the Environment using the EnvironmentDebugView
    Surface(
        modifier = Modifier.fillMaxSize(),
        color = MaterialTheme.colors.background
    ) {
        EnvironmentDebugView(
            environment = sampleEnvironment,
            modifier = Modifier.padding(16.dp)
        )
    }
}

/**
 * Creates a sample Environment instance for demonstration purposes.
 * This function populates an Environment with various types of DnclObjects.
 */
private fun createSampleEnvironment(): Environment = runBlocking {
    // Create a dummy AstNode for the DnclObjects
    val dummyAstNode = AstNode.Program(statements = emptyList())

    // Create a parent environment
    val parentEnv = Environment()
    parentEnv.set("parentVar1", DnclObject.Int(42, dummyAstNode))
    parentEnv.set("parentVar2", DnclObject.String("Parent value", dummyAstNode))

    // Create a child environment
    val childEnv = parentEnv.createChildEnvironment()
    childEnv.set("intValue", DnclObject.Int(123, dummyAstNode))
    childEnv.set("floatValue", DnclObject.Float(3.14f, dummyAstNode))
    childEnv.set("stringValue", DnclObject.String("Hello, World!", dummyAstNode))
    childEnv.set("boolValue", DnclObject.Boolean(true, dummyAstNode))

    // Create an array
    val arrayElements = mutableListOf<DnclObject>(
        DnclObject.Int(1, dummyAstNode),
        DnclObject.Int(2, dummyAstNode),
        DnclObject.Int(3, dummyAstNode)
    )
    childEnv.set("arrayValue", DnclObject.Array(arrayElements, dummyAstNode))

    // Create a null value
    childEnv.set("nullValue", DnclObject.Null(dummyAstNode))

    childEnv
}