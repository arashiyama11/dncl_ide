package io.github.arashiyama11.dncl_ide.interpreter.api

import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock

/**
 * 仮想ファイルを表現する共通インターフェース。
 * 標準入出力を含むすべてのストリームを統一的に扱うために導入した。
 */
interface VirtualFile {
    val path: String

    /**
     * ファイルにデータを書き込む。標準出力では追記扱いとなる。
     */
    suspend fun write(text: String)

    /**
     * ファイル内容を文字列として取得する。対応しない場合は例外を投げる。
     */
    suspend fun read(): String = throw UnsupportedOperationException("read is not supported for $path")

    /**
     * バッファをフラッシュする。未対応の場合は何もしない。
     */
    suspend fun flush() {}

    /**
     * 内容を消去する。未対応の場合は例外を投げる。
     */
    suspend fun clear(): Unit = throw UnsupportedOperationException("clear is not supported for $path")

    /**
     * フレーム単位でのコミット。標準出力向けの特殊操作。未対応の場合は何もしない。
     */
    suspend fun commitFrame() {}

    /**
     * 内容を差し替える。未対応の場合は clear/write を使ったデフォルト実装。
     */
    suspend fun replace(text: String) {
        clear()
        write(text)
        flush()
    }

    /**
     * ファイルをクローズする。必要なリソースがあればここで解放する。
     */
    suspend fun close() {}
}

/**
 * VirtualFile へのハンドル。DNCL言語上ではこれを File オブジェクトとして expose する。
 */
class VirtualFileHandle internal constructor(
    private val file: VirtualFile,
    private val identifier: Int
) {
    val path: String get() = file.path

    suspend fun write(text: String) = file.write(text)
    suspend fun read(): String = file.read()
    suspend fun flush() = file.flush()
    suspend fun clear() = file.clear()
    suspend fun commitFrame() = file.commitFrame()
    suspend fun replace(text: String) = file.replace(text)
    suspend fun close() = file.close()

    override fun equals(other: Any?): Boolean =
        other is VirtualFileHandle && other.identifier == identifier

    override fun hashCode(): Int = identifier

    override fun toString(): String = "VirtualFileHandle(path=$path, id=$identifier)"
}

/**
 * シンプルなインメモリ仮想ファイル実装。標準ファイル以外のデフォルトとして利用する。
 */
class InMemoryVirtualFile(
    override val path: String,
    initialContent: String = ""
) : VirtualFile {
    private val buffer = StringBuilder(initialContent)
    private val mutex = Mutex()

    override suspend fun write(text: String) {
        mutex.withLock {
            buffer.append(text)
        }
    }

    override suspend fun read(): String =
        mutex.withLock { buffer.toString() }

    override suspend fun clear() {
        mutex.withLock { buffer.clear() }
    }

    override suspend fun replace(text: String) {
        mutex.withLock {
            buffer.clear()
            buffer.append(text)
        }
    }
}

/**
 * 旧来の Stdout API から VirtualFile への変換ヘルパ。
 */
fun Stdout.asVirtualFile(path: String = StandardVirtualFile.Stdout.path): VirtualFile =
    object : VirtualFile {
        override val path: String = path

        override suspend fun write(text: String) = append(text)
        override suspend fun flush() = this@asVirtualFile.flush()
        override suspend fun clear() = this@asVirtualFile.clear()
        override suspend fun commitFrame() = this@asVirtualFile.commitFrame()
        override suspend fun replace(text: String) = this@asVirtualFile.replace(text)
    }

/**
 * 仮想ファイルを収容・管理するファイルシステム。必要に応じて新規ファイルを生成する。
 */
class VirtualFileSystem(
    private val defaultFileFactory: (String) -> VirtualFile = { path -> InMemoryVirtualFile(path) }
) {
    private val files = mutableMapOf<String, VirtualFile>()
    private var handleCounter = 0

    fun register(file: VirtualFile) {
        files[file.path] = file
    }

    fun register(path: String, file: VirtualFile) {
        files[path] = file
    }

    fun open(path: String, createIfMissing: Boolean = false): VirtualFileHandle? {
        val file = files[path] ?: if (createIfMissing) {
            defaultFileFactory(path).also { files[path] = it }
        } else {
            null
        }

        return file?.let { VirtualFileHandle(it, nextHandleId()) }
    }

    fun openOrCreate(path: String): VirtualFileHandle =
        open(path, createIfMissing = true)!!

    fun require(path: String): VirtualFileHandle =
        open(path) ?: throw IllegalArgumentException("$path is not registered in VirtualFileSystem")

    fun listFiles(): Set<String> = files.keys.toSet()

    private fun nextHandleId(): Int {
        handleCounter += 1
        return handleCounter
    }
}

/**
 * 標準ストリームのパスをまとめた定数。
 */
enum class StandardVirtualFile(val path: String) {
    Stdout("/dev/stdout"),
    Stderr("/dev/stderr"),
    Stdin("/dev/stdin");

    companion object {
        fun fromPath(path: String): StandardVirtualFile? =
            entries.firstOrNull { it.path == path }
    }
}
