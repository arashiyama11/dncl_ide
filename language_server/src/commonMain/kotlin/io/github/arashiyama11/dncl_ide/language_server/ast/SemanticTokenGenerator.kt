package io.github.arashiyama11.dncl_ide.language_server.ast

import io.github.arashiyama11.dncl_ide.interpreter.model.AllBuiltInFunction
import io.github.arashiyama11.dncl_ide.interpreter.model.AstNode
import io.github.arashiyama11.dncl_ide.interpreter.model.SymbolTable
import io.github.arashiyama11.dncl_ide.interpreter.model.Symbol
import io.github.arashiyama11.dncl_ide.interpreter.model.SymbolKind
import io.github.arashiyama11.dncl_ide.interpreter.model.Token

class SemanticTokenGenerator(
    private val code: String,
    private val globalScope: SymbolTable,
    private val tokens: List<Token> // Lexerから得られたトークンリスト
) {
    private val data = mutableListOf<Int>()
    private var lastLine = 0
    private var lastChar = 0
    private var currentScope: SymbolTable = globalScope

    // LSPのSemantic Token Types (一部抜粋)
    // https://microsoft.github.io/language-server-protocol/specifications/lsp/3.17/specification/#semanticTokenTypes
    private val tokenTypes = listOf(
        "namespace", "type", "class", "enum", "interface", "struct", "parameter", "variable",
        "property", "enumMember", "event", "function", "method", "macro", "keyword", "modifier",
        "comment", "string", "number", "regexp", "operator", "decorator"
    )

    fun generate(program: AstNode.Program): List<Int> {
        visitProgram(program)
        return data
    }

    private fun addToken(token: Token, type: Int, modifiers: Int = 0) {
        val (tokenLine, tokenChar) = calculateLineAndCharacter(code, token.range.first)
        val tokenLength = token.literal.length

        data.add(tokenLine - lastLine)
        data.add(if (tokenLine == lastLine) tokenChar - lastChar else tokenChar)
        data.add(tokenLength)
        data.add(type)
        data.add(modifiers)

        lastLine = tokenLine
        lastChar = tokenChar
    }

    fun calculateLineAndCharacter(program: String, offset: Int): Pair<Int, Int> {
        var line = 0
        var character = 0
        var currentOffset = 0

        val lines = program.lines()
        for ((idx, s) in lines.withIndex()) {
            if (currentOffset + s.length + 1 > offset) { // +1 for newline character
                line = idx
                character = offset - currentOffset
                break
            }
            currentOffset += s.length + 1
        }
        return Pair(line, character)
    }

    private fun getTokenAtRange(range: IntRange): Token? {
        return tokens.firstOrNull { it.range == range }
    }

    private fun enterScope(newScope: SymbolTable) {
        currentScope // TODO: 新しいSymbolTableベースのスコープ管理を実装
    }

    private fun exitScope() {
        // TODO: SymbolTableに親スコープへの参照機能を追加する必要がある
    }

    private fun visitProgram(program: AstNode.Program) {
        program.statements.forEach { visitStatement(it) }
    }

    private fun visitStatement(statement: AstNode.Statement) {
        when (statement) {
            is AstNode.AssignStatement -> {
                statement.assignments.forEach { (assignable, expression) ->
                    if (assignable is AstNode.Identifier) {
                        getTokenAtRange(assignable.range)?.let { token ->
                            addToken(token, tokenTypes.indexOf("variable"))
                        }
                    }
                    visitExpression(expression)
                }
            }

            is AstNode.ExpressionStatement -> visitExpression(statement.expression)
            is AstNode.IfStatement -> {
                getTokenAtRange(statement.condition.range)?.let { token ->
                    // "もし" キーワード
                    if (token is Token.If) addToken(token, tokenTypes.indexOf("keyword"))
                }
                visitExpression(statement.condition)
                // "ならば" キーワード
                tokens.firstOrNull { it.range.first > statement.condition.range.last && it is Token.Then }
                    ?.let { token ->
                        addToken(token, tokenTypes.indexOf("keyword"))
                    }
                // スコープの切り替えはSymbolTableBuilderで行われるため、ここではASTの走査のみ
                visitBlockStatement(statement.consequence)
                statement.alternative?.let {
                    // "そうでなければ" キーワード
                    tokens.firstOrNull { t -> t.range.first > statement.consequence.range.last && t is Token.Else }
                        ?.let { token ->
                            addToken(token, tokenTypes.indexOf("keyword"))
                        }
                    visitBlockStatement(it)
                }
            }

            is AstNode.ForStatement -> {
                // "繰り返し" キーワード
                tokens.firstOrNull { it.range.first == statement.range.first && it is Token.Japanese && it.literal == "繰り返し" }
                    ?.let { token ->
                        addToken(token, tokenTypes.indexOf("keyword"))
                    }
                getTokenAtRange(statement.loopCounter.range)?.let { token ->
                    addToken(token, tokenTypes.indexOf("variable")) // ループカウンタは変数
                }
                // "から" "まで" "ずつ増やす/減らす" キー��ード
                tokens.filter { it.range.first > statement.loopCounter.range.last && (it is Token.Kara || it is Token.Made || it is Token.UpTo || it is Token.DownTo) }
                    .forEach { token ->
                        addToken(token, tokenTypes.indexOf("keyword"))
                    }
                visitExpression(statement.start)
                visitExpression(statement.end)
                visitExpression(statement.step)
                visitBlockStatement(statement.block)
            }

            is AstNode.WhileStatement -> {
                // "間" キーワ���ド
                tokens.firstOrNull { it.range.first == statement.range.first && it is Token.While }
                    ?.let { token ->
                        addToken(token, tokenTypes.indexOf("keyword"))
                    }
                visitExpression(statement.condition)
                visitBlockStatement(statement.block)
            }

            is AstNode.FunctionStatement -> {
                // "関数" キーワード
                tokens.firstOrNull { it.range.first == statement.range.first && it is Token.Function }
                    ?.let { token ->
                        addToken(token, tokenTypes.indexOf("keyword"))
                    }
                // 関数名
                tokens.firstOrNull { it.range.first == statement.range.first + 3 && (it is Token.Identifier || it is Token.Japanese) && it.literal == statement.name.literal }
                    ?.let { token ->
                        addToken(token, tokenTypes.indexOf("function"))
                    }
                statement.parameters.forEach { paramName ->
                    // パラメータ
                    tokens.firstOrNull { it.literal == paramName.literal && it.range.first > statement.range.first && it.range.last < statement.block.range.first }
                        ?.let { token ->
                            addToken(token, tokenTypes.indexOf("parameter"))
                        }
                }
                visitBlockStatement(statement.block)
                // "定義終わり" キーワード
                tokens.firstOrNull { it.range.first > statement.block.range.last && it is Token.Define }
                    ?.let { token ->
                        addToken(token, tokenTypes.indexOf("keyword"))
                    }
            }

            is AstNode.BlockStatement -> {
                visitBlockStatement(statement)
            }
        }
    }

    private fun visitBlockStatement(block: AstNode.BlockStatement) {
        block.statements.forEach { visitStatement(it) }
    }

    private fun visitExpression(expression: AstNode.Expression) {
        when (expression) {
            is AstNode.Identifier -> {
                getTokenAtRange(expression.range)?.let { token ->
                    val symbol = currentScope.resolve(expression.value, expression.range.first)
                    if (symbol != null) {
                        addToken(token, tokenTypes.indexOf(getTokenType(symbol)))
                    } else {
                        // 組み込み関数かどうか
                        if (AllBuiltInFunction.allIdentifiers().contains(expression.value)) {
                            addToken(token, tokenTypes.indexOf("function"))
                        } else {
                            addToken(
                                token,
                                tokenTypes.indexOf("variable")
                            ) // 未定義の識別子もとりあえず変数としてハイライト
                        }
                    }
                }
            }

            is AstNode.IntLiteral, is AstNode.FloatLiteral -> {
                getTokenAtRange(expression.range)?.let { token ->
                    addToken(token, tokenTypes.indexOf("number"))
                }
            }

            is AstNode.StringLiteral -> {
                getTokenAtRange(expression.range)?.let { token ->
                    addToken(token, tokenTypes.indexOf("string"))
                }
            }

            is AstNode.BooleanLiteral -> {
                getTokenAtRange(expression.range)?.let { token ->
                    addToken(token, tokenTypes.indexOf("keyword")) // 真/偽はキーワード
                }
            }

            is AstNode.SystemLiteral -> {
                getTokenAtRange(expression.range)?.let { token ->
                    addToken(token, tokenTypes.indexOf("macro")) // システムリテラルはマクロ
                }
            }

            is AstNode.PrefixExpression -> {
                getTokenAtRange(expression.operator.range)?.let { token ->
                    addToken(token, tokenTypes.indexOf("operator"))
                }
                visitExpression(expression.right)
            }

            is AstNode.InfixExpression -> {
                visitExpression(expression.left)
                getTokenAtRange(expression.operator.range)?.let { token ->
                    addToken(token, tokenTypes.indexOf("operator"))
                }
                visitExpression(expression.right)
            }

            is AstNode.IndexExpression -> {
                visitExpression(expression.left)
                visitExpression(expression.right)
            }

            is AstNode.CallExpression -> {
                visitExpression(expression.function)
                expression.arguments.forEach { visitExpression(it) }
            }

            is AstNode.ArrayLiteral -> expression.elements.forEach { visitExpression(it) }
            is AstNode.FunctionLiteral -> {
                // "関数" キーワード
                tokens.firstOrNull { it.range.first == expression.range.first && it is Token.Function }
                    ?.let { token ->
                        addToken(token, tokenTypes.indexOf("keyword"))
                    }
                expression.parameters.forEach { paramName ->
                    // パラメータ
                    tokens.firstOrNull { it.literal == paramName.literal && it.range.first > expression.range.first && it.range.last < expression.body.range.first }
                        ?.let { token ->
                            addToken(token, tokenTypes.indexOf("parameter"))
                        }
                }
                visitBlockStatement(expression.body)
                // "定義終わり" キーワード
                tokens.firstOrNull { it.range.first > expression.body.range.last && it is Token.Define }
                    ?.let { token ->
                        addToken(token, tokenTypes.indexOf("keyword"))
                    }
            }

            is AstNode.WhileExpression -> {
                // "間" キーワード
                tokens.firstOrNull { it.range.first == expression.range.first && it is Token.While }
                    ?.let { token ->
                        addToken(token, tokenTypes.indexOf("keyword"))
                    }
                visitExpression(expression.condition)
                visitBlockStatement(expression.block)
                // "終わり" キーワード
                tokens.firstOrNull { it.range.first > expression.block.range.last && it is Token.Japanese && it.literal == "終わり" }
                    ?.let { token ->
                        addToken(token, tokenTypes.indexOf("keyword"))
                    }
            }
        }
    }

    private fun getTokenType(symbol: Symbol): String {
        return when (symbol.kind) {
            SymbolKind.VARIABLE -> "variable"
            SymbolKind.FUNCTION -> "function"
            SymbolKind.PARAMETER -> "parameter"
            SymbolKind.BUILT_IN_FUNCTION -> "function"
            SymbolKind.UNKNOWN -> "variable"
        }
    }
}