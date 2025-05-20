package app.game.editor

import Styles
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import components.IconButton
import ellipsize
import format3Decimals
import game.Game
import org.jetbrains.compose.web.attributes.placeholder
import org.jetbrains.compose.web.css.AlignItems
import org.jetbrains.compose.web.css.DisplayStyle
import org.jetbrains.compose.web.css.FlexDirection
import org.jetbrains.compose.web.css.JustifyContent
import org.jetbrains.compose.web.css.alignItems
import org.jetbrains.compose.web.css.display
import org.jetbrains.compose.web.css.flexDirection
import org.jetbrains.compose.web.css.gap
import org.jetbrains.compose.web.css.justifyContent
import org.jetbrains.compose.web.css.marginBottom
import org.jetbrains.compose.web.css.maxWidth
import org.jetbrains.compose.web.css.overflow
import org.jetbrains.compose.web.css.padding
import org.jetbrains.compose.web.css.percent
import org.jetbrains.compose.web.css.width
import org.jetbrains.compose.web.dom.Button
import org.jetbrains.compose.web.dom.Div
import org.jetbrains.compose.web.dom.Text
import org.jetbrains.compose.web.dom.TextInput
import r
import kotlin.math.round

/**
 * Section for adding and managing game captions (text overlays) attached to the timeline.
 */
@Composable
fun CaptionsSection(game: Game?) {
    PanelSection(
        title = "Captions",
        icon = "subtitles",
        enabled = game != null,
        initiallyExpanded = false,
        closeOtherPanels = true
    ) {
        if (game == null) {
            Text("No game loaded")
            return@PanelSection
        }

        // Add new caption
        var newCaptionText by remember { mutableStateOf("") }
        var newCaptionDuration by remember { mutableStateOf("5") }
        Div({
            style {
                display(DisplayStyle.Flex)
                gap(0.5.r)
                alignItems(AlignItems.Center)
                marginBottom(1.r)
            }
        }) {
            TextInput(newCaptionText) {
                classes(Styles.textarea)
                placeholder("Text")
                style { width(100.percent) }
                onInput { newCaptionText = it.value }
            }
            TextInput(newCaptionDuration) {
                classes(Styles.textarea)
                placeholder("Duration (seconds)")
                style { width(20.percent) }
                onInput { newCaptionDuration = it.value }
            }
            Button({
                classes(Styles.button)
                onClick {
                    if (newCaptionText.isNotBlank()) {
                        val dur = newCaptionDuration.toDoubleOrNull() ?: 5.0
                        game.animationData.addCaption(newCaptionText, dur)
                        newCaptionText = ""
                        newCaptionDuration = "5"
                    }
                }
            }) {
                Text("Add Caption")
            }
        }

        // List existing captions
        val captions = game.animationData.captions
        if (captions.isEmpty()) {
            Div { Text("No captions added yet") }
        } else {
            captions.forEach { caption ->
                var isEditing by remember { mutableStateOf(false) }
                var editText by remember(caption) { mutableStateOf(caption.text) }
                var editTime by remember(caption) { mutableStateOf(caption.time.format3Decimals()) }
                var editDuration by remember(caption) { mutableStateOf(caption.duration.format3Decimals()) }

                Div({
                    style {
                        display(DisplayStyle.Flex)
                        flexDirection(FlexDirection.Column)
                        padding(0.5.r)
                        marginBottom(0.5.r)
                        property("background-color", "rgba(0, 0, 0, 0.05)")
                        property("border-radius", "4px")
                    }
                }) {
                    if (isEditing) {
                        TextInput(editText) {
                            classes(Styles.textarea)
                            placeholder("Caption text")
                            style { width(100.percent); marginBottom(0.5.r) }
                            onInput { editText = it.value }
                        }
                        TextInput(editTime) {
                            classes(Styles.textarea)
                            placeholder("Time (seconds)")
                            style { width(100.percent); marginBottom(0.5.r) }
                            onInput { editTime = it.value }
                        }
                        TextInput(editDuration) {
                            classes(Styles.textarea)
                            placeholder("Duration (sec)")
                            style { width(100.percent); marginBottom(0.5.r) }
                            onInput { editDuration = it.value }
                        }
                        Div({
                            style {
                                display(DisplayStyle.Flex); gap(0.5.r)
                                justifyContent(JustifyContent.FlexEnd)
                            }
                        }) {
                            Button({
                                classes(Styles.textButton); onClick {
                                isEditing = false
                                editText = caption.text
                                editTime = caption.time.format3Decimals()
                                editDuration = caption.duration.format3Decimals()
                            }
                            }) { Text("Cancel") }
                            Button({
                                classes(Styles.button); onClick {
                                // Use the new updateCaption method to ensure instant UI updates
                                game.animationData.updateCaption(
                                    id = caption.id,
                                    text = editText,
                                    time = editTime.toDoubleOrNull()?.let { round(it * 1000.0) / 1000.0 },
                                    duration = editDuration.toDoubleOrNull()?.let { round(it * 1000.0) / 1000.0 }
                                )
                                // Set the time to refresh the current view
                                game.setTime(game.animationData.currentTime)
                                isEditing = false
                            }
                            }) { Text("Save") }
                        }
                    } else {
                        Div({
                            style {
                                display(DisplayStyle.Flex)
                                justifyContent(JustifyContent.SpaceBetween)
                                alignItems(AlignItems.Center)
                            }
                        }) {
                            Div({
                                style {
                                    display(DisplayStyle.Flex)
                                    flexDirection(FlexDirection.Column)
                                    overflow("hidden")
                                }
                            }) {
                                Div(
                                    {
                                        style {
                                            ellipsize()
                                            width(100.percent)
                                            maxWidth(100.percent)
                                        }
                                    }
                                ) {
                                    Text(caption.text)
                                }
                                Div{
                                    Text("${caption.time.format3Decimals()}s")
                                }
                            }
                            Div({ style { display(DisplayStyle.Flex); gap(0.5.r) } }) {
                                IconButton(name = "edit", title = "Edit caption", onClick = { isEditing = true })
                                IconButton(
                                    name = "delete",
                                    title = "Delete caption",
                                    onClick = { game.animationData.removeCaption(caption.id) })
                                IconButton(
                                    name = "play_arrow",
                                    title = "Go to caption",
                                    onClick = { game.setTime(caption.time) })
                            }
                        }
                    }
                }
            }
        }
    }
}
