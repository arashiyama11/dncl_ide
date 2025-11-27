package io.github.arashiyama11.dncl_ide.language_server

import io.github.arashiyama11.dncl_ide.language_server.service.AstInfo

data class DocumentState(
    val documentContents: Map<String, Map<String, DocumentEntry>> = emptyMap()
)

data class DocumentEntry(
    val text: String,
    val version: Int,
    val diagnostics: List<Diagnostic> = emptyList(),
    val astInfo: AstInfo? = null
)

data class DocumentAnalysis(
    val diagnostics: List<Diagnostic>? = null,
    val astInfo: AstInfo? = null
)

data class DocumentSnapshot(
    val text: String,
    val version: Int,
    val diagnostics: List<Diagnostic>,
    val astInfo: AstInfo?
)

class DocumentManager(private val state: DocumentState = DocumentState()) {

    private fun updateEntry(
        uri: String,
        transform: (DocumentEntry?) -> DocumentEntry?
    ): DocumentManager {
        val notebookCellUri = NotebookCellUri.parse(uri)
        val rootKey = notebookCellUri?.notebookUri ?: "file:///"
        val innerKey = notebookCellUri?.cellId ?: uri

        val contents = state.documentContents.toMutableMap()
        val documentMap = contents[rootKey]?.toMutableMap() ?: mutableMapOf()
        val currentEntry = documentMap[innerKey]
        val updatedEntry = transform(currentEntry)

        if (updatedEntry == null) {
            documentMap.remove(innerKey)
        } else {
            documentMap[innerKey] = updatedEntry
        }

        if (documentMap.isEmpty()) {
            contents.remove(rootKey)
        } else {
            contents[rootKey] = documentMap.toMap()
        }

        return DocumentManager(DocumentState(contents.toMap()))
    }

    private fun findEntry(uri: String): DocumentEntry? {
        val notebookCellUri = NotebookCellUri.parse(uri)
        return if (notebookCellUri != null) {
            state.documentContents[notebookCellUri.notebookUri]?.get(notebookCellUri.cellId)
        } else {
            state.documentContents["file:///"]?.get(uri)
        }
    }

    fun getDocument(uri: String): String? = findEntry(uri)?.text

    fun getSnapshot(uri: String): DocumentSnapshot? {
        val entry = findEntry(uri) ?: return null
        return DocumentSnapshot(
            text = entry.text,
            version = entry.version,
            diagnostics = entry.diagnostics,
            astInfo = entry.astInfo
        )
    }

    fun getAstInfo(uri: String): AstInfo? = findEntry(uri)?.astInfo

    fun getDiagnostics(uri: String): List<Diagnostic>? = findEntry(uri)?.diagnostics

    fun setDocument(uri: String, text: String, version: Int? = null): DocumentManager {
        return updateEntry(uri) { previous ->
            val nextVersion = version ?: ((previous?.version ?: -1) + 1)
            DocumentEntry(
                text = text,
                version = nextVersion
            )
        }
    }

    fun updateAnalysis(uri: String, analysis: DocumentAnalysis): DocumentManager {
        val existing = findEntry(uri) ?: return this
        val mergedEntry = existing.copy(
            diagnostics = analysis.diagnostics ?: existing.diagnostics,
            astInfo = analysis.astInfo ?: existing.astInfo
        )
        return updateEntry(uri) { mergedEntry }
    }

    fun getAllDocuments(): Map<String, Map<String, DocumentEntry>> {
        return state.documentContents
    }
}
