package components

import Styles
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.web.events.SyntheticDragEvent
import org.jetbrains.compose.web.attributes.Draggable
import org.jetbrains.compose.web.css.Color
import org.jetbrains.compose.web.css.LineStyle
import org.jetbrains.compose.web.css.Position
import org.jetbrains.compose.web.css.border
import org.jetbrains.compose.web.css.borderRadius
import org.jetbrains.compose.web.css.cursor
import org.jetbrains.compose.web.css.opacity
import org.jetbrains.compose.web.css.percent
import org.jetbrains.compose.web.css.position
import org.jetbrains.compose.web.css.px
import org.jetbrains.compose.web.css.width
import org.jetbrains.compose.web.dom.Div
import org.w3c.dom.HTMLElement
import web.cssom.PropertyName.Companion.borderBottomColor
import web.cssom.PropertyName.Companion.borderTopColor

enum class DragOverPosition {
    Above,
    Below
}

@Composable
fun DraggableContentItem(
    index: Int,
    editable: Boolean,
    onReorder: (fromIndex: Int, toIndex: Int) -> Unit,
    onDragStateChange: (Boolean) -> Unit = {},
    itemRenderer: @Composable () -> Unit
) {
    var isDragging by remember { mutableStateOf(false) }


    var dragOverPosition by remember { mutableStateOf<DragOverPosition?>(null) }

    Div({
        style {
            position(Position.Relative)
            width(100.percent)
            border(3.px, LineStyle.Solid, Color.transparent)
            borderRadius(4.px)
            property("transition", "all 0.1s ease")
        }

        if (editable) {
            style {
                cursor("grab")

                if (isDragging) {
                    opacity(0.25)
                }

                if (dragOverPosition == DragOverPosition.Above) {
                    property(borderTopColor.toString(), Styles.colors.primary.toString())
                } else if (dragOverPosition == DragOverPosition.Below) {
                    property(borderBottomColor.toString(), Styles.colors.primary.toString())
                }
            }
            draggable(Draggable.True)

            // Drag over - allow dropping on the container
            onDragOver { event: SyntheticDragEvent ->
                try {
                    event.preventDefault()
                    if (event.currentTarget is HTMLElement) {
                        val rect = (event.currentTarget as HTMLElement).getBoundingClientRect()
                        val midY = rect.top + rect.height / 2
                        val mouseY = event.clientY

                        dragOverPosition = if (mouseY < midY) DragOverPosition.Above else DragOverPosition.Below
                    }
                } catch (e: Exception) {
                    console.error("Drag over error:", e)
                }
            }

            // Drag leave
            onDragLeave { event: SyntheticDragEvent ->
                try {
                    dragOverPosition = null
                } catch (e: Exception) {
                    console.error("Drag leave error:", e)
                }
            }

            // Drop
            onDrop { event: SyntheticDragEvent ->
                try {
                    event.preventDefault()
                    val fromIndex = event.dataTransfer?.getData("text/plain")?.toIntOrNull()

                    if (fromIndex != null && fromIndex != index) {
                        val toIndex = if (dragOverPosition == DragOverPosition.Above) index else index + 1
                        onReorder(fromIndex, toIndex)
                    }

                    dragOverPosition = null
                    isDragging = false
                } catch (e: Exception) {
                    console.error("Drop error:", e)
                }
            }

            onDragStart { event: SyntheticDragEvent ->
                try {
                    event.dataTransfer?.setData("text/plain", index.toString())
                    isDragging = true
                    onDragStateChange(true)
                } catch (e: Exception) {
                    console.error("Drag start error:", e)
                }
            }

            onDragEnd { event: SyntheticDragEvent ->
                try {
                    isDragging = false
                    dragOverPosition = null
                    onDragStateChange(false)
                } catch (e: Exception) {
                    console.error("Drag end error:", e)
                }
            }
        }
    }) {
        itemRenderer()
    }
}
