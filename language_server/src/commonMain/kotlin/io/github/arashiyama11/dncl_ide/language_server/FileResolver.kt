package io.github.arashiyama11.dncl_ide.language_server

/**
 * 言語サーバーがソースを読むための最小限のファイル解決インタフェース。
 * パスは `/` 区切りの文字列で、相対・絶対どちらも受け付ける。
 */
interface FileResolver {
    /** ルートディレクトリの絶対パス（`/` 区切り）。 */
    val root: String

    /**
     * 与えられたパスの内容を返す。存在しなければ null。
     * `path` は絶対パス、または `root` 基準の相対パスのどちらでもよい。
     */
    suspend fun read(path: String): String?

    /**
     * ディレクトリ配下のエントリを返す。返すパスは `root` からの相対パス。
     * 存在しない場合やファイルの場合は空リスト。
     */
    suspend fun list(path: String): List<String>
}

