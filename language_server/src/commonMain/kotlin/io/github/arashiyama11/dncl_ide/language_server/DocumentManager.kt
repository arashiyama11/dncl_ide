package io.github.arashiyama11.dncl_ide.language_server

class DocumentManager {
    private val documentContents = mutableMapOf<String, MutableMap<String, String>>()

    fun getDocument(uri: String): String? {
        val notebookCellUri = NotebookCellUri.parse(uri)
        return if (notebookCellUri != null) {
            documentContents[notebookCellUri.notebookUri]?.get(notebookCellUri.cellId)
        } else {
            documentContents["file:///"]?.get(uri)
        }
    }

    fun setDocument(uri: String, text: String) {
        val notebookCellUri = NotebookCellUri.parse(uri)
        if (notebookCellUri != null) {
            documentContents.getOrPut(notebookCellUri.notebookUri) { mutableMapOf() }[notebookCellUri.cellId] = text
        } else {
            documentContents.getOrPut("file:///") { mutableMapOf() }[uri] = text
        }
    }

    fun getAllDocuments(): Map<String, Map<String, String>> {
        return documentContents
    }

    fun calculateOffset(program: String, line: Int, character: Int): Int {
        var offset = 0
        val lines = program.lines()
        for (i in 0 until line) {
            offset += lines[i].length + 1 // +1 for newline character
        }
        offset += character
        return offset
    }
}
