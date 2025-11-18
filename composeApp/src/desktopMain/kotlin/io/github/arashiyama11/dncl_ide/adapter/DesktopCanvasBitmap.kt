package io.github.arashiyama11.dncl_ide.adapter

import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.toComposeImageBitmap
import io.github.arashiyama11.dncl_ide.interpreter.api.CanvasFrame
import org.jetbrains.skia.ColorAlphaType
import org.jetbrains.skia.ColorType
import org.jetbrains.skia.Image
import org.jetbrains.skia.ImageInfo

actual fun CanvasFrame.toImageBitmap(): ImageBitmap? {
    if (header.width <= 0 || header.height <= 0) return null
    if (header.colorSpace != 0) return null // RGBA8888 のみ対応
    return runCatching {
        val rowBytes = header.width * 4
        val imageInfo = ImageInfo(header.width, header.height, ColorType.RGBA_8888, ColorAlphaType.UNPREMUL)
        Image.makeRaster(imageInfo, payload, rowBytes).toComposeImageBitmap()
    }.getOrNull()
}
