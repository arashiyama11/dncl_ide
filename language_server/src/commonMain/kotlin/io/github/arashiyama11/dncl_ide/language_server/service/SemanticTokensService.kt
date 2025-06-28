package io.github.arashiyama11.dncl_ide.language_server.service

import io.github.arashiyama11.dncl_ide.interpreter.lexer.Lexer
import io.github.arashiyama11.dncl_ide.interpreter.model.Token
import io.github.arashiyama11.dncl_ide.interpreter.model.AstNode
import io.github.arashiyama11.dncl_ide.language_server.SemanticTokens
import io.github.arashiyama11.dncl_ide.language_server.util.calculateLineAndCharacter
import io.github.arashiyama11.dncl_ide.language_server.ast.Symbol
import io.github.arashiyama11.dncl_ide.language_server.ast.SymbolKind

class SemanticTokensService(
    private val astInfoService: AstInfoService
) {
    fun getSemanticTokens(code: String): SemanticTokens {
        val lexer = Lexer(code)
        val tokens = lexer.toList().mapNotNull { it.getOrNull() }

        // ASTとシンボルテーブルを取得
        val astInfo = astInfoService.parseAndAnalyze(code)

        val data = mutableListOf<Int>()
        var lastLine = 0
        var lastChar = 0

        tokens.forEach { token ->
            if (token is Token.Indent || token is Token.NewLine) {
                return@forEach
            }
            val (tokenLine, tokenChar) = calculateLineAndCharacter(code, token.range.first)
            val tokenLength = token.literal.length + if (token is Token.String) 2 else 0
            val tokenType = getEnhancedTokenType(token, astInfo, token.range.first)
            val tokenModifiers = getTokenModifiers(token, astInfo, token.range.first)
            if (tokenType == -1) {
                return@forEach
            }

            /*0"keyword",
                       1"variable",
                       2"function",
                       3"number",
                       4"string",
                       5"comment",
                       6"operator",
                       7"parameter"*/
            //println("$token delta line: ${tokenLine - lastLine}, delta char: ${if (tokenLine == lastLine) tokenChar - lastChar else tokenChar}, length: $tokenLength, type: $tokenType, modifiers: $tokenModifiers")
            data.add(tokenLine - lastLine)
            data.add(if (tokenLine == lastLine) tokenChar - lastChar else tokenChar)
            data.add(tokenLength)
            data.add(tokenType)
            data.add(tokenModifiers)

            lastLine = tokenLine
            lastChar = tokenChar
        }
        return SemanticTokens(data = data)
    }

    private fun getEnhancedTokenType(token: Token, astInfo: AstInfo?, offset: Int): Int {
        // 基本的なトークンタイプのマッピング
        val basicType = when (token) {
            is Token.If, is Token.Function, is Token.Wo, is Token.Kara, is Token.Made,
            is Token.While, is Token.UpTo, is Token.DownTo, is Token.Define, is Token.Then,
            is Token.Else, is Token.Elif, is Token.And, is Token.Or -> 0 // keyword
            is Token.Int, is Token.Float -> 3 // number
            is Token.String -> 4 // string
            is Token.Comment -> 5 // comment
            is Token.Plus, is Token.Minus, is Token.Times, is Token.Divide, is Token.DivideInt,
            is Token.Modulo, is Token.Assign, is Token.Equal, is Token.NotEqual,
            is Token.GreaterThan, is Token.LessThan, is Token.GreaterThanOrEqual,
            is Token.LessThanOrEqual, is Token.Bang -> 6 // operator
            else -> -1 // 未判定
        }

        // 基本的なトークンタイプが確定している場合はそれを返す
        if (basicType != -1) return basicType

        // 識別子の場合、ASTとシンボルテーブルを使用してより詳細な解析を行う
        if (token is Token.Identifier || token is Token.Japanese) {
            astInfo?.let { info ->
                // シンボルテーブルから情報を取得
                val symbol = info.symbolTable.resolve(token.literal, offset)
                symbol?.let {
                    return when (it.kind) {
                        SymbolKind.FUNCTION -> 2 // function
                        SymbolKind.BUILT_IN_FUNCTION -> 2 // function
                        SymbolKind.PARAMETER -> 7 // parameter
                        SymbolKind.VARIABLE -> 1 // variable
                        SymbolKind.UNKNOWN -> 1 // variable (default)
                    }
                }

                // シンボルテーブルにない場合、ASTノードから判定
                val node = astInfoService.findNodeAtOffset(info, offset)
                node?.let { astNode ->
                    return when {
                        // 関数定義の文脈
                        isInFunctionDefinition(astNode) -> 2 // function
                        // 関数呼び出しの文脈
                        isInFunctionCall(astNode) -> 2 // function
                        // 変数宣言の文脈
                        isInVariableDeclaration(astNode) -> 1 // variable
                        // 配列アクセスの文脈
                        isInArrayAccess(astNode) -> 1 // variable
                        else -> 1 // variable (default)
                    }
                }
            }
        }

        return -1
    }

    private fun getTokenModifiers(token: Token, astInfo: AstInfo?, offset: Int): Int {
        var modifiers = 0

        // 識別子の場合のみ修飾子を計算
        if (token is Token.Identifier || token is Token.Japanese) {
            astInfo?.let { info ->
                val symbol = info.symbolTable.resolve(token.literal, offset)
                symbol?.let {
                    // 定義箇所かどうかチェック
                    if (isDefinitionSite(info, offset, it)) {
                        modifiers = modifiers or 1 // definition modifier
                    }

                    // 読み取り専用かどうかチェック
                    if (it.kind == SymbolKind.PARAMETER) {
                        modifiers = modifiers or 2 // readonly modifier
                    }
                }
            }
        }

        return modifiers
    }

    private fun isInFunctionDefinition(node: AstNode): Boolean {
        return when (node) {
            is AstNode.FunctionStatement -> true
            else -> false
        }
    }

    private fun isInFunctionCall(node: AstNode): Boolean {
        return when (node) {
            is AstNode.CallExpression -> true
            else -> false
        }
    }

    private fun isInVariableDeclaration(node: AstNode): Boolean {
        return when (node) {
            is AstNode.AssignStatement -> true
            else -> false
        }
    }

    private fun isInArrayAccess(node: AstNode): Boolean {
        return when (node) {
            is AstNode.IndexExpression -> true
            else -> false
        }
    }

    private fun isDefinitionSite(astInfo: AstInfo, offset: Int, symbol: Symbol): Boolean {
        // シンボルの定義位置と現在の位置が一致するか���ェック
        return offset in symbol.range
    }
}
