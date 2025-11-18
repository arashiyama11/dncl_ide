package io.github.arashiyama11.dncl_ide.interpreter.api

import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock

data class CanvasHeader(
    val magic: Int,
    val version: Int,
    val colorSpace: Int,
    val width: Int,
    val height: Int,
    val backgroundColor: Int,
    val frameNumber: Int,
    val reserved: Int,
    val payloadSize: Int,
)

data class CanvasFrame(
    val path: String,
    val header: CanvasHeader,
    val payload: ByteArray,
) {
    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other == null || this::class != other::class) return false

        other as CanvasFrame

        if (path != other.path) return false
        if (header != other.header) return false
        if (!payload.contentEquals(other.payload)) return false

        return true
    }

    override fun hashCode(): Int {
        var result = path.hashCode()
        result = 31 * result + header.hashCode()
        result = 31 * result + payload.contentHashCode()
        return result
    }
}

class CanvasVirtualFile(
    override val path: String,
    private val onFrameCommitted: suspend (CanvasFrame) -> Unit,
    private val headerSize: Int = DEFAULT_HEADER_SIZE,
) : VirtualFile {
    private val buffer = mutableListOf<Byte>()
    private val mutex = Mutex()

    override suspend fun write(text: String) {
        write(text.encodeToByteArray())
    }

    override suspend fun write(bytes: ByteArray) {
        mutex.withLock {
            bytes.forEach(buffer::add)
        }
    }

    override suspend fun clear() {
        mutex.withLock { buffer.clear() }
    }

    override suspend fun commitFrame() {
        val frame = mutex.withLock {
            if (buffer.isEmpty()) return
            val data = buffer.toByteArray()
            buffer.clear()
            parseFrame(data)
        } ?: return

        onFrameCommitted(frame)
    }

    private fun parseFrame(data: ByteArray): CanvasFrame {
        require(data.size >= headerSize) { "Canvas frame must contain header" }
        val header = CanvasHeader(
            magic = data.readInt(0),
            version = data.readUShort(4),
            colorSpace = data.readUShort(6),
            width = data.readInt(8),
            height = data.readInt(12),
            backgroundColor = data.readInt(16),
            frameNumber = data.readInt(20),
            reserved = data.readInt(24),
            payloadSize = data.readInt(28),
        )

        require(header.payloadSize >= 0) { "Payload size must be non-negative" }
        val expectedSize = headerSize + header.payloadSize
        require(data.size >= expectedSize) {
            "Frame payload is smaller than declared size: declared=${header.payloadSize}, actual=${data.size - headerSize}"
        }

        val payload = data.copyOfRange(headerSize, expectedSize)
        return CanvasFrame(path = path, header = header, payload = payload)
    }

    private fun MutableList<Byte>.toByteArray(): ByteArray {
        val result = ByteArray(size)
        indices.forEach { result[it] = this[it] }
        return result
    }

    companion object {
        const val DEFAULT_HEADER_SIZE: Int = 32
    }
}

private fun ByteArray.readInt(offset: Int): Int {
    require(offset + 4 <= size) { "Cannot read Int at offset=$offset" }
    var value = 0
    repeat(4) { index ->
        value = (value shl 8) or (this[offset + index].toInt() and 0xFF)
    }
    return value
}

private fun ByteArray.readUShort(offset: Int): Int {
    require(offset + 2 <= size) { "Cannot read UShort at offset=$offset" }
    var value = 0
    repeat(2) { index ->
        value = (value shl 8) or (this[offset + index].toInt() and 0xFF)
    }
    return value
}
