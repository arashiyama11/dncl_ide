package io.github.arashiyama11.dncl_ide.language_server.service

import io.arashiyama11.dncl_ide.generated.DnclLibs
import io.github.arashiyama11.dncl_ide.language_server.FileResolver

/**
 * include (@{...}) 用のパスを正規化し、FileResolver + 組み込み stdlib から内容を取得する。
 */
internal suspend fun resolveLibText(
    fileResolver: FileResolver,
    rawPath: String
): String {
    val normalized = normalizeIncludePath(rawPath)

    // FileResolver（アプリ側のリポジトリや実ファイル系）を優先
    fileResolver.read(normalized)?.let { return it }
    if (normalized.startsWith("/")) {
        fileResolver.read(normalized.removePrefix("/"))?.let { return it }
    }

    // 最後に組み込み stdlib を参照
    return DnclLibs.texts[normalized]
        ?: DnclLibs.texts[normalized.removePrefix("/")]
        ?: ""
}

internal fun normalizeIncludePath(path: String): String =
    path.replace('\\', '/').removePrefix("./")

/**
 * stdlib のみを解決する簡易 FileResolver（テスト・スタンドアロン実行向け）。
 */
class StdlibOnlyFileResolver(
    override val root: String = ""
) : FileResolver {
    override suspend fun read(path: String): String? {
        val normalized = normalizeIncludePath(path).removePrefix("/")
        return DnclLibs.texts[normalized]
    }

    override suspend fun list(path: String): List<String> {
        val normalized = normalizeIncludePath(path).removePrefix("/")
        return DnclLibs.texts.keys
            .filter { it.startsWith(normalized) }
    }
}
