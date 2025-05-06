package io.github.arashiyama11.dncl_ide.ui.components

import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material.MaterialTheme
import androidx.compose.material.Surface
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import io.github.arashiyama11.dncl_ide.interpreter.lexer.Lexer
import io.github.arashiyama11.dncl_ide.interpreter.model.AstNode
import io.github.arashiyama11.dncl_ide.interpreter.parser.Parser

/**
 * Example usage of the AstNodeDebugView Composable.
 * This Composable creates a sample AstNode instance and displays it using the AstNodeDebugView.
 */
@Composable
fun AstNodeDebugViewExample() {
    // Create a sample AstNode instance
    val sampleAstNode = createSampleAstNode()

    // Display the AstNode using the AstNodeDebugView
    Surface(
        modifier = Modifier.fillMaxSize(),
        color = MaterialTheme.colors.background
    ) {
        AstNodeDebugView(
            astNode = sampleAstNode,
            modifier = Modifier.padding(16.dp)
        )
    }
}

/**
 * Creates a sample AstNode instance for demonstration purposes.
 * This function parses a simple code snippet and returns the resulting AST.
 */
private fun createSampleAstNode(): AstNode {
    // Simple code snippet to parse
    val codeSnippet = """Data = [3,18,29,33,48,52,62,77,89,97]
kazu = 要素数(Data)
表示する("0～99の数字を入力してください")
atai = 整数変換(【外部からの入力】)
hidari = 0 , migi = kazu - 1
owari = 0
hidari <= migi and owari == 0 の間繰り返す:
  aida = (hidari+migi) ÷ 2 # 演算子÷は商の整数値を返す
  もし Data[aida] == atai ならば:
    表示する(atai, "は", aida, "番目にありました")
    owari = 1
  そうでなくもし Data[aida] < atai ならば:
    hidari = aida + 1
  そうでなければ:
    migi = aida - 1
もし owari == 0 ならば:
  表示する(atai, "は見つかりませんでした")
表示する("添字", " ", "要素")
i を 0 から kazu - 1 まで 1 ずつ増やしながら繰り返す:
  表示する(i, " ", Data[i])
"""

    // Parse the code snippet to create an AstNode
    val parser = Parser(Lexer(codeSnippet)).getOrNull()!!
    val program = parser.parseProgram()

    // Return the parsed program or a simple fallback if parsing fails
    return program.getOrNull() ?: createFallbackAstNode()
}

/**
 * Creates a simple fallback AstNode in case parsing fails.
 */
private fun createFallbackAstNode(): AstNode {
    // Create a simple program with one statement
    return AstNode.Program(
        statements = listOf(
            AstNode.ExpressionStatement(
                expression = AstNode.StringLiteral(
                    value = "Parsing failed, this is a fallback AST",
                    range = 0..30
                )
            )
        )
    )
}