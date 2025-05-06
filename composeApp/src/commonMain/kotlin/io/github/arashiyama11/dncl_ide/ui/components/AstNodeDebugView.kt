package io.github.arashiyama11.dncl_ide.ui.components

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
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
import io.github.arashiyama11.dncl_ide.interpreter.model.AstNode

/**
 * A Composable function that displays information about an AstNode in a tree structure.
 * This is a debug tool to visualize the AST (Abstract Syntax Tree).
 *
 * @param astNode The AstNode to display
 * @param modifier Modifier for the Composable
 */
@Composable
fun AstNodeDebugView(
    astNode: AstNode,
    modifier: Modifier = Modifier
) {
    LazyColumn(modifier = modifier) {
        item {
            AstNodeItem(astNode = astNode, level = 0)
        }
    }
}

/**
 * A Composable function that displays a single AstNode with its children.
 *
 * @param astNode The AstNode to display
 * @param level The indentation level (depth in the tree)
 */
@Composable
private fun AstNodeItem(
    astNode: AstNode,
    level: Int
) {
    var expanded by remember { mutableStateOf(false) }

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(start = (level * 16).dp, top = 4.dp, bottom = 4.dp, end = 4.dp),
        elevation = 2.dp
    ) {
        Column(modifier = Modifier.padding(8.dp)) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.fillMaxWidth()
            ) {
                // Expand/collapse button for nodes with children
                if (hasChildren(astNode)) {
                    IconButton(onClick = { expanded = !expanded }) {
                        Icon(
                            imageVector = if (expanded) Icons.Default.KeyboardArrowDown else Icons.Default.KeyboardArrowRight,
                            contentDescription = if (expanded) "Collapse" else "Expand"
                        )
                    }
                } else {
                    Spacer(modifier = Modifier.width(48.dp))
                }

                // Node type and basic info
                Column {
                    null!!
                    Text(
                        text = astNode::class.simpleName ?: "Unknown",
                        style = MaterialTheme.typography.subtitle1
                    )
                    Text(
                        text = "Literal: ${astNode.literal}",
                        style = MaterialTheme.typography.body2
                    )
                }
            }

            // Display child nodes if expanded
            if (expanded) {
                Spacer(modifier = Modifier.height(4.dp))
                when (astNode) {
                    is AstNode.Program -> {
                        astNode.statements.forEach { statement ->
                            AstNodeItem(astNode = statement, level = level + 1)
                        }
                    }

                    is AstNode.BlockStatement -> {
                        astNode.statements.forEach { statement ->
                            AstNodeItem(astNode = statement, level = level + 1)
                        }
                    }

                    is AstNode.ExpressionStatement -> {
                        AstNodeItem(astNode = astNode.expression, level = level + 1)
                    }

                    is AstNode.IfStatement -> {
                        Text("Condition:", style = MaterialTheme.typography.caption)
                        AstNodeItem(astNode = astNode.condition, level = level + 1)
                        Text("Consequence:", style = MaterialTheme.typography.caption)
                        AstNodeItem(astNode = astNode.consequence, level = level + 1)
                        astNode.alternative?.let {
                            Text("Alternative:", style = MaterialTheme.typography.caption)
                            AstNodeItem(astNode = it, level = level + 1)
                        }
                    }

                    is AstNode.ForStatement -> {
                        Text(
                            "Loop Counter: ${astNode.loopCounter.literal}",
                            style = MaterialTheme.typography.caption
                        )
                        Text("Start:", style = MaterialTheme.typography.caption)
                        AstNodeItem(astNode = astNode.start, level = level + 1)
                        Text("End:", style = MaterialTheme.typography.caption)
                        AstNodeItem(astNode = astNode.end, level = level + 1)
                        Text(
                            "Step (${astNode.stepType}):",
                            style = MaterialTheme.typography.caption
                        )
                        AstNodeItem(astNode = astNode.step, level = level + 1)
                        Text("Block:", style = MaterialTheme.typography.caption)
                        AstNodeItem(astNode = astNode.block, level = level + 1)
                    }

                    is AstNode.WhileStatement -> {
                        Text("Condition:", style = MaterialTheme.typography.caption)
                        AstNodeItem(astNode = astNode.condition, level = level + 1)
                        Text("Block:", style = MaterialTheme.typography.caption)
                        AstNodeItem(astNode = astNode.block, level = level + 1)
                    }

                    is AstNode.WhileExpression -> {
                        Text("Condition:", style = MaterialTheme.typography.caption)
                        AstNodeItem(astNode = astNode.condition, level = level + 1)
                        Text("Block:", style = MaterialTheme.typography.caption)
                        AstNodeItem(astNode = astNode.block, level = level + 1)
                    }

                    is AstNode.FunctionStatement -> {
                        Text(
                            "Parameters: ${astNode.parameters.joinToString(", ")}",
                            style = MaterialTheme.typography.caption
                        )
                        Text("Block:", style = MaterialTheme.typography.caption)
                        AstNodeItem(astNode = astNode.block, level = level + 1)
                    }

                    is AstNode.FunctionLiteral -> {
                        Text(
                            "Parameters: ${astNode.parameters.joinToString(", ")}",
                            style = MaterialTheme.typography.caption
                        )
                        Text("Body:", style = MaterialTheme.typography.caption)
                        AstNodeItem(astNode = astNode.body, level = level + 1)
                    }

                    is AstNode.CallExpression -> {
                        Text("Function:", style = MaterialTheme.typography.caption)
                        AstNodeItem(astNode = astNode.function, level = level + 1)
                        if (astNode.arguments.isNotEmpty()) {
                            Text("Arguments:", style = MaterialTheme.typography.caption)
                            astNode.arguments.forEach { arg ->
                                AstNodeItem(astNode = arg, level = level + 1)
                            }
                        }
                    }

                    is AstNode.PrefixExpression -> {
                        Text(
                            "Operator: ${astNode.operator.literal}",
                            style = MaterialTheme.typography.caption
                        )
                        Text("Right:", style = MaterialTheme.typography.caption)
                        AstNodeItem(astNode = astNode.right, level = level + 1)
                    }

                    is AstNode.InfixExpression -> {
                        Text("Left:", style = MaterialTheme.typography.caption)
                        AstNodeItem(astNode = astNode.left, level = level + 1)
                        Text(
                            "Operator: ${astNode.operator.literal}",
                            style = MaterialTheme.typography.caption
                        )
                        Text("Right:", style = MaterialTheme.typography.caption)
                        AstNodeItem(astNode = astNode.right, level = level + 1)
                    }

                    is AstNode.IndexExpression -> {
                        Text("Left:", style = MaterialTheme.typography.caption)
                        AstNodeItem(astNode = astNode.left, level = level + 1)
                        Text("Index:", style = MaterialTheme.typography.caption)
                        AstNodeItem(astNode = astNode.right, level = level + 1)
                    }

                    is AstNode.AssignStatement -> {
                        astNode.assignments.forEachIndexed { index, (assignable, expression) ->
                            Text(
                                "Assignment ${index + 1}:",
                                style = MaterialTheme.typography.caption
                            )
                            Text("Target:", style = MaterialTheme.typography.caption)
                            AstNodeItem(astNode = assignable, level = level + 1)
                            Text("Value:", style = MaterialTheme.typography.caption)
                            AstNodeItem(astNode = expression, level = level + 1)
                        }
                    }

                    is AstNode.ArrayLiteral -> {
                        if (astNode.elements.isNotEmpty()) {
                            Text("Elements:", style = MaterialTheme.typography.caption)
                            astNode.elements.forEachIndexed { index, element ->
                                Text("Element ${index}:", style = MaterialTheme.typography.caption)
                                AstNodeItem(astNode = element, level = level + 1)
                            }
                        }
                    }
                    // For leaf nodes (no children), we don't need to display anything extra
                    else -> {}
                }
            }
        }
    }
}

/**
 * Determines if an AstNode has child nodes.
 *
 * @param astNode The AstNode to check
 * @return True if the node has children, false otherwise
 */
private fun hasChildren(astNode: AstNode): Boolean {
    return when (astNode) {
        is AstNode.Program -> astNode.statements.isNotEmpty()
        is AstNode.BlockStatement -> astNode.statements.isNotEmpty()
        is AstNode.ExpressionStatement -> true
        is AstNode.IfStatement -> true
        is AstNode.ForStatement -> true
        is AstNode.WhileStatement -> true
        is AstNode.WhileExpression -> true
        is AstNode.FunctionStatement -> true
        is AstNode.FunctionLiteral -> true
        is AstNode.CallExpression -> true
        is AstNode.PrefixExpression -> true
        is AstNode.InfixExpression -> true
        is AstNode.IndexExpression -> true
        is AstNode.AssignStatement -> astNode.assignments.isNotEmpty()
        is AstNode.ArrayLiteral -> astNode.elements.isNotEmpty()
        else -> false
    }
}