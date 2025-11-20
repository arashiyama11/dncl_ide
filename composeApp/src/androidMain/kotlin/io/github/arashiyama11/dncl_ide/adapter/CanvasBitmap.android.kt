package io.github.arashiyama11.dncl_ide.adapter

import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asImageBitmap
import io.github.arashiyama11.dncl_ide.domain.canvas.CanvasFrame
import androidx.core.graphics.createBitmap

actual fun CanvasFrame.toImageBitmap(): ImageBitmap? {
    if (header.width <= 0 || header.height <= 0) return null
    if (header.colorSpace != 0) return null // いまは RGBA8888 のみ対応

    val pixelCount = header.width * header.height
    val requiredBytes = pixelCount * 4
    if (payload.size < requiredBytes) return null

    val argb = IntArray(pixelCount)
    var src = 0
    for (i in 0 until pixelCount) {
        val r = payload[src++].toInt() and 0xFF
        val g = payload[src++].toInt() and 0xFF
        val b = payload[src++].toInt() and 0xFF
        val a = payload[src++].toInt() and 0xFF
        argb[i] = (a shl 24) or (r shl 16) or (g shl 8) or b
    }

    val bitmap = createBitmap(header.width, header.height)
    bitmap.setPixels(argb, 0, header.width, 0, 0, header.width, header.height)
    return bitmap.asImageBitmap()
}
