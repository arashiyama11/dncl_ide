package io.github.arashiyama11.dncl_ide.language_server

data class DocumentState(
    val documentContents: Map<String, Map<String, String>> = emptyMap()
)

class DocumentManager(private val state: DocumentState = DocumentState()) {

    // Helper to update document contents immutably
    private fun updateContents(
        contents: Map<String, Map<String, String>>,
        uri: String,
        text: String
    ): Map<String, Map<String, String>> {
        val notebookCellUri = NotebookCellUri.parse(uri)
        return if (notebookCellUri != null) {
            val notebookMap = contents[notebookCellUri.notebookUri] ?: emptyMap()
            contents + (notebookCellUri.notebookUri to (notebookMap + (notebookCellUri.cellId to text)))
        } else {
            val fileMap = contents["file:///"] ?: emptyMap()
            contents + ("file:///" to (fileMap + (uri to text)))
        }
    }

    fun getDocument(uri: String): String? {
        val notebookCellUri = NotebookCellUri.parse(uri)
        return if (notebookCellUri != null) {
            state.documentContents[notebookCellUri.notebookUri]?.get(notebookCellUri.cellId)
        } else {
            state.documentContents["file:///"]?.get(uri)
        }
    }

    fun setDocument(uri: String, text: String): DocumentManager {
        val newContents = updateContents(state.documentContents, uri, text)
        return DocumentManager(DocumentState(newContents))
    }

    fun getAllDocuments(): Map<String, Map<String, String>> {
        return state.documentContents
    }
}
