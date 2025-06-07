package app.widget.space

import Styles
import app.widget.DrawInfo
import baseUrl
import com.queatz.db.Card
import com.queatz.widgets.widgets.SpaceContent
import com.queatz.widgets.widgets.SpaceItem
import notBlank
import org.w3c.dom.CENTER
import org.w3c.dom.CanvasRenderingContext2D
import org.w3c.dom.CanvasTextAlign
import org.w3c.dom.CanvasTextBaseline
import org.w3c.dom.HTMLImageElement
import org.w3c.dom.MIDDLE
import org.w3c.dom.START
import kotlin.math.PI
import kotlin.math.absoluteValue

fun drawCanvas(
    context: CanvasRenderingContext2D,
    offset: Pair<Double, Double>,
    cardId: String?,
    cardsById: Map<String, Card>,
    items: List<SpaceItem>,
    selectedItem: SpaceItem?,
    darkMode: Boolean,
    drawInfo: DrawInfo?,
    mousePosition: Pair<Double, Double>? = null,
    isItemVisible: (SpaceItem) -> Boolean,
    itemVisibility: Map<String, Boolean>
) {
    with(context) {
        clearRect(
            x = 0.0,
            y = 0.0,
            w = canvas.width.toDouble(),
            h = canvas.height.toDouble()
        )

        // Apply the offset transformation
        save()
        translate(offset.first, offset.second)

        // Draw the pages
        items.filter { isItemVisible(it) }.forEachIndexed { index, (item, position) ->
            when (item) {
                is SpaceContent.Line -> {
                    if (item.page == null || item.page == cardId) {
                        save()
                        lineWidth = if (selectedItem?.content == item) 3.0 else 1.0
                        strokeStyle = Styles.colors.gray
                        beginPath()
                        moveTo(position.first, position.second)
                        lineTo(item.to.first, item.to.second)
                        stroke()

                        // Check if mouse is hovering over this line
                        val isHovering = mousePosition?.let { (mouseX, mouseY) ->
                            // Simple line hit detection - check if mouse is close to the line
                            val x1 = position.first
                            val y1 = position.second
                            val x2 = item.to.first
                            val y2 = item.to.second

                            // Calculate distance from point to line segment
                            val lineLength = kotlin.math.sqrt((x2 - x1) * (x2 - x1) + (y2 - y1) * (y2 - y1))
                            if (lineLength == 0.0) return@let false // Line is a point

                            val t = ((mouseX - x1) * (x2 - x1) + (mouseY - y1) * (y2 - y1)) / (lineLength * lineLength)
                            val t_clamped = kotlin.math.max(0.0, kotlin.math.min(1.0, t))

                            val closestX = x1 + t_clamped * (x2 - x1)
                            val closestY = y1 + t_clamped * (y2 - y1)

                            val distance = kotlin.math.sqrt((mouseX - closestX) * (mouseX - closestX) + (mouseY - closestY) * (mouseY - closestY))
                            distance < 10.0 // Consider hovering if within 10 pixels of the line
                        } ?: false

                        // Draw resize handles if selected or hovering
                        if (selectedItem?.content == item || isHovering) {
                            if (selectedItem?.content == item) {
                                fillStyle = if (darkMode) Styles.colors.white else Styles.colors.primary
                            } else {
                                // Set opacity to 0.25 for hover state
                                globalAlpha = 0.25
                                fillStyle = if (darkMode) Styles.colors.white else Styles.colors.primary
                            }

                            // Draw start point handle
                            beginPath()
                            arc(position.first, position.second, 5.0, 0.0, 2 * PI)
                            fill()

                            // Draw end point handle
                            beginPath()
                            arc(item.to.first, item.to.second, 5.0, 0.0, 2 * PI)
                            fill()

                            // Reset opacity
                            globalAlpha = 1.0
                        }

                        restore()
                    }
                }

                is SpaceContent.Box -> {
                    if (item.page == null || item.page == cardId) {
                        save()
                        lineWidth = if (selectedItem?.content == item) 3.0 else 1.0
                        strokeStyle = Styles.colors.gray
                        beginPath()

                        roundedRect(
                            x1 = position.first,
                            y1 = position.second,
                            x2 = item.to.first,
                            y2 = item.to.second
                        )

                        stroke()

                        // Check if mouse is hovering over this box
                        val isHovering = mousePosition?.let { (mouseX, mouseY) ->
                            // Simple box hit detection - check if mouse is inside the box
                            val minX = kotlin.math.min(position.first, item.to.first)
                            val maxX = kotlin.math.max(position.first, item.to.first)
                            val minY = kotlin.math.min(position.second, item.to.second)
                            val maxY = kotlin.math.max(position.second, item.to.second)

                            mouseX >= minX && mouseX <= maxX && mouseY >= minY && mouseY <= maxY
                        } ?: false

                        // Draw resize handles if selected or hovering
                        if (selectedItem?.content == item || isHovering) {
                            if (selectedItem?.content == item) {
                                fillStyle = if (darkMode) Styles.colors.white else Styles.colors.primary
                            } else {
                                // Set opacity to 0.25 for hover state
                                globalAlpha = 0.25
                                fillStyle = if (darkMode) Styles.colors.white else Styles.colors.primary
                            }

                            // Draw top-left handle
                            beginPath()
                            arc(position.first, position.second, 5.0, 0.0, 2 * PI)
                            fill()

                            // Draw top-right handle
                            beginPath()
                            arc(item.to.first, position.second, 5.0, 0.0, 2 * PI)
                            fill()

                            // Draw bottom-left handle
                            beginPath()
                            arc(position.first, item.to.second, 5.0, 0.0, 2 * PI)
                            fill()

                            // Draw bottom-right handle
                            beginPath()
                            arc(item.to.first, item.to.second, 5.0, 0.0, 2 * PI)
                            fill()

                            // Reset opacity
                            globalAlpha = 1.0
                        }

                        restore()
                    }
                }

                is SpaceContent.Circle -> {
                    if (item.page == null || item.page == cardId) {
                        save()
                        lineWidth = if (selectedItem?.content == item) 3.0 else 1.0
                        strokeStyle = Styles.colors.gray
                        beginPath()
                        val radiusX = (item.to.first - position.first).absoluteValue
                        val radiusY = (item.to.second - position.second).absoluteValue
                        ellipse(
                            x = position.first,
                            y = position.second,
                            radiusX = radiusX,
                            radiusY = radiusY,
                            rotation = 0.0,
                            startAngle = 0.0,
                            endAngle = 360.0
                        )
                        stroke()

                        // Check if mouse is hovering over this circle
                        val isHovering = mousePosition?.let { (mouseX, mouseY) ->
                            // Calculate distance from mouse to center of circle
                            val dx = mouseX - position.first
                            val dy = mouseY - position.second

                            // Check if point is inside ellipse using the ellipse equation
                            // (x/a)² + (y/b)² <= 1
                            if (radiusX > 0 && radiusY > 0) {
                                (dx * dx) / (radiusX * radiusX) + (dy * dy) / (radiusY * radiusY) <= 1.0
                            } else {
                                false
                            }
                        } ?: false

                        // Draw resize handles if selected or hovering
                        if (selectedItem?.content == item || isHovering) {
                            if (selectedItem?.content == item) {
                                fillStyle = if (darkMode) Styles.colors.white else Styles.colors.primary
                            } else {
                                // Set opacity to 0.25 for hover state
                                globalAlpha = 0.25
                                fillStyle = if (darkMode) Styles.colors.white else Styles.colors.primary
                            }

                            // Draw center handle
                            beginPath()
                            arc(position.first, position.second, 5.0, 0.0, 2 * PI)
                            fill()

                            // Draw right handle
                            beginPath()
                            arc(position.first + radiusX, position.second, 5.0, 0.0, 2 * PI)
                            fill()

                            // Draw left handle
                            beginPath()
                            arc(position.first - radiusX, position.second, 5.0, 0.0, 2 * PI)
                            fill()

                            // Draw bottom handle
                            beginPath()
                            arc(position.first, position.second + radiusY, 5.0, 0.0, 2 * PI)
                            fill()

                            // Draw top handle
                            beginPath()
                            arc(position.first, position.second - radiusY, 5.0, 0.0, 2 * PI)
                            fill()

                            // Reset opacity
                            globalAlpha = 1.0
                        }

                        restore()
                    }
                }

                is SpaceContent.Scribble -> {
                    if (item.page == null || item.page == cardId) {
                        save()
                        lineWidth = if (selectedItem?.content == item) 3.0 else 1.0
                        strokeStyle = Styles.colors.gray

                        // Draw the scribble line
                        beginPath()
                        if (item.points.isNotEmpty()) {
                            val firstPoint = item.points.first()
                            moveTo(firstPoint.first, firstPoint.second)

                            // Draw lines connecting all points
                            for (i in 1 until item.points.size) {
                                val point = item.points[i]
                                lineTo(point.first, point.second)
                            }
                        }
                        stroke()

                        // Check if mouse is hovering over this scribble's bounding box
                        val isHovering = mousePosition?.let { (mouseX, mouseY) ->
                            // For scribble, we'll use the bounding box for hover detection
                            // A more precise detection would check proximity to the actual line segments
                            val minX = kotlin.math.min(position.first, item.to.first)
                            val maxX = kotlin.math.max(position.first, item.to.first)
                            val minY = kotlin.math.min(position.second, item.to.second)
                            val maxY = kotlin.math.max(position.second, item.to.second)

                            mouseX >= minX && mouseX <= maxX && mouseY >= minY && mouseY <= maxY
                        } ?: false

                        // Draw resize handles if selected or hovering
                        if (selectedItem?.content == item || isHovering) {
                            if (selectedItem?.content == item) {
                                fillStyle = if (darkMode) Styles.colors.white else Styles.colors.primary
                            } else {
                                // Set opacity to 0.25 for hover state
                                globalAlpha = 0.25
                                fillStyle = if (darkMode) Styles.colors.white else Styles.colors.primary
                            }

                            // Draw top-left handle (position)
                            beginPath()
                            arc(position.first, position.second, 5.0, 0.0, 2 * PI)
                            fill()

                            // Draw top-right handle
                            beginPath()
                            arc(item.to.first, position.second, 5.0, 0.0, 2 * PI)
                            fill()

                            // Draw bottom-left handle
                            beginPath()
                            arc(position.first, item.to.second, 5.0, 0.0, 2 * PI)
                            fill()

                            // Draw bottom-right handle (to)
                            beginPath()
                            arc(item.to.first, item.to.second, 5.0, 0.0, 2 * PI)
                            fill()

                            // Reset opacity
                            globalAlpha = 1.0
                        }

                        restore()
                    }
                }

                is SpaceContent.Photo -> {
                    if (item.page == null || item.page == cardId) {
                        save()

                        // Draw the photo
                        val image = js("new Image()") as HTMLImageElement
                        image.src = "$baseUrl${item.photo}"

                        // Calculate dimensions based on the original image size or default to a reasonable size
                        val width = item.width?.toDouble() ?: 200.0
                        val height = item.height?.toDouble() ?: 150.0

                        // Calculate the scale to fit within the bounds defined by position and to
                        val scaleX = (item.to.first - position.first).absoluteValue / width
                        val scaleY = (item.to.second - position.second).absoluteValue / height

                        // Draw the image with rounded corners
                        save()
                        beginPath()
                        roundedRect(
                            x1 = position.first,
                            y1 = position.second,
                            x2 = position.first + width * scaleX,
                            y2 = position.second + height * scaleY
                        )
                        clip()
                        drawImage(
                            image = image,
                            dx = position.first,
                            dy = position.second,
                            dw = width * scaleX,
                            dh = height * scaleY
                        )
                        restore()

                        // Check if mouse is hovering over this photo
                        val isHovering = mousePosition?.let { (mouseX, mouseY) ->
                            // Simple box hit detection for the photo
                            val minX = position.first
                            val maxX = position.first + width * scaleX
                            val minY = position.second
                            val maxY = position.second + height * scaleY

                            mouseX >= minX && mouseX <= maxX && mouseY >= minY && mouseY <= maxY
                        } ?: false

                        // Draw selection border if selected or hovering
                        if (selectedItem?.content == item || isHovering) {
                            if (selectedItem?.content == item) {
                                lineWidth = 3.0
                                strokeStyle = if (darkMode) Styles.colors.gray else Styles.colors.primary
                            } else {
                                // Set opacity to 0.25 for hover state
                                globalAlpha = 0.25
                                lineWidth = 2.0
                                strokeStyle = if (darkMode) Styles.colors.gray else Styles.colors.primary
                            }

                            beginPath()
                            roundedRect(
                                x1 = position.first,
                                y1 = position.second,
                                x2 = position.first + width * scaleX,
                                y2 = position.second + height * scaleY
                            )
                            stroke()

                            // Draw resize handles
                            fillStyle = if (darkMode) Styles.colors.white else Styles.colors.primary

                            // Draw top-left handle
                            beginPath()
                            arc(position.first, position.second, 5.0, 0.0, 2 * PI)
                            fill()

                            // Draw top-right handle
                            beginPath()
                            arc(position.first + width * scaleX, position.second, 5.0, 0.0, 2 * PI)
                            fill()

                            // Draw bottom-left handle
                            beginPath()
                            arc(position.first, position.second + height * scaleY, 5.0, 0.0, 2 * PI)
                            fill()

                            // Draw bottom-right handle
                            beginPath()
                            arc(position.first + width * scaleX, position.second + height * scaleY, 5.0, 0.0, 2 * PI)
                            fill()

                            // Reset opacity
                            globalAlpha = 1.0
                        }

                        restore()
                    }
                }

                is SpaceContent.Text -> {
                    if (item.page == null || item.page == cardId) {
                        val fontSize = 24
                        val lineHeight = fontSize * 1.5f

                        fillStyle = if (darkMode) Styles.colors.white else Styles.colors.black
                        font = if (selectedItem?.content == item) {
                            "bold ${fontSize}px ${font.split(" ").last()}"
                        } else {
                            "${fontSize}px ${font.split(" ").last()}"
                        }

                        textAlign = CanvasTextAlign.START

                        item.text?.notBlank?.split("\n")?.forEachIndexed { index, text ->
                            fillText(text, position.first, position.second + index * lineHeight)
                        }
                    }
                }

                is SpaceContent.Page -> {
                    val card = cardsById[item.id] ?: return@forEachIndexed
                    val (x, y) = position

                    save()

                    if (card.photo != null) {
                        val image = js("new Image()") as HTMLImageElement
                        image.src = "$baseUrl${card.photo!!}"
                        beginPath()
                        arc(x, y, 24.0, 0.0, 2 * PI)
                        clip()
                        val imageAspect = image.width.toDouble() / image.height.toDouble()
                        val circleDiameter = 48.0
                        val circleRadius = circleDiameter / 2

                        if (imageAspect > 1) {
                            val scaledHeight = circleDiameter
                            val scaledWidth = circleDiameter * imageAspect
                            drawImage(
                                image = image,
                                dx = x - circleRadius - (scaledWidth - circleDiameter) / 2,
                                dy = y - circleRadius,
                                dw = scaledWidth,
                                dh = scaledHeight
                            )
                        } else {
                            val scaledWidth = circleDiameter
                            val scaledHeight = circleDiameter / imageAspect
                            drawImage(
                                image = image,
                                dx = x - circleRadius,
                                dy = y - circleRadius - (scaledHeight - circleDiameter) / 2,
                                dw = scaledWidth,
                                dh = scaledHeight
                            )
                        }
                    }

                    // Check if mouse is hovering over this page
                    val isHovering = mousePosition?.let { (mouseX, mouseY) ->
                        // Simple circle hit detection for the page
                        val dx = mouseX - x
                        val dy = mouseY - y
                        val distance = kotlin.math.sqrt(dx * dx + dy * dy)

                        distance <= 24.0 // Circle radius is 24.0
                    } ?: false

                    beginPath()
                    arc(x, y, 24.0, 0.0, 2 * PI)

                    if (selectedItem?.content == item) {
                        lineWidth = 3.0
                        strokeStyle = if (darkMode) Styles.colors.gray else Styles.colors.primary
                    } else if (isHovering) {
                        // Set opacity to 0.25 for hover state
                        globalAlpha = 0.25
                        lineWidth = 2.0
                        strokeStyle = if (darkMode) Styles.colors.gray else Styles.colors.primary
                    } else {
                        lineWidth = 1.0
                        strokeStyle = if (darkMode) Styles.colors.gray else Styles.colors.primary
                    }

                    stroke()

                    // Reset opacity
                    globalAlpha = 1.0

                    restore()

                    // Draw the page name
                    fillStyle = if (darkMode) Styles.colors.white else Styles.colors.black
                    font = "18px ${font.split(" ").last()}"
                    textAlign = CanvasTextAlign.CENTER
                    textBaseline = CanvasTextBaseline.MIDDLE

                    fillText(card.name ?: "New page", x, y - 24 - 18)
                }

                is SpaceContent.Slide -> {

                }
            }
        }

        // Draw line
        drawInfo?.let { drawInfo ->
            when (drawInfo.tool) {
                is SpaceWidgetTool.Line -> {
                    drawInfo.from?.let { from ->
                        drawInfo.to?.let { to ->
                            save()
                            lineWidth = 2.0
                            strokeStyle = if (darkMode) Styles.colors.gray else Styles.colors.primary
                            beginPath()
                            moveTo(from.first, from.second)
                            lineTo(to.first, to.second)
                            stroke()
                            restore()
                        }
                    }
                }
                is SpaceWidgetTool.Box -> {
                    drawInfo.from?.let { from ->
                        drawInfo.to?.let { to ->
                            save()
                            lineWidth = 2.0
                            strokeStyle = if (darkMode) Styles.colors.gray else Styles.colors.primary
                            beginPath()
                            roundedRect(
                                x1 = from.first,
                                y1 = from.second,
                                x2 = to.first,
                                y2 = to.second
                            )
                            stroke()
                            restore()
                        }
                    }
                }
                is SpaceWidgetTool.Circle -> {
                    drawInfo.from?.let { from ->
                        drawInfo.to?.let { to ->
                            save()
                            lineWidth = 2.0
                            strokeStyle = if (darkMode) Styles.colors.gray else Styles.colors.primary
                            beginPath()
                            ellipse(
                                x = from.first,
                                y = from.second,
                                radiusX = (to.first - from.first).absoluteValue,
                                radiusY = (to.second - from.second).absoluteValue,
                                rotation = 0.0,
                                startAngle = 0.0,
                                endAngle = 360.0
                            )
                            stroke()
                            restore()
                        }
                    }
                }
                is SpaceWidgetTool.Scribble -> {
                    // For Scribble, we need to draw all the points that have been collected
                    val points = drawInfo.points

                    if (points.isNotEmpty()) {
                        save()
                        lineWidth = 2.0
                        strokeStyle = if (darkMode) Styles.colors.gray else Styles.colors.primary
                        beginPath()

                        val firstPoint = points.first()
                        moveTo(firstPoint.first, firstPoint.second)

                        for (i in 1 until points.size) {
                            val point = points[i]
                            lineTo(point.first, point.second)
                        }

                        stroke()
                        restore()
                    } else {
                        // No points to draw
                        Unit
                    }
                }
                else -> Unit
            }
        }

        restore()
    }
}

data class SpacePathItem(
    val id: String,
    val card: Card,
    val offset: Pair<Double, Double> = 0.0 to 0.0,
    val selectedItem: SpaceItem? = null,
)
