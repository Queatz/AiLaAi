package app.widget.space

import org.w3c.dom.CanvasPath
import kotlin.math.max
import kotlin.math.min

fun CanvasPath.roundedRect(
    x1: Double,
    y1: Double,
    x2: Double,
    y2: Double
) {
    val x = min(x1, x2)
    val y = min(y1, y2)
    val width = max(x1, x2) - x
    val height = max(y1, y2) - y
    val cornerRadius = 12.0.coerceAtMost(width / 2.0).coerceAtMost(height / 2.0)

    // Top-left corner
    moveTo(x + cornerRadius, y)
    // Top edge
    lineTo(x + width - cornerRadius, y)
    // Top-right corner
    arcTo(
        x1 = x + width,
        y1 = y,
        x2 = x + width,
        y2 = y + cornerRadius,
        radiusX = cornerRadius,
        radiusY = cornerRadius,
        rotation = 0.0
    )
    // Right edge
    lineTo(x + width, y + height - cornerRadius)
    // Bottom-right corner
    arcTo(
        x1 = x + width,
        y1 = y + height,
        x2 = x + width - cornerRadius,
        y2 = y + height,
        radiusX = cornerRadius,
        radiusY = cornerRadius,
        rotation = 0.0
    )
    // Bottom edge
    lineTo(x + cornerRadius, y + height)
    // Bottom-left corner
    arcTo(
        x1 = x,
        y1 = y + height,
        x2 = x,
        y2 = y + height - cornerRadius,
        radiusX = cornerRadius,
        radiusY = cornerRadius,
        rotation = 0.0
    )
    // Left edge
    lineTo(x, y + cornerRadius)
    // Top-left corner
    arcTo(
        x1 = x,
        y1 = y,
        x2 = x + cornerRadius,
        y2 = y,
        radiusX = cornerRadius,
        radiusY = cornerRadius,
        rotation = 0.0
    )
}
