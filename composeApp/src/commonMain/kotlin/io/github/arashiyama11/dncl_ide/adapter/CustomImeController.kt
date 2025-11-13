package io.github.arashiyama11.dncl_ide.adapter

import androidx.compose.ui.text.TextRange
import androidx.compose.ui.text.input.TextFieldValue
import io.github.arashiyama11.dncl_ide.interpreter.lexer.Lexer
import io.github.arashiyama11.dncl_ide.interpreter.model.Token
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update

/**
 * カスタムIMEに関する状態管理とテキスト操作を担うクラス。
 * トークン列ベースでスニペット順位を決定し、入力状況に応じたカスタムIME体験を提供する。
 */
class CustomImeController(
    private val getCurrentTextValue: () -> TextFieldValue,
    private val onTextChanged: (TextFieldValue, Boolean?) -> Unit,
    defaultSnippets: List<CustomImeSnippet> = DEFAULT_SNIPPETS,
    defaultQuickKeys: List<String> = DEFAULT_QUICK_KEYS,
    defaultKeywords: List<CustomImeKeyword> = DEFAULT_KEYWORDS
) {
    data class SnippetContext(
        val tokens: List<Token>,
        val cursorPosition: Int,
        val currentLiteral: String?,
        val precedingLiterals: List<String>
    )

    private val baselineSnippets = MutableStateFlow(defaultSnippets.toMutableList())
    private val _snippets = MutableStateFlow(defaultSnippets)
    val snippets: StateFlow<List<CustomImeSnippet>> = _snippets.asStateFlow()

    private val _quickKeys = MutableStateFlow(defaultQuickKeys)
    val quickKeys: StateFlow<List<String>> = _quickKeys.asStateFlow()

    private val _keywords = MutableStateFlow(defaultKeywords)
    val keywords: StateFlow<List<CustomImeKeyword>> = _keywords.asStateFlow()

    private var currentContext: SnippetContext = SnippetContext(
        tokens = emptyList(),
        cursorPosition = 0,
        currentLiteral = null,
        precedingLiterals = emptyList()
    )

    private val snippetTokenCache: MutableMap<String, List<Token>> = mutableMapOf()

    var rankingStrategy: SnippetRankingStrategy = SnippetRankingStrategy.Identity
        set(value) {
            field = value
            refreshSnippetOrdering()
        }

    init {
        rebuildSnippetTokenCache()
        refreshSnippetOrdering()
    }

    fun setSnippets(snippets: List<CustomImeSnippet>) {
        baselineSnippets.value = snippets.toMutableList()
        rebuildSnippetTokenCache()
        refreshSnippetOrdering()
    }

    fun updateSnippets(transform: (List<CustomImeSnippet>) -> List<CustomImeSnippet>) {
        baselineSnippets.update { current ->
            transform(current.toList()).toMutableList()
        }
        rebuildSnippetTokenCache()
        refreshSnippetOrdering()
    }

    fun setQuickKeys(quickKeys: List<String>) {
        _quickKeys.value = quickKeys
    }

    fun updateQuickKeys(transform: (List<String>) -> List<String>) {
        _quickKeys.update { current -> transform(current) }
    }

    fun setKeywords(keywords: List<CustomImeKeyword>) {
        _keywords.value = keywords
    }

    fun updateKeywords(transform: (List<CustomImeKeyword>) -> List<CustomImeKeyword>) {
        _keywords.update { current -> transform(current) }
    }

    fun onSnippetSelected(snippet: CustomImeSnippet) {
        commitText(snippet.body)
    }

    fun onQuickKeySelected(symbol: String) {
        commitText(symbol)
    }

    fun onKeywordSelected(keyword: CustomImeKeyword) {
        commitText(keyword.text)
    }

    fun onInsertNewLine() {
        commitText("\n")
    }

    fun onDeleteBackward() {
        val currentValue = getCurrentTextValue()
        val selection = currentValue.selection
        val start = selection.start
        val end = selection.end

        if (start == 0 && end == 0) return

        val (deleteStart, deleteEnd) = when {
            start != end -> start to end
            start > 0 -> (start - 1) to start
            else -> return
        }

        val newText = buildString {
            append(currentValue.text.substring(0, deleteStart))
            append(currentValue.text.substring(deleteEnd))
        }
        val newValue = TextFieldValue(
            text = newText,
            selection = TextRange(deleteStart)
        )
        onTextChanged(newValue, true)
    }

    /**
     * テキスト編集時に呼び出し、カーソル位置とトークン列を基にスニペット順位を更新する。
     */
    fun onEditorContextChanged(updatedValue: TextFieldValue, tokens: List<Token>) {
        val newContext = SnippetContext(
            tokens = tokens,
            cursorPosition = updatedValue.selection.start,
            currentLiteral = extractCurrentLiteral(updatedValue, tokens),
            precedingLiterals = extractPrecedingLiterals(tokens, updatedValue.selection.start)
        )
        if (newContext == currentContext) return
        currentContext = newContext
        refreshSnippetOrdering()
    }

    private fun refreshSnippetOrdering() {
        val base = baselineSnippets.value.toList()
        _snippets.value = rankingStrategy.order(base, currentContext, ::tokensForSnippet)
    }

    private fun commitText(text: String) {
        val currentValue = getCurrentTextValue()
        val selection = currentValue.selection
        val newText = buildString {
            append(currentValue.text.substring(0, selection.start))
            append(text)
            append(currentValue.text.substring(selection.end))
        }
        val newCursor = selection.start + text.length
        val newValue = TextFieldValue(newText, TextRange(newCursor))
        onTextChanged(newValue, true)
    }

    private fun extractCurrentLiteral(
        value: TextFieldValue,
        tokens: List<Token>
    ): String? {
        val cursor = value.selection.start.coerceAtLeast(0).coerceAtMost(value.text.length)
        if (cursor == 0) return null
        val focused = tokens.lastOrNull { it.range.first < cursor }
        if (focused != null && cursor <= focused.range.last + 1) {
            val start = focused.range.first.coerceAtLeast(0)
            val end = cursor.coerceAtMost(focused.range.last + 1).coerceAtMost(value.text.length)
            if (start < end) {
                return value.text.substring(start, end)
            }
        }
        return extractTextSuffix(value.text, cursor)
    }

    private fun extractPrecedingLiterals(tokens: List<Token>, cursorPosition: Int): List<String> {
        return tokens
            .filter { it.range.last < cursorPosition }
            .takeLast(5)
            .map { it.literal }
    }

    private fun extractTextSuffix(text: String, cursor: Int): String? {
        if (cursor == 0) return null
        val prefix = text.substring(0, cursor.coerceAtMost(text.length))
        val suffix = prefix.takeLastWhile { !it.isWhitespace() }
        return suffix.ifBlank { null }
    }

    private fun rebuildSnippetTokenCache() {
        snippetTokenCache.clear()
        baselineSnippets.value.forEach { snippet ->
            snippetTokenCache[snippet.id] = lexDncl(snippet.body)
        }
    }

    private fun tokensForSnippet(snippet: CustomImeSnippet): List<Token> =
        snippetTokenCache[snippet.id] ?: lexDncl(snippet.body).also {
            snippetTokenCache[snippet.id] = it
        }

    private fun lexDncl(text: String): List<Token> =
        Lexer(text).mapNotNull { it.getOrNull() }

    fun interface SnippetRankingStrategy {
        fun order(
            snippets: List<CustomImeSnippet>,
            context: SnippetContext,
            snippetTokensProvider: (CustomImeSnippet) -> List<Token>
        ): List<CustomImeSnippet>

        companion object {
            val Identity = SnippetRankingStrategy { snippets, _, _ -> snippets }

            val PrefixMatch = SnippetRankingStrategy { snippets, context, snippetTokens ->
                val query = context.currentLiteral?.lowercase()?.takeIf { it.isNotBlank() }
                    ?: return@SnippetRankingStrategy snippets
                val history = context.precedingLiterals.map { it.lowercase() }
                snippets.sortedWith(
                    compareByDescending<CustomImeSnippet> {
                        val tokens = snippetTokens(it).map { token -> token.literal.lowercase() }
                        when {
                            tokens.firstOrNull()?.startsWith(query) == true -> 4
                            tokens.any { literal -> literal.startsWith(query) } -> 3
                            it.title.lowercase().startsWith(query) -> 2
                            history.isNotEmpty() &&
                                    tokens.firstOrNull() == history.lastOrNull() -> 1

                            else -> 0
                        }
                    }.thenBy { it.title }
                )
            }
        }
    }

    companion object {
        val DEFAULT_SNIPPETS: List<CustomImeSnippet> = listOf(
            CustomImeSnippet(
                id = "if-basic",
                title = "もし〜ならば",
                body = "もし 条件 ならば:\n  ",
                description = "条件分岐の基本形"
            ),
            CustomImeSnippet(
                id = "if-else",
                title = "もし〜そうでなければ",
                body = "もし 条件 ならば:\n  \nそうでなければ:\n  ",
                description = "if/else テンプレート"
            ),
            CustomImeSnippet(
                id = "elif-inline",
                title = "そうでなくもし",
                body = "そうでなくもし 条件 ならば:",
                description = "elif 節のテンプレート"
            ),
            CustomImeSnippet(
                id = "else-inline",
                title = "そうでなければ",
                body = "そうでなければ:",
                description = "else 節のテンプレート"
            ),
            CustomImeSnippet(
                id = "repeat-loop",
                title = "繰り返しテンプレ",
                body = "i を 0 から 上限 まで 1 ずつ増やしながら繰り返す:\n  ",
                description = "カウンタ付き繰り返し構文"
            ),
            CustomImeSnippet(
                id = "while-loop",
                title = "の間繰り返す",
                body = "条件 の間繰り返す:",
                description = "条件成立時に継続するループ"
            ),
            CustomImeSnippet(
                id = "function",
                title = "関数定義",
                body = "関数 名前(引数)を:\n  \nと定義する\n",
                description = "基本的な関数の骨組み"
            ),
            CustomImeSnippet(
                id = "function-return",
                title = "関数定義(戻り値)",
                body = "関数 f(x) を:\n  戻り値(x+1)\nと定義する",
                description = "戻り値を伴う関数定義例"
            ),
            CustomImeSnippet(
                id = "input-placeholder",
                title = "外部入力プレースホルダ",
                body = "【外部からの入力】",
                description = "外部入力を示すマーカー"
            )
        )

        val DEFAULT_QUICK_KEYS: List<String> = listOf(
            "(", ")",
            "[", "]",
            "{", "}",
            "\"\"",
            "==", "!=", "<=", ">=",
            "+", "-", "*", "/",
            ":"
        )

        val DEFAULT_KEYWORDS: List<CustomImeKeyword> = listOf(
            CustomImeKeyword(
                id = "keyword-if",
                label = "もし",
                text = "もし "
            ),
            CustomImeKeyword(
                id = "keyword-if-end",
                label = "ならば",
                text = "ならば:"
            ),
            CustomImeKeyword(
                id = "keyword-elif",
                label = "そうでなくもし",
                text = "そうでなくもし"
            ),
            CustomImeKeyword(
                id = "keyword-else",
                label = "そうでなければ",
                text = "そうでなければ:"
            ),
            CustomImeKeyword(
                id = "keyword-wo",
                label = "を",
                text = "を"
            ),
            CustomImeKeyword(
                id = "keyword-kara",
                label = "から",
                text = "から"
            ),
            CustomImeKeyword(
                id = "keyword-made",
                label = "まで",
                text = "まで"
            ),
            CustomImeKeyword(
                id = "keyword-increase",
                label = "ずつ増やしながら繰り返す",
                text = "ずつ増やしながら繰り返す:"
            ),
            CustomImeKeyword(
                id = "keyword-decrease",
                label = "ずつ減らしながら繰り返す",
                text = "ずつ減らしながら繰り返す:"
            ),
            CustomImeKeyword(
                id = "keyword-while",
                label = "条件繰り返し",
                text = "i < 10 の間繰り返す:"
            ),
            CustomImeKeyword(
                id = "keyword-input",
                label = "外部入力",
                text = "【外部からの入力】"
            )
        )
    }
}
