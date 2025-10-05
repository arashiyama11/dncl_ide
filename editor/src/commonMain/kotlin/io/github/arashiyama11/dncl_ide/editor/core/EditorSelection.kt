package io.github.arashiyama11.dncl_ide.editor.core

/** 選択範囲（キャレット位置含む）を 0-based index で表現。 */
data class EditorSelection(
    val start: Int,
    val end: Int
) {
    init {
        require(start >= 0) { "start must be >= 0 (was $start)" }
        require(end >= 0) { "end must be >= 0 (was $end)" }
    }

    val isCollapsed: Boolean get() = start == end

    fun normalize(): EditorSelection = if (start <= end) this else EditorSelection(end, start)
}
