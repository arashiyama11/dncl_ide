package io.github.arashiyama11.dncl_ide.language_server

import io.github.arashiyama11.dncl_ide.domain.model.EntryPath
import io.github.arashiyama11.dncl_ide.domain.model.FileName
import io.github.arashiyama11.dncl_ide.domain.model.Folder
import io.github.arashiyama11.dncl_ide.domain.model.NotebookFile
import io.github.arashiyama11.dncl_ide.domain.model.ProgramFile
import io.github.arashiyama11.dncl_ide.domain.repository.FileRepository

/**
 * FileRepository を委譲し、文字列パスで扱える FileResolver 実装。
 * - 相対パスは root 配下として解決
 * - 絶対パスも受け付け
 * - "stdlib/..." または "./stdlib/..." は仮想 stdlib を優先
 */
class FileResolverImpl(private val fileRepository: FileRepository) : FileResolver {
    override val root: String = normalize(fileRepository.rootPath.toString())

    override suspend fun read(path: String): String? {
        val entryPath = path.toEntryPath()
        return when (val entry = fileRepository.getEntryByPath(entryPath)) {
            is ProgramFile -> fileRepository.getFileContent(entry).value
            is NotebookFile -> fileRepository.getNotebookFileContent(entry).value
            else -> null
        }
    }

    override suspend fun list(path: String): List<String> {
        val entryPath = path.toEntryPath()
        val folder = when (val entry = fileRepository.getEntryByPath(entryPath)) {
            is Folder -> entry
            else -> return emptyList()
        }

        val base = entryPath.value
        return folder.entities.map { child ->
            (entryPath + child.name).toString()
                .removePrefix(rootPrefix())
                .removePrefix("/")
        }
    }

    private fun rootPrefix(): String = root.removeSuffix("/") + "/"

    private fun String.toEntryPath(): EntryPath {
        val normalized = normalize(this)
        val raw = when {
            normalized.startsWith(rootPrefix()) -> normalized
            normalized.startsWith("/") -> normalized
            normalized.startsWith("./") -> "$root/${normalized.removePrefix("./")}"
            else -> "$root/$normalized"
        }

        // stdlib プレフィックスをそのまま維持するため、余計な正規化はしない
        return EntryPath.fromString(raw)
    }

    private fun normalize(path: String): String {
        return path.replace('\\', '/').trimEnd('/')
    }
}

