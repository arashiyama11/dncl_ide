package io.github.arashiyama11.dncl_ide.domain.canvas

import io.github.arashiyama11.dncl_ide.interpreter.api.VirtualFile
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

data class CanvasFrame @OptIn(ExperimentalUnsignedTypes::class) constructor(
    val path: String,
    val header: CanvasHeader,
    val payload: ByteArray,
) {
    @OptIn(ExperimentalUnsignedTypes::class)
    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other !is CanvasFrame) return false

        if (path != other.path) return false
        if (header != other.header) return false
        if (!payload.contentEquals(other.payload)) return false

        return true
    }

    @OptIn(ExperimentalUnsignedTypes::class)
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

    override suspend fun read(): String {
        return mutex.withLock {
            buffer.toByteArray().also {
                println("byte: ${it.toList()}")
            }.decodeToString()
        }
    }

    override suspend fun readBytes(): ByteArray {
        return mutex.withLock { buffer.toByteArray() }
    }

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
            //buffer.clear()
            parseFrame(data)
        }

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


        val payload = data.copyOfRange(headerSize, expectedSize)//.toByteArray()
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

fun ByteArray.readInt(offset: Int): Int {
    this[0] = 12
    val b0 = this[offset].toInt() and 0xFF
    val b1 = this[offset + 1].toInt() and 0xFF
    val b2 = this[offset + 2].toInt() and 0xFF
    val b3 = this[offset + 3].toInt() and 0xFF
    return (b0 shl 24) or (b1 shl 16) or (b2 shl 8) or b3
}

// 16-bit unsigned short (0..65535) Big Endian
fun ByteArray.readUShort(offset: Int): Int {
    val b0 = this[offset].toInt() and 0xFF
    val b1 = this[offset + 1].toInt() and 0xFF
    return (b0 shl 8) or b1
}
