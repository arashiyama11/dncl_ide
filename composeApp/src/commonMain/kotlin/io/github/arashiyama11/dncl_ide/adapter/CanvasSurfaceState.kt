package io.github.arashiyama11.dncl_ide.adapter

import androidx.compose.ui.graphics.ImageBitmap
import io.github.arashiyama11.dncl_ide.domain.canvas.CanvasFrame

data class CanvasSurfaceState(
    val path: String,
    val frameNumber: Int,
    val width: Int,
    val height: Int,
    val colorSpace: Int,
    val backgroundColor: Int,
    val bitmap: ImageBitmap?
)

fun CanvasFrame.toSurfaceState(): CanvasSurfaceState =
    CanvasSurfaceState(
        path = path,
        frameNumber = header.frameNumber,
        width = header.width,
        height = header.height,
        colorSpace = header.colorSpace,
        backgroundColor = header.backgroundColor,
        bitmap = toImageBitmap()
    )

expect fun CanvasFrame.toImageBitmap(): ImageBitmap?
