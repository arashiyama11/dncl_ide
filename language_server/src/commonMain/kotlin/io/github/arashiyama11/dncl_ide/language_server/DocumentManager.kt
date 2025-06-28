package io.github.arashiyama11.dncl_ide.language_server

data class DocumentState(
    val documentContents: Map<String, Map<String, String>> = emptyMap()
)

class DocumentManager(private var state: DocumentState = DocumentState()) {

    fun getDocument(uri: String): String? {
        val notebookCellUri = NotebookCellUri.parse(uri)
        return if (notebookCellUri != null) {
            state.documentContents[notebookCellUri.notebookUri]?.get(notebookCellUri.cellId)
        } else {
            state.documentContents["file:///"]?.get(uri)
        }
    }

    fun setDocument(uri: String, text: String): DocumentManager {
        val notebookCellUri = NotebookCellUri.parse(uri)
        val newContents = if (notebookCellUri != null) {
            val notebookMap = state.documentContents[notebookCellUri.notebookUri] ?: emptyMap()
            val updatedNotebookMap = notebookMap + (notebookCellUri.cellId to text)
            state.documentContents + (notebookCellUri.notebookUri to updatedNotebookMap)
        } else {
            val fileMap = state.documentContents["file:///"] ?: emptyMap()
            val updatedFileMap = fileMap + (uri to text)
            state.documentContents + ("file:///" to updatedFileMap)
        }

        return DocumentManager(DocumentState(newContents))
    }

    fun updateState(uri: String, text: String) {
        val notebookCellUri = NotebookCellUri.parse(uri)
        val newContents = if (notebookCellUri != null) {
            val notebookMap = state.documentContents[notebookCellUri.notebookUri] ?: emptyMap()
            val updatedNotebookMap = notebookMap + (notebookCellUri.cellId to text)
            state.documentContents + (notebookCellUri.notebookUri to updatedNotebookMap)
        } else {
            val fileMap = state.documentContents["file:///"] ?: emptyMap()
            val updatedFileMap = fileMap + (uri to text)
            state.documentContents + ("file:///" to updatedFileMap)
        }

        state = DocumentState(newContents)
    }

    fun getAllDocuments(): Map<String, Map<String, String>> {
        return state.documentContents
    }

    companion object {
        fun calculateOffset(program: String, line: Int, character: Int): Int {
            var offset = 0
            val lines = program.lines()
            for (i in 0 until line) {
                if (i < lines.size) {
                    offset += lines[i].length + 1 // +1 for newline character
                }
            }
            offset += character
            return offset
        }
    }
}
