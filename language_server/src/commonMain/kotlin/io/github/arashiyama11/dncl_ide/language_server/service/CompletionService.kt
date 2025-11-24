package io.github.arashiyama11.dncl_ide.language_server.service

import arrow.core.Either
import io.github.arashiyama11.dncl_ide.interpreter.lexer.Lexer
import io.github.arashiyama11.dncl_ide.interpreter.model.AllBuiltInFunction
import io.github.arashiyama11.dncl_ide.interpreter.model.AstNode
import io.github.arashiyama11.dncl_ide.interpreter.model.BuiltInFunctionSignature
import io.github.arashiyama11.dncl_ide.interpreter.model.DnclError
import io.github.arashiyama11.dncl_ide.interpreter.model.Token
import io.github.arashiyama11.dncl_ide.interpreter.parser.Parser
import io.github.arashiyama11.dncl_ide.interpreter.preprocessor.preProcess
import io.github.arashiyama11.dncl_ide.language_server.CompletionItem
import io.github.arashiyama11.dncl_ide.language_server.FileResolver
import io.github.arashiyama11.dncl_ide.language_server.service.StdlibOnlyFileResolver
import io.github.arashiyama11.dncl_ide.language_server.service.resolveLibText
import kotlinx.coroutines.flow.toList
import kotlin.math.abs
import kotlin.math.max
import kotlin.math.min

class CompletionService(
    private val fileResolver: FileResolver = StdlibOnlyFileResolver()
) {
    suspend fun getCompletionItems(
        code: String,
        offset: Int,
        filePath: String? = null,
        astInfo: AstInfo? = null
    ): List<CompletionItem> {
        val suggestionUseCase = SuggestionUseCase(fileResolver, filePath)
        val suggestions = suggestionUseCase.suggestWithAstOrFallback(code, offset, astInfo)
        return suggestions.map { def ->
            CompletionItem(
                label = def.literal,
                kind = def.kind.completionItemKind,
                detail = def.detail ?: def.kind.defaultDetail,
                insertText = def.insertText ?: def.literal,
                insertTextFormat = def.insertTextFormat
            )
        }
    }
}

@Suppress("UNUSED")
private object LspCompletionItemKind {
    const val TEXT = 1
    const val METHOD = 2
    const val FUNCTION = 3
    const val CONSTRUCTOR = 4
    const val FIELD = 5
    const val VARIABLE = 6
    const val CLASS = 7
    const val INTERFACE = 8
    const val MODULE = 9
    const val PROPERTY = 10
    const val UNIT = 11
    const val VALUE = 12
    const val ENUM = 13
    const val KEYWORD = 14
    const val SNIPPET = 15
    const val COLOR = 16
    const val FILE = 17
    const val REFERENCE = 18
    const val FOLDER = 19
    const val ENUM_MEMBER = 20
    const val CONSTANT = 21
    const val STRUCT = 22
    const val EVENT = 23
    const val OPERATOR = 24
    const val TYPE_PARAMETER = 25
}

@Suppress("unused")
private object LspInsertTextFormat {
    const val PLAINTEXT = 1
    const val SNIPPET = 2
}

private enum class SuggestionKind(
    val completionItemKind: Int,
    val defaultDetail: String?
) {
    BuiltinFunction(LspCompletionItemKind.FUNCTION, "組み込み関数"),
    UserFunction(LspCompletionItemKind.FUNCTION, "関数"),
    FunctionVariable(LspCompletionItemKind.FUNCTION, "関数（変数）"),
    Variable(LspCompletionItemKind.VARIABLE, "変数"),
    Parameter(LspCompletionItemKind.VARIABLE, "引数"),
    LoopVariable(LspCompletionItemKind.VARIABLE, "ループ変数"),
    Keyword(LspCompletionItemKind.KEYWORD, "キーワード"),
    Snippet(LspCompletionItemKind.SNIPPET, "スニペット")
}

private data class Definition(
    val literal: String,
    val position: Int?,
    val kind: SuggestionKind,
    val detail: String? = null,
    val insertText: String? = null,
    val insertTextFormat: Int? = null
)

private val KEYWORD_SUGGESTIONS = listOf(
    Definition(
        literal = "もし",
        position = null,
        kind = SuggestionKind.Keyword,
        insertText = "もし ",
        detail = "if 条件開始"
    ),
    Definition(
        literal = "ならば:",
        position = null,
        kind = SuggestionKind.Keyword,
        insertText = "ならば:\n",
        detail = "if ブロック開始"
    ),
    Definition(
        literal = "そうでなければ:",
        position = null,
        kind = SuggestionKind.Keyword,
        insertText = "そうでなければ:\n",
        detail = "else ブロック開始"
    ),
    Definition(
        literal = "そうでなくもし",
        position = null,
        kind = SuggestionKind.Keyword,
        insertText = "そうでなくもし ",
        detail = "elif 相当"
    ),
    Definition(
        literal = "の間繰り返す:",
        position = null,
        kind = SuggestionKind.Keyword,
        insertText = "の間繰り返す:\n",
        detail = "while 相当"
    ),
    Definition(
        literal = "関数",
        position = null,
        kind = SuggestionKind.Keyword,
        insertText = "関数 ",
        detail = "関数宣言"
    ),
    Definition(
        literal = "と定義する",
        position = null,
        kind = SuggestionKind.Keyword,
        insertText = "と定義する\n",
        detail = "関数宣言の終端"
    ),
    Definition(
        literal = "ずつ増やしながら繰り返す",
        position = null,
        kind = SuggestionKind.Keyword,
        insertText = "ずつ増やしながら繰り返す:\n",
        detail = "for 増加ループ"
    ),
    Definition(
        literal = "ずつ減らしながら繰り返す",
        position = null,
        kind = SuggestionKind.Keyword,
        insertText = "ずつ減らしながら繰り返す:\n",
        detail = "for 減少ループ"
    )
)

private val SNIPPET_SUGGESTIONS = listOf(
    Definition(
        literal = "if 文（もし／そうでなければ）",
        position = null,
        kind = SuggestionKind.Snippet,
        detail = "if 文",
        insertText = "もし \${1:条件} ならば:\n  \${0}\nそうでなければ:\n  ",
        insertTextFormat = LspInsertTextFormat.SNIPPET
    ),
    Definition(
        literal = "if-elif 文",
        position = null,
        kind = SuggestionKind.Snippet,
        detail = "if-elif 文",
        insertText = "もし \${1:条件} ならば:\n  \${0}\nそうでなくもし \${2:条件} ならば:\n  ",
        insertTextFormat = LspInsertTextFormat.SNIPPET
    ),
    Definition(
        literal = "for 文（増加）",
        position = null,
        kind = SuggestionKind.Snippet,
        detail = "for 文",
        insertText = "\${1:i} を \${2:0} から \${3:10} まで \${4:1} ずつ増やしながら繰り返す:\n  \${0}",
        insertTextFormat = LspInsertTextFormat.SNIPPET
    ),
    Definition(
        literal = "while 文",
        position = null,
        kind = SuggestionKind.Snippet,
        detail = "while 文",
        insertText = "\${1:条件} の間繰り返す:\n  \${0}",
        insertTextFormat = LspInsertTextFormat.SNIPPET
    ),
    Definition(
        literal = "関数定義",
        position = null,
        kind = SuggestionKind.Snippet,
        detail = "関数定義",
        insertText = "関数 \${1:名前}(\${2:引数}) を:\n  \${0}\nと定義する\n",
        insertTextFormat = LspInsertTextFormat.SNIPPET
    )
)

private val BASE_SUGGESTIONS = KEYWORD_SUGGESTIONS + SNIPPET_SUGGESTIONS

private val DEFAULT_BUILTIN_DEFINITIONS = AllBuiltInFunction.allIdentifiers().map {
    Definition(
        literal = it,
        position = null,
        kind = SuggestionKind.BuiltinFunction
    )
}

private fun builtInDefinitions(signatures: List<BuiltInFunctionSignature>): List<Definition> {
    if (signatures.isEmpty()) return DEFAULT_BUILTIN_DEFINITIONS
    return signatures.map { sig ->
        val params = sig.params.joinToString(", ")
        Definition(
            literal = sig.name,
            position = null,
            kind = SuggestionKind.BuiltinFunction,
            detail = if (params.isBlank()) "${sig.name}()" else "${sig.name}($params)",
            insertText = if (params.isBlank()) "${sig.name}()" else "${sig.name}($params)"
        )
    }
}

private data class ParseResult(
    val program: AstNode.Program?,
    val builtInSignatures: List<BuiltInFunctionSignature>
)

private val SUGGESTION_KIND_PRIORITY = mapOf(
    SuggestionKind.Keyword to 0,
    SuggestionKind.UserFunction to 1,
    SuggestionKind.BuiltinFunction to 2,
    SuggestionKind.FunctionVariable to 3,
    SuggestionKind.Variable to 3,
    SuggestionKind.Parameter to 3,
    SuggestionKind.LoopVariable to 3,
    SuggestionKind.Snippet to 4
)

private class SuggestionUseCase(
    private val fileResolver: FileResolver,
    private val filePath: String?
) {
    suspend fun suggestWithAstOrFallback(
        code: String,
        position: Int,
        astInfo: AstInfo?
    ): List<Definition> {
        if (astInfo != null) {
            val tokens = Lexer(code, filePath).toList()
            return suggestWithParsedProgram(
                code = code,
                position = position,
                tokens = tokens,
                program = astInfo.ast,
                builtInSignatures = astInfo.builtInSignatures
            )
        }

        // AST が無い場合は従来のフォールバック（軽量パース付き）
        val fixedCode = code.substring(0 until position) + "u" + code.substring(position)
        val lexer = Lexer(fixedCode, filePath)
        val parseResult = parseProgramWithBuiltIns(lexer, filePath)
        val program = parseResult.program
            ?: run {
                val fallback = BASE_SUGGESTIONS + builtInDefinitions(parseResult.builtInSignatures)
                val query = extractActiveQuery(code, position)
                return sortCandidates(
                    candidates = fallback,
                    query = query,
                    position = position,
                    codeLength = code.length
                )
            }
        return suggestWithParsedProgram(
            code = code,
            position = position,
            tokens = Lexer(code, filePath).toList(),
            program = program,
            builtInSignatures = parseResult.builtInSignatures
        )

    }

    fun suggestWithParsedProgram(
        code: String,
        position: Int,
        tokens: List<Either<DnclError, Token>>,
        program: AstNode.Program,
        builtInSignatures: List<BuiltInFunctionSignature>
    ): List<Definition> {
        val positionTokenIndex =
            tokens.indexOfFirst { it.getOrNull()?.range?.contains(position) == true }
        val currentToken =
            tokens.slice(
                max(
                    0,
                    positionTokenIndex - 1
                ) until min(positionTokenIndex + 1, tokens.size)
            )
                .firstOrNull { it.getOrNull() is Token.Identifier || it.getOrNull() is Token.Japanese }
                ?: tokens.getOrNull(positionTokenIndex)
        val globalDefinitions = collectGlobalDefinitions(program, builtInSignatures)
        val words =
            (collectDefinitions(
                statements = program.statements,
                depth = Int.MAX_VALUE,
                limitPosition = position,
                targetFilePath = filePath
            ) + globalDefinitions + BASE_SUGGESTIONS).distinctBy { it.literal to it.kind }

        val activeTokenLiteral = currentToken?.getOrNull()?.let { token ->
            when (token) {
                is Token.Identifier, is Token.Japanese -> token.literal
                else -> null
            }
        }

        val query = activeTokenLiteral ?: extractActiveQuery(code, position)
        return sortCandidates(words, query, position, code.length)
    }

    private fun collectGlobalDefinitions(
        program: AstNode.Program,
        builtInSignatures: List<BuiltInFunctionSignature>
    ): List<Definition> {
        return builtInDefinitions(builtInSignatures) +
                collectDefinitions(
                    statements = program.statements,
                    depth = 1,
                    limitPosition = Int.MAX_VALUE,
                    targetFilePath = filePath
                )
    }

    private fun collectDefinitions(
        statements: List<AstNode.Statement>,
        depth: Int,
        limitPosition: Int,
        targetFilePath: String? = null
    ): List<Definition> {
        if (depth == 0) return emptyList()
        if (statements.isEmpty()) return emptyList()
        val result = mutableListOf<Definition>()
        for (stmt in statements) {
            if (targetFilePath != null && stmt.filePath != null && stmt.filePath != targetFilePath) {
                continue
            }
            if (stmt.range.first > limitPosition) return result
            when (stmt) {
                is AstNode.AssignStatement -> {
                    result.addAll(stmt.assignments.map {
                        Definition(
                            literal = it.first.literal,
                            position = it.first.range.first,
                            kind = if (it.second is AstNode.FunctionLiteral) {
                                SuggestionKind.FunctionVariable
                            } else {
                                SuggestionKind.Variable
                            }
                        )
                    })
                }

                is AstNode.BlockStatement -> {
                    result.addAll(
                        collectDefinitions(
                            stmt.statements,
                            depth - 1,
                            limitPosition,
                            targetFilePath
                        )
                    )
                }

                is AstNode.ExpressionStatement -> {}
                is AstNode.ForStatement -> {
                    result.add(
                        Definition(
                            literal = stmt.loopCounter.literal,
                            position = stmt.loopCounter.range.first,
                            kind = SuggestionKind.LoopVariable
                        )
                    )
                    result.addAll(
                        collectDefinitions(
                            stmt.block.statements,
                            depth - 1,
                            limitPosition,
                            targetFilePath
                        )
                    )
                }

                is AstNode.FunctionStatement -> {
                    result.add(
                        Definition(
                            literal = stmt.name.literal,
                            position = stmt.name.range.first,
                            kind = SuggestionKind.UserFunction
                        )
                    )
                    if (stmt.range.contains(limitPosition)) {
                        result.addAll(stmt.parameters.map {
                            Definition(
                                literal = it.literal,
                                position = it.range.first,
                                kind = SuggestionKind.Parameter
                            )
                        })
                        result.addAll(
                            collectDefinitions(
                                stmt.block.statements,
                                depth - 1,
                                limitPosition,
                                targetFilePath
                            )
                        )
                    }
                }

                is AstNode.IfStatement -> {
                    result.addAll(
                        collectDefinitions(
                            stmt.consequence.statements,
                            depth - 1,
                            limitPosition,
                            targetFilePath
                        )
                    )
                    stmt.alternative?.let {
                        result.addAll(
                            collectDefinitions(
                                it.statements,
                                depth - 1,
                                limitPosition,
                                targetFilePath
                            )
                        )
                    }
                }

                is AstNode.WhileStatement -> {
                    result.addAll(
                        collectDefinitions(
                            stmt.block.statements,
                            depth - 1,
                            limitPosition,
                            targetFilePath
                        )
                    )
                }
            }
        }
        return result
    }

    private fun sortCandidates(
        candidates: List<Definition>,
        query: String?,
        position: Int,
        codeLength: Int
    ): List<Definition> {
        val normalizedQuery = query?.takeIf { it.isNotBlank() }?.lowercase()

        val scored = candidates.mapIndexed { index, definition ->
            val matchRank =
                normalizedQuery?.let { computeMatchRank(definition.literal, it) } ?: Int.MAX_VALUE
            val editDistance = normalizedQuery?.let {
                val distance =
                    levenshteinDistance(definition.literal.lowercase(), it, maxDistance = 4)
                if (distance > 4) Int.MAX_VALUE else distance
            } ?: Int.MAX_VALUE
            val proximity = computeProximity(definition, position, codeLength)

            val basePriority = SUGGESTION_KIND_PRIORITY.getOrElse(definition.kind) {
                Int.MAX_VALUE
            }
            val kindPriority = when {
                normalizedQuery == null -> when (definition.kind) {
                    SuggestionKind.Keyword -> basePriority - 10
                    SuggestionKind.Snippet -> basePriority - 9
                    else -> basePriority
                }

                else -> when (definition.kind) {
                    SuggestionKind.Keyword -> basePriority + 10
                    SuggestionKind.Snippet -> basePriority + 9
                    else -> basePriority
                }
            }

            val lengthPriority =
                if (normalizedQuery != null && matchRank <= 1) definition.literal.length else Int.MAX_VALUE

            ScoredDefinition(
                definition = definition,
                matchRank = matchRank,
                editDistance = editDistance,
                proximity = proximity,
                kindPriority = kindPriority,
                lengthPriority = lengthPriority,
                literalLength = definition.literal.length,
                originalIndex = index
            )
        }

        val comparator = compareBy<ScoredDefinition>(
            { it.matchRank },
            { it.editDistance },
            { it.lengthPriority },
            { it.kindPriority },
            { it.proximity },
            { it.literalLength },
            { it.originalIndex }
        )

        return scored.sortedWith(comparator).map { it.definition }
    }

    private suspend fun parseProgramWithBuiltIns(
        lexer: Lexer,
        filePath: String? = null
    ): ParseResult {
        val builtIns = mutableListOf<BuiltInFunctionSignature>()
        val tokens = preProcess(
            lexer,
            resolveLib = { path -> resolveLibText(fileResolver, path) },
            onBuiltInSignature = { builtIns += it }
        ).toList()
        val program = Parser(tokens).fold(
            ifLeft = { null },
            ifRight = { parser ->
                parser.parseProgram().fold(
                    ifLeft = { null },
                    ifRight = { it }
                )
            }
        )
        return ParseResult(program, builtIns)
    }

    private fun computeMatchRank(target: String, query: String): Int {
        val lowerTarget = target.lowercase()
        return when {
            lowerTarget == query -> 0
            lowerTarget.startsWith(query) -> 1
            lowerTarget.contains(query) -> 2
            else -> 3
        }
    }

    private fun computeProximity(definition: Definition, position: Int, codeLength: Int): Int {
        val declarationPos = definition.position ?: return Int.MAX_VALUE
        return if (position >= declarationPos) {
            position - declarationPos
        } else {
            codeLength + (declarationPos - position)
        }
    }

    private fun levenshteinDistance(a: String, b: String, maxDistance: Int): Int {
        if (a == b) return 0
        if (a.isEmpty()) return b.length
        if (b.isEmpty()) return a.length
        if (abs(a.length - b.length) > maxDistance) return maxDistance + 1

        var previous = IntArray(b.length + 1) { it }
        var current = IntArray(b.length + 1)

        for (i in 1..a.length) {
            current[0] = i
            var bestForRow = current[0]
            val charA = a[i - 1]

            for (j in 1..b.length) {
                val cost = if (charA == b[j - 1]) 0 else 1
                val deletion = previous[j] + 1
                val insertion = current[j - 1] + 1
                val substitution = previous[j - 1] + cost
                val value = min(min(deletion, insertion), substitution)
                current[j] = value
                if (value < bestForRow) bestForRow = value
            }

            if (bestForRow > maxDistance) {
                return maxDistance + 1
            }

            val temp = previous
            previous = current
            current = temp
        }

        return previous[b.length]
    }

    private data class ScoredDefinition(
        val definition: Definition,
        val matchRank: Int,
        val editDistance: Int,
        val proximity: Int,
        val kindPriority: Int,
        val lengthPriority: Int,
        val literalLength: Int,
        val originalIndex: Int
    )

    private fun extractActiveQuery(code: String, position: Int): String? {
        if (code.isEmpty()) return null
        val clamp = position.coerceIn(0, code.length)
        var start = clamp
        while (start > 0 && code[start - 1].isIdentifierChar()) {
            start--
        }
        if (start == clamp) return null
        return code.substring(start, clamp)
    }

    private fun Char.isIdentifierChar(): Boolean {
        return this == '_' || isLetterOrDigit()
    }
}
