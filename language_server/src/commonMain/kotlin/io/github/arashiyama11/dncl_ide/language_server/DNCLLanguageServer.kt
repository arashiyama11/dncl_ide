package io.github.arashiyama11.dncl_ide.language_server

import io.github.arashiyama11.dncl_ide.domain.usecase.SuggestionUseCase
import arrow.core.Either
import io.github.arashiyama11.dncl_ide.interpreter.lexer.Lexer
import io.github.arashiyama11.dncl_ide.interpreter.model.DnclError
import io.github.arashiyama11.dncl_ide.interpreter.model.Token
import io.github.arashiyama11.dncl_ide.interpreter.parser.Parser
import kotlinx.serialization.builtins.ListSerializer
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.channels.ReceiveChannel
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.decodeFromJsonElement
import kotlinx.serialization.json.encodeToJsonElement

class DNCLLanguageServer {

    private val json = Json {
        prettyPrint = true
        isLenient = false
        ignoreUnknownKeys = true // ignoreUnknownKeysをtrueに戻す
        encodeDefaults = true
    }
    private val outputChannel = Channel<String>(1024)
    val output: ReceiveChannel<String> = outputChannel
    private val documentContents = mutableMapOf<String, MutableMap<String, String>>()

    suspend fun handleMessage(jsonRpcRequest: JsonRpcRequest) {
        try {
            when (jsonRpcRequest.method) {
                "initialize" -> handleInitialize(jsonRpcRequest)
                "initialized" -> handleInitialized()
                "shutdown" -> handleShutdown(jsonRpcRequest)
                "textDocument/didOpen" -> handleDidOpen(jsonRpcRequest)
                "textDocument/didChange" -> handleDidChange(jsonRpcRequest)
                "textDocument/completion" -> handleCompletion(jsonRpcRequest)
                "textDocument/hover" -> handleHover(jsonRpcRequest)
                "textDocument/definition" -> handleDefinition(jsonRpcRequest)
                "textDocument/references" -> handleReferences(jsonRpcRequest)
                "textDocument/rename" -> handleRename(jsonRpcRequest)
                "textDocument/formatting" -> handleFormatting(jsonRpcRequest)
                "textDocument/codeAction" -> handleCodeAction(jsonRpcRequest)
                "textDocument/semanticTokens/full" -> handleSemanticTokensFull(jsonRpcRequest)
                else -> sendErrorResponse(jsonRpcRequest.id, -32601, "Method not found")
            }
        } catch (e: Exception) {
            outputChannel.send(
                json.encodeToString(
                    JsonRpcResponse(
                        id = jsonRpcRequest.id,
                        error = JsonRpcError(
                            code = -32603,
                            message = "Internal error: ${e.message}"
                        )
                    )
                )
            )
        }
    }

    suspend fun handleMessage(message: String) {
        try {
            val jsonRpcRequest = json.decodeFromString<JsonRpcRequest>(message)
            handleMessage(jsonRpcRequest)
        } catch (e: Exception) {
            // Handle parsing errors or other exceptions
            outputChannel.send(
                json.encodeToString(
                    JsonRpcResponse(
                        id = null,
                        error = JsonRpcError(
                            code = -32700,
                            message = "Parse error: ${e.message}"
                        )
                    )
                )
            )
        }
    }

    private suspend fun handleInitialize(request: JsonRpcRequest) {
        val params = request.params?.let { json.decodeFromJsonElement<InitializeParams>(it) }
        // For now, just acknowledge initialization
        val capabilities = ServerCapabilities(
            textDocumentSync = 1, // Full text document synchronization
            completionProvider = CompletionOptions(
                resolveProvider = false,
                triggerCharacters = listOf("：")
            ),
            hoverProvider = true
        )
        val result = InitializeResult(capabilities)
        sendResponse(request.id, json.encodeToJsonElement(InitializeResult.serializer(), result))
    }

    private suspend fun handleInitialized() {
        // Client has acknowledged initialization
        // No response needed for notifications
    }

    private suspend fun handleShutdown(request: JsonRpcRequest) {
        // No specific action for now, just respond
        sendResponse(request.id, null)
    }

    private suspend fun handleDidOpen(request: JsonRpcRequest) {
        val params = request.params?.let { json.decodeFromJsonElement<DidOpenTextDocumentParams>(it) }
        params?.textDocument?.let {
            val notebookCellUri = NotebookCellUri.parse(it.uri)
            if (notebookCellUri != null) {
                documentContents.getOrPut(notebookCellUri.notebookUri) { mutableMapOf() }[notebookCellUri.cellId] =
                    it.text
                publishDiagnostics(it.uri, it.text)
            } else {
                documentContents.getOrPut("file:///") { mutableMapOf() }[it.uri] =
                    it.text // Default to a single file notebook
                publishDiagnostics(it.uri, it.text)
            }
        }
    }

    private suspend fun handleDidChange(request: JsonRpcRequest) {
        val params = request.params?.let { json.decodeFromJsonElement<DidChangeTextDocumentParams>(it) }
        params?.textDocument?.let { docId ->
            params.contentChanges.firstOrNull()?.let { change ->
                val notebookCellUri = NotebookCellUri.parse(docId.uri)
                if (notebookCellUri != null) {
                    documentContents.getOrPut(notebookCellUri.notebookUri) { mutableMapOf() }[notebookCellUri.cellId] =
                        change.text
                } else {
                    documentContents.getOrPut("file:///") { mutableMapOf() }[docId.uri] =
                        change.text
                }
                publishDiagnostics(docId.uri, change.text)
            }
        }
    }

    private suspend fun publishDiagnostics(uri: String, text: String) {
        val diagnostics = mutableListOf<Diagnostic>()
        val lexer = Lexer(text)
        val parser: Parser = when (val parserResult = Parser(lexer)) {
            is Either.Left -> {
                diagnostics.add(parserResult.value.toDiagnostic(text))
                sendNotification(
                    "textDocument/publishDiagnostics",
                    PublishDiagnosticsParams(uri, diagnostics)
                )
                return // Return from the suspend function
            }

            is Either.Right -> parserResult.value
        }

        when (val programResult = parser.parseProgram()) {
            is Either.Left -> diagnostics.add(programResult.value.toDiagnostic(text))
            is Either.Right -> { /* Success, no diagnostics from parser */
            }
        }
        sendNotification(
            "textDocument/publishDiagnostics",
            PublishDiagnosticsParams(uri, diagnostics)
        )
    }

    private fun DnclError.toDiagnostic(program: String): Diagnostic {
        val (line, character) = calculateLineAndCharacter(program, this.errorRange?.first ?: 0)
        val (endLine, endCharacter) = calculateLineAndCharacter(program, this.errorRange?.last ?: 0)

        return Diagnostic(
            range = Range(
                start = Position(line, character),
                end = Position(endLine, endCharacter)
            ),
            severity = 1, // Error
            message = this.explain(program),
            source = "dncl-ls"
        )
    }

    private fun calculateLineAndCharacter(program: String, offset: Int): Pair<Int, Int> {
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

    private suspend fun sendResponse(id: Long?, result: JsonElement?) {
        val response = JsonRpcResponse(id = id, result = result)
        outputChannel.send(json.encodeToString(response))
    }

    private suspend fun sendNotification(method: String, params: Any?) {
        val notification =
            JsonRpcRequest(method = method, params = params?.let { json.encodeToJsonElement(it) })
        outputChannel.send(json.encodeToString(notification))
    }

    private suspend fun handleCompletion(request: JsonRpcRequest) {
        val params = request.params?.let { json.decodeFromJsonElement<CompletionParams>(it) }
        params?.let {
            val code = documentContents[NotebookCellUri.parse(it.textDocument.uri)?.notebookUri
                ?: "file:///"]?.get(
                NotebookCellUri.parse(it.textDocument.uri)?.cellId ?: it.textDocument.uri
            ) ?: return
            val offset = calculateOffset(code, it.position.line, it.position.character)
            val suggestionUseCase = SuggestionUseCase()
            val suggestions = suggestionUseCase.suggestWhenFailingParse(code, offset)
            val completionItems = suggestions.map { def ->
                CompletionItem(
                    label = def.literal,
                    kind = if (def.isFunction) 2 else 1
                ) // 2 for Function, 1 for Text
            }
            sendResponse(
                request.id,
                json.encodeToJsonElement(
                    CompletionList.serializer(),
                    CompletionList(isIncomplete = false, items = completionItems)
                )
            )
        }
    }

    private fun calculateOffset(program: String, line: Int, character: Int): Int {
        var offset = 0
        val lines = program.lines()
        for (i in 0 until line) {
            offset += lines[i].length + 1 // +1 for newline character
        }
        offset += character
        return offset
    }

    private suspend fun handleHover(request: JsonRpcRequest) {
        val params = request.params?.let { json.decodeFromJsonElement<HoverParams>(it) }
        params?.let {
            val code = documentContents[NotebookCellUri.parse(it.textDocument.uri)?.notebookUri
                ?: "file:///"]?.get(
                NotebookCellUri.parse(it.textDocument.uri)?.cellId ?: it.textDocument.uri
            ) ?: return
            val offset = calculateOffset(code, it.position.line, it.position.character)

            val lexer = Lexer(code)
            val tokens = lexer.toList().mapNotNull { it.getOrNull() }

            val hoveredToken = tokens.firstOrNull { token ->
                token.range.contains(offset)
            }

            val hoverContent = when (hoveredToken) {
                is Token.Japanese -> builtInFunctionDescriptions[hoveredToken.literal]
                is Token.Identifier -> builtInFunctionDescriptions[hoveredToken.literal]
                else -> null
            }

            if (hoverContent != null) {
                val hover = Hover(
                    contents = MarkupContent(kind = "markdown", value = hoverContent),
                    range = hoveredToken?.let { token ->
                        val (startLine, startChar) = calculateLineAndCharacter(
                            code,
                            token.range.first
                        )
                        val (endLine, endChar) = calculateLineAndCharacter(code, token.range.last)
                        Range(Position(startLine, startChar), Position(endLine, endChar))
                    }
                )
                sendResponse(request.id, json.encodeToJsonElement(Hover.serializer(), hover))
            } else {
                sendResponse(request.id, null)
            }
        }
    }

    private suspend fun handleDefinition(request: JsonRpcRequest) {
        val params = request.params?.let { json.decodeFromJsonElement<DefinitionParams>(it) }
        params?.let {
            val code = documentContents[NotebookCellUri.parse(it.textDocument.uri)?.notebookUri
                ?: "file:///"]?.get(
                NotebookCellUri.parse(it.textDocument.uri)?.cellId ?: it.textDocument.uri
            ) ?: return
            val offset = calculateOffset(code, it.position.line, it.position.character)

            val lexer = Lexer(code)
            val parser: Parser = when (val parserResult = Parser(lexer)) {
                is Either.Left -> {
                    sendResponse(request.id, null)
                    return
                }

                is Either.Right -> parserResult.value
            }

            val program = when (val programResult = parser.parseProgram()) {
                is Either.Left -> {
                    sendResponse(request.id, null)
                    return
                }

                is Either.Right -> programResult.value
            }

            val suggestionUseCase = SuggestionUseCase()
            val definitions =
                suggestionUseCase.suggestWithParsedData(code, offset, lexer.toList(), program)

            val definitionLocation = definitions.firstOrNull { def ->
                def.position?.let { pos -> pos <= offset && (pos + def.literal.length) >= offset } == true
            }?.let { def ->
                val pos =
                    def.position!! // Now this is safe because the filter ensures it's not null
                val (startLine, startChar) = calculateLineAndCharacter(code, pos)
                val (endLine, endChar) = calculateLineAndCharacter(code, pos + def.literal.length)
                Location(
                    uri = it.textDocument.uri,
                    range = Range(Position(startLine, startChar), Position(endLine, endChar))
                )
            }
            sendResponse(
                request.id,
                definitionLocation?.let { json.encodeToJsonElement(Location.serializer(), it) })
        }
    }

    private suspend fun handleReferences(request: JsonRpcRequest) {
        val params = request.params?.let { json.decodeFromJsonElement<ReferenceParams>(it) }
        params?.let {
            val code = documentContents[NotebookCellUri.parse(it.textDocument.uri)?.notebookUri
                ?: "file:///"]?.get(
                NotebookCellUri.parse(it.textDocument.uri)?.cellId ?: it.textDocument.uri
            ) ?: return
            val offset = calculateOffset(code, it.position.line, it.position.character)

            val lexer = Lexer(code)
            val tokens = lexer.toList().mapNotNull { it.getOrNull() }

            val targetToken = tokens.firstOrNull { token ->
                token.range.contains(offset) && (token is Token.Identifier || token is Token.Japanese)
            }

            val references = mutableListOf<Location>()
            if (targetToken != null) {
                val targetLiteral = targetToken.literal
                tokens.filter { it.literal == targetLiteral && (it is Token.Identifier || it is Token.Japanese) }
                    .forEach { token ->
                        val (startLine, startChar) = calculateLineAndCharacter(
                            code,
                            token.range.first
                        )
                        val (endLine, endChar) = calculateLineAndCharacter(code, token.range.last)
                        references.add(
                            Location(
                                uri = it.textDocument.uri,
                                range = Range(
                                    Position(startLine, startChar),
                                    Position(endLine, endChar)
                                )
                            )
                        )
                    }
            }
            sendResponse(
                request.id,
                json.encodeToJsonElement(ListSerializer(Location.serializer()), references)
            )
        }
    }

    private suspend fun handleRename(request: JsonRpcRequest) {
        val params = request.params?.let { json.decodeFromJsonElement<RenameParams>(it) }
        params?.let {
            val code = documentContents[NotebookCellUri.parse(it.textDocument.uri)?.notebookUri
                ?: "file:///"]?.get(
                NotebookCellUri.parse(it.textDocument.uri)?.cellId ?: it.textDocument.uri
            ) ?: return
            val offset = calculateOffset(code, it.position.line, it.position.character)
            val newName = it.newName

            val lexer = Lexer(code)
            val tokens = lexer.toList().mapNotNull { it.getOrNull() }

            val targetToken = tokens.firstOrNull { token ->
                token.range.contains(offset) && (token is Token.Identifier || token is Token.Japanese)
            }

            if (targetToken != null) {
                val targetLiteral = targetToken.literal
                val edits = mutableListOf<TextEdit>()
                tokens.filter { it.literal == targetLiteral && (it is Token.Identifier || it is Token.Japanese) }
                    .forEach { token ->
                        val (startLine, startChar) = calculateLineAndCharacter(
                            code,
                            token.range.first
                        )
                        val (endLine, endChar) = calculateLineAndCharacter(code, token.range.last)
                        edits.add(
                            TextEdit(
                                range = Range(
                                    Position(startLine, startChar),
                                    Position(endLine, endChar)
                                ),
                                newText = newName
                            )
                        )
                    }
                val textDocumentEdit = TextDocumentEdit(
                    textDocument = VersionedTextDocumentIdentifier(
                        uri = it.textDocument.uri,
                        version = -1
                    ), // -1 for unknown version
                    edits = edits
                )
                val workspaceEdit = WorkspaceEdit(documentChanges = listOf(textDocumentEdit))
                sendResponse(
                    request.id,
                    json.encodeToJsonElement(WorkspaceEdit.serializer(), workspaceEdit)
                )
            } else {
                sendResponse(request.id, null)
            }
        }
    }

    private suspend fun handleFormatting(request: JsonRpcRequest) {
        val params =
            request.params?.let { json.decodeFromJsonElement<DocumentFormattingParams>(it) }
        params?.let {
            val code = documentContents[NotebookCellUri.parse(it.textDocument.uri)?.notebookUri
                ?: "file:///"]?.get(
                NotebookCellUri.parse(it.textDocument.uri)?.cellId ?: it.textDocument.uri
            ) ?: return
            val formattedText = code.lines().joinToString("\n") { line ->
                line.trim()
            }
            val edits = listOf(
                TextEdit(
                    range = Range(
                        start = Position(0, 0),
                        end = Position(code.lines().size, 0)
                    ),
                    newText = formattedText
                )
            )
            sendResponse(
                request.id,
                json.encodeToJsonElement(ListSerializer(TextEdit.serializer()), edits)
            )
        }
    }

    private suspend fun handleCodeAction(request: JsonRpcRequest) {
        val params = request.params?.let { json.decodeFromJsonElement<CodeActionParams>(it) }
        params?.let {
            val codeActions = mutableListOf<CodeAction>()
            it.context.diagnostics.forEach { diagnostic ->
                if (diagnostic.message.contains("未定義の識別子")) {
                    val identifier = diagnostic.message.substringAfter("未定義の識別子 ").trim()
                    val range = diagnostic.range
                    val textEdit = TextEdit(range = range, newText = "表示 $identifier")
                    val textDocumentEdit = TextDocumentEdit(
                        textDocument = VersionedTextDocumentIdentifier(
                            uri = it.textDocument.uri,
                            version = -1
                        ),
                        edits = listOf(textEdit)
                    )
                    val workspaceEdit = WorkspaceEdit(documentChanges = listOf(textDocumentEdit))
                    codeActions.add(
                        CodeAction(
                            title = "'${identifier}' を表示する",
                            kind = "quickfix",
                            diagnostics = listOf(diagnostic),
                            edit = workspaceEdit
                        )
                    )
                }
            }
            sendResponse(
                request.id,
                json.encodeToJsonElement(ListSerializer(CodeAction.serializer()), codeActions)
            )
        }
    }

    private suspend fun handleSemanticTokensFull(request: JsonRpcRequest) {
        val params = request.params?.let { json.decodeFromJsonElement<SemanticTokensParams>(it) }
        params?.let {
            val code = documentContents[NotebookCellUri.parse(it.textDocument.uri)?.notebookUri
                ?: "file:///"]?.get(
                NotebookCellUri.parse(it.textDocument.uri)?.cellId ?: it.textDocument.uri
            ) ?: return
            val lexer = Lexer(code)
            val tokens = lexer.toList().mapNotNull { it.getOrNull() }

            val data = mutableListOf<Int>()
            var lastLine = 0
            var lastChar = 0

            tokens.forEach { token ->
                val (tokenLine, tokenChar) = calculateLineAndCharacter(code, token.range.first)
                val tokenLength = token.literal.length
                val tokenType = getTokenType(token)
                val tokenModifiers = 0 // No modifiers for now

                data.add(tokenLine - lastLine)
                data.add(if (tokenLine == lastLine) tokenChar - lastChar else tokenChar)
                data.add(tokenLength)
                data.add(tokenType)
                data.add(tokenModifiers)

                lastLine = tokenLine
                lastChar = tokenChar
            }
            sendResponse(
                request.id,
                json.encodeToJsonElement(SemanticTokens.serializer(), SemanticTokens(data = data))
            )
        }
    }

    private fun getTokenType(token: Token): Int {
        return when (token) {
            is Token.If, is Token.Function, is Token.Wo, is Token.Kara, is Token.Made, is Token.While, is Token.UpTo, is Token.DownTo, is Token.Define, is Token.Then, is Token.Else, is Token.Elif, is Token.And, is Token.Or -> 0 // keyword
            is Token.Identifier, is Token.Japanese -> 1 // variable
            is Token.Int, is Token.Float -> 3 // number
            is Token.String -> 4 // string
            is Token.Comment -> 5 // comment
            is Token.Plus, is Token.Minus, is Token.Times, is Token.Divide, is Token.DivideInt, is Token.Modulo, is Token.Assign, is Token.Equal, is Token.NotEqual, is Token.GreaterThan, is Token.LessThan, is Token.GreaterThanOrEqual, is Token.LessThanOrEqual, is Token.Bang -> 6 // operator
            else -> 0 // default to keyword
        }
    }

    private val builtInFunctionDescriptions = mapOf(
        "表示する" to "**表示する**: 画面に値を出力する",
        "要素数" to "**要素数**: 配列や文字列の要素数を返す",
        "差分" to "**差分**: 2つの数値の差分を返す",
        "戻り値" to "**戻り値**: 関数の戻り値を指定する",
        "連結" to "**連結**: 文字列や配列を連結する",
        "末尾追加" to "**末尾追加**: 配列の末尾に要素を追加する",
        "先頭削除" to "**先頭削除**: 配列の先頭の要素を削除する",
        "先頭追加" to "**先頭追加**: 配列の先頭に要素を追加する",
        "末尾削除" to "**末尾削除**: 配列の末尾の要素を削除する",
        "整数変換" to "**整数変換**: 値を整数に変換する",
        "浮動小数点変換" to "**浮動小数点変換**: 値を浮動小数点数に変換する",
        "文字列変換" to "**文字列変換**: 値を文字列に変換する",
        "インポート" to "**インポート**: 外部ファイルを読み込む",
        "文字コード" to "**文字コード**: 文字の文字コードを返す",
        "コードから文字" to "**コードから文字**: 文字コードから文字を生成する",
        "部分配列" to "**部分配列**: 配列の部分配列を返す",
        "配列結合" to "**配列結合**: 複数の配列を結合する",
        "並べ替え" to "**並べ替え**: 配列を並べ替える",
        "逆順" to "**逆順**: 配列の要素を逆順にする",
        "検索" to "**検索**: 配列や文字列から要素を検索する",
        "部分文字列" to "**部分文字列**: 文字列の部分文字列を返す",
        "分割" to "**分割**: 文字列を区切り文字で分割する",
        "空白除去" to "**空白除去**: 文字列の先頭と末尾の空白を除去する",
        "置換" to "**置換**: 文字列の一部を置換する",
        "四捨五入" to "**四捨五入**: 数値を四捨五入する",
        "切り捨て" to "**切り捨て**: 数値を切り捨てる",
        "切り上げ" to "**切り上げ**: 数値を切り上げる",
        "乱数" to "**乱数**: 乱数を生成する",
        "最大値" to "**最大値**: 複数の数値の最大値を返す",
        "最小値" to "**最小値**: 複数の数値の最小値を返す",
        "整数判定" to "**整数判定**: 値が整数かどうかを判定する",
        "浮動小数点判定" to "**浮動小数点判定**: 値が浮動小数点数かどうかを判定する",
        "文字列判定" to "**文字列判定**: 値が文字列かどうかを判定する",
        "配列判定" to "**配列判定**: 値が配列かどうかを判定する",
        "真偽値判定" to "**真偽値判定**: 値が真偽値かどうかを判定する",
        "出力消去" to "**出力消去**: コンソール出力を消去する",
        "待機" to "**待機**: 指定した時間だけ処理を停止する"
    )

    private suspend fun sendErrorResponse(id: Long?, code: Int, message: String) {
        println("[Info] Sending error response: code=$code, message=$message")
        val error = JsonRpcError(code = code, message = message)
        val response = JsonRpcResponse(id = id, error = error)
        outputChannel.send(json.encodeToString(response))
    }
}

