package io.github.arashiyama11.dncl_ide.interpreter.api

import kotlinx.coroutines.runBlocking
import kotlin.test.Test
import kotlin.test.assertContentEquals
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class CanvasVirtualFileTest {
    @Test
    fun commitFrameEmitsParsedHeader() = runBlocking {
        val frames = mutableListOf<CanvasFrame>()
        val file = CanvasVirtualFile(path = "/dev/canvas0") { frame ->
            frames.add(frame)
        }

        val payload = byteArrayOf(1, 2, 3, 4)
        val header = buildHeader(payloadSize = payload.size, frameNumber = 7)
        file.write(header + payload)

        file.commitFrame()

        assertEquals(1, frames.size)
        val frame = frames.first()
        assertEquals("/dev/canvas0", frame.path)
        assertEquals(0x44434E43.toInt(), frame.header.magic)
        assertEquals(1, frame.header.version)
        assertEquals(512, frame.header.width)
        assertEquals(256, frame.header.height)
        assertEquals(7, frame.header.frameNumber)
        assertContentEquals(payload, frame.payload)
    }

    @Test
    fun commitFrameWithoutPayloadDoesNothing() = runBlocking {
        val frames = mutableListOf<CanvasFrame>()
        val file = CanvasVirtualFile(path = "/dev/canvas1") { frame ->
            frames.add(frame)
        }

        file.write(ByteArray(CanvasVirtualFile.DEFAULT_HEADER_SIZE - 1))
        file.commitFrame()

        assertTrue(frames.isEmpty())
    }

    private fun buildHeader(
        magic: Int = 0x44434E43.toInt(),
        version: Int = 1,
        colorSpace: Int = 0,
        width: Int = 512,
        height: Int = 256,
        backgroundColor: Int = 0,
        frameNumber: Int = 0,
        reserved: Int = 0,
        payloadSize: Int,
    ): ByteArray {
        val header = ByteArray(CanvasVirtualFile.DEFAULT_HEADER_SIZE)
        header.writeInt(0, magic)
        header.writeUShort(4, version)
        header.writeUShort(6, colorSpace)
        header.writeInt(8, width)
        header.writeInt(12, height)
        header.writeInt(16, backgroundColor)
        header.writeInt(20, frameNumber)
        header.writeInt(24, reserved)
        header.writeInt(28, payloadSize)
        return header
    }

    private fun ByteArray.writeInt(offset: Int, value: Int) {
        var remaining = value
        for (i in 3 downTo 0) {
            this[offset + i] = (remaining and 0xFF).toByte()
            remaining = remaining shr 8
        }
    }

    private fun ByteArray.writeUShort(offset: Int, value: Int) {
        var remaining = value
        for (i in 1 downTo 0) {
            this[offset + i] = (remaining and 0xFF).toByte()
            remaining = remaining shr 8
        }
    }
}
