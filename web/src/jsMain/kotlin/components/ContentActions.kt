package components

import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.web.events.SyntheticDragEvent
import app.AppStyles
import app.menu.Menu
import appString
import com.queatz.db.StoryContent
import com.queatz.db.isPart
import org.jetbrains.compose.web.attributes.Draggable
import org.jetbrains.compose.web.css.DisplayStyle
import org.jetbrains.compose.web.css.cursor
import org.jetbrains.compose.web.css.display
import org.jetbrains.compose.web.dom.Div
import org.jetbrains.compose.web.dom.Span
import org.jetbrains.compose.web.dom.Text
import org.w3c.dom.DOMRect

fun deleteContent(
    currentContent: List<StoryContent>,
    index: Int,
    onUpdate: (List<StoryContent>) -> Unit
) {
    val newContent = currentContent.toMutableList()
    newContent.removeAt(index)
    onUpdate(newContent)
}

/**
 * A reusable component for content actions (drag/edit/delete) that can be used in both StoriesPage and ExplorePage.
 *
 * @param index The index of the content part in the list
 * @param part The content part to provide actions for
 * @param isEditable Whether the content is editable by the current user
 * @param currentContent The current list of content parts
 * @param onContentUpdated Callback when content is updated
 * @param additionalActions Optional composable for additional custom actions
 */
@Composable
fun ContentActions(
    index: Int,
    part: StoryContent,
    isEditable: Boolean,
    currentContent: List<StoryContent>,
    onContentUpdated: (List<StoryContent>) -> Unit,
    additionalActions: @Composable () -> Unit = {}
) {
    var aspectMenuTarget by remember { mutableStateOf<DOMRect?>(null) }

    if (isEditable && part.isPart()) {
        aspectMenuTarget?.let { target ->
            Menu(
                onDismissRequest = { aspectMenuTarget = null },
                target = target
            ) {
                val photos = part as StoryContent.Photos
                item("None") {
                    val newContent = currentContent.toMutableList()
                    newContent[index] = photos.copy(aspect = null)
                    onContentUpdated(newContent)
                }
                item("Portrait") {
                    val newContent = currentContent.toMutableList()
                    newContent[index] = photos.copy(aspect = .75f)
                    onContentUpdated(newContent)
                }
                item("Landscape") {
                    val newContent = currentContent.toMutableList()
                    newContent[index] = photos.copy(aspect = 1.5f)
                    onContentUpdated(newContent)
                }
                item("Square") {
                    val newContent = currentContent.toMutableList()
                    newContent[index] = photos.copy(aspect = 1f)
                    onContentUpdated(newContent)
                }
            }
        }

        Div({
            style {
                display(DisplayStyle.Flex)
            }
        }) {
            // Drag handle
            Span({
                classes(AppStyles.iconButton)

                style {
                    cursor("grab")
                }

                title("Reorder")

                draggable(Draggable.True)

                onDragStart { event: SyntheticDragEvent ->
                    event.dataTransfer?.setData("text/plain", index.toString())
                }
            }) {
                Span({
                    classes("material-symbols-outlined")
                }) {
                    Text("drag_indicator")
                }
            }

            // Type-specific edit actions
            when (part) {
                is StoryContent.Photos -> {
                    IconButton("aspect_ratio", appString { edit }) { event ->
                        aspectMenuTarget = if (aspectMenuTarget == null) {
                            (event.target as? org.w3c.dom.HTMLElement)?.getBoundingClientRect()
                        } else {
                            null
                        }
                    }
                }

                else -> Unit
            }

            // Allow for additional custom actions
            additionalActions()

            // Delete part
            IconButton("delete", appString { remove }) {
                deleteContent(currentContent, index) { newContent ->
                    onContentUpdated(newContent)
                }
            }
        }
    }
}
