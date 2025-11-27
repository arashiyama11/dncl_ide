package io.github.arashiyama11.dncl_ide.domain.canvas

import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertContentEquals
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertNotNull

class CanvasVirtualFileTest {

    val data = byteArrayOf(
        68,
        67,
        78,
        67,
        0,
        1,
        0,
        0,
        0,
        0,
        0,
        4,
        0,
        0,
        0,
        4,
        0,
        0,
        0,
        0,
        0,
        0,
        0,
        0,
        0,
        0,
        0,
        0,
        0,
        0,
        0,
        64,
        0,
        0,
        0,
        0,
        0,
        0,
        0,
        0,
        0,
        0,
        0,
        0,
        0,
        0,
        0,
        0,
        0,
        0,
        0,
        0,
        0,
        0,
        0,
        0,
        0,
        0,
        0,
        0,
        0,
        0,
        0,
        0,
        0,
        0,
        0,
        0,
        0,
        0,
        0,
        0,
        0,
        0,
        0,
        0,
        0,
        0,
        0,
        0,
        0,
        0,
        0,
        0,
        0,
        0,
        0,
        0,
        0,
        0,
        0,
        0,
        0,
        0,
        0,
        0
    )

    fun commitFrame_parsesHeaderAndPayload() = runTest {
        var captured: CanvasFrame? = null
        val file = CanvasVirtualFile(path = "/dev/canvas0", { frame ->
            captured = frame
        })

        val payload = byteArrayOf(1, 2, 3, 4)
        val header = ByteArray(CanvasVirtualFile.DEFAULT_HEADER_SIZE).apply {
            writeInt(0, 0xCAFE_BABE.toInt())
            writeUShort(4, 1)
            writeUShort(6, 0)
            writeInt(8, 2)
            writeInt(12, 2)
            writeInt(16, 0)
            writeInt(20, 1)
            writeInt(24, 0)
            writeInt(28, payload.size)
        }

        file.write(data)
        file.commitFrame()

        val frame = assertNotNull(captured)
        println(captured)
        assertEquals("/dev/canvas0", frame.path)
        assertEquals(1, frame.header.frameNumber)
        assertContentEquals(payload, frame.payload)
    }

    @Test
    fun commitFrame_throwsWhenHeaderMissing() = runTest {
        val file = CanvasVirtualFile(path = "/dev/canvas1", { error("should not be called") })
        val insufficient = ByteArray(CanvasVirtualFile.DEFAULT_HEADER_SIZE - 1)

        file.write(insufficient)

        assertFailsWith<IllegalArgumentException> {
            file.commitFrame()
        }
    }
}

private fun ByteArray.writeInt(offset: Int, value: Int) {
    this[offset] = ((value ushr 24) and 0xFF).toByte()
    this[offset + 1] = ((value ushr 16) and 0xFF).toByte()
    this[offset + 2] = ((value ushr 8) and 0xFF).toByte()
    this[offset + 3] = (value and 0xFF).toByte()
}

private fun ByteArray.writeUShort(offset: Int, value: Int) {
    this[offset] = ((value ushr 8) and 0xFF).toByte()
    this[offset + 1] = (value and 0xFF).toByte()
}
