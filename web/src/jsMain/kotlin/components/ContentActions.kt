package components

import androidx.compose.runtime.Composable
import androidx.compose.runtime.rememberCoroutineScope
import app.dialog.inputDialog
import appString
import application
import com.queatz.db.StoryContent
import com.queatz.db.isPart
import kotlinx.coroutines.launch
import org.jetbrains.compose.web.css.DisplayStyle
import org.jetbrains.compose.web.css.display
import org.jetbrains.compose.web.dom.Div


suspend fun editSection(
    currentContent: List<StoryContent>,
    index: Int,
    sectionContent: StoryContent.Section,
    onUpdate: (List<StoryContent>) -> Unit
) {
    val result = inputDialog(
        title = application.appString { section },
        placeholder = "",
        confirmButton = application.appString { save },
        defaultValue = sectionContent.section
    )
    if (result != null) {
        val newContent = currentContent.toMutableList()
        newContent[index] = sectionContent.copy(section = result)
        onUpdate(newContent)
    }
}

suspend fun editText(
    currentContent: List<StoryContent>,
    index: Int,
    textContent: StoryContent.Text,
    onUpdate: (List<StoryContent>) -> Unit
) {
    val result = inputDialog(
        title = application.appString { text },
        placeholder = "",
        confirmButton = application.appString { save },
        defaultValue = textContent.text
    )
    if (result != null) {
        val newContent = currentContent.toMutableList()
        newContent[index] = textContent.copy(text = result)
        onUpdate(newContent)
    }
}

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
 * A reusable component for content actions (edit/delete) that can be used in both StoriesPage and ExplorePage.
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
    val scope = rememberCoroutineScope()

    if (isEditable && part.isPart()) {
        Div({
            style {
                display(DisplayStyle.Flex)
            }
        }) {
            // Edit section
            when (part) {
                is StoryContent.Section -> {
                    IconButton("edit", appString { edit }) {
                        scope.launch {
                            editSection(currentContent, index, part) { newContent ->
                                onContentUpdated(newContent)
                            }
                        }
                    }
                }
                // Edit text
                is StoryContent.Text -> {
                    IconButton("edit", appString { edit }) {
                        scope.launch {
                            editText(currentContent, index, part) { newContent ->
                                onContentUpdated(newContent)
                            }
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
