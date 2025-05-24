package app.game.editor

import Styles
import androidx.compose.runtime.*
import components.Icon
// import com.queatz.db.SketchLayerData (not used)
import game.Map
import notBlank
import org.jetbrains.compose.web.attributes.autoFocus
import org.jetbrains.compose.web.css.*
import org.jetbrains.compose.web.dom.*
import r

/**
 * Panel for managing sketch layers: visibility, selection, renaming.
 */
@Composable
fun SketchLayersSection(map: Map) {
    PanelSection(
        title = "Sketch Layers",
        icon = "layers",
        initiallyExpanded = false,
        closeOtherPanels = true
    ) {
        val layers = map.sketchManager.layers
        // Button to add a new layer
        Button({
            classes(Styles.button)
            onClick {
                map.sketchManager.createLayer()
            }
            style { marginBottom(0.5.r) }
        }) {
            Text("Add Layer")
        }
        // List existing layers
        Div({ style { display(DisplayStyle.Flex); flexDirection(FlexDirection.Column); gap(0.5.r) } }) {
            layers.forEach { layer ->
                // Entire row clickable to select layer
                Div({
                    style {
                        display(DisplayStyle.Flex)
                        alignItems(AlignItems.Center)
                        gap(0.5.r)
                        padding(1.r)
                        borderRadius(0.5.r)
                        boxSizing("border-box")
                        if (map.sketchManager.currentLayer.id == layer.id) {
                            backgroundColor(Styles.colors.primary)
                        }
                        cursor("pointer")
                    }
                    onClick { map.sketchManager.selectLayer(layer.id) }
                }) {
                    // Visibility toggle
                    val eyeIcon = if (layer.visible) "visibility" else "visibility_off"
                    Icon(
                        if (layer.visible) "visibility" else "visibility_off",
                        onClick = {
                            map.sketchManager.toggleVisibility(layer.id)
                        },
                        styles = {
                            if (!layer.visible) {
                                opacity(.5f)
                            }
                        }
                    )

                    // Glow toggle
                    Icon(
                        "light_mode",
                        onClick = {
                            map.sketchManager.toggleGlow(layer.id)
                        },
                        styles = {
                            if (!layer.glow) {
                                opacity(.5f)
                            }
                        }
                    )
                    // Layer name or rename input
                    var editing by remember { mutableStateOf(false) }
                    if (editing) {
                        var name by remember { mutableStateOf(layer.name) }
                        TextInput(
                            value = name,
                            attrs = {
                                classes(Styles.textarea)
                                onInput { e -> name = e.value }
                                onKeyDown { e ->
                                    when (e.key) {
                                        "Enter" -> {
                                            map.sketchManager.selectLayer(layer.id)
                                            layer.name = name
                                            editing = false
                                            e.stopPropagation()
                                        }

                                        "Escape", "Esc" -> {
                                            editing = false
                                            e.stopPropagation()
                                        }
                                    }
                                }
                                onBlur {
                                    map.sketchManager.selectLayer(layer.id)
                                    layer.name = name
                                    editing = false
                                }

                                autoFocus()
                            }
                        )
                    } else {
                        Span({ style { flexGrow(1.0) } }) { Text(layer.name) }
                        // Rename icon
                        Icon(
                            name = "edit",
                            onClick = { editing = true }
                        )
                        // Delete icon
                        if (layers.size > 1) {
                            Icon(
                                name = "delete",
                                onClick = { map.sketchManager.removeLayer(layer.id) },
                                styles = { color(Styles.colors.red) }
                            )
                        }
                    }
                }
            }
        }
    }
}
