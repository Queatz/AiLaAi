package app.dialog

import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import api
import app.AppStyles
import app.ailaai.api.gameScenes
import appString
import appText
import application
import bulletedString
import com.queatz.db.GameScene
import components.LinkifyText
import components.Loading
import components.SearchField
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch
import notBlank
import org.jetbrains.compose.web.css.AlignItems
import org.jetbrains.compose.web.css.DisplayStyle
import org.jetbrains.compose.web.css.FlexDirection
import org.jetbrains.compose.web.css.JustifyContent
import org.jetbrains.compose.web.css.alignItems
import org.jetbrains.compose.web.css.display
import org.jetbrains.compose.web.css.flex
import org.jetbrains.compose.web.css.flexDirection
import org.jetbrains.compose.web.css.fontSize
import org.jetbrains.compose.web.css.fontWeight
import org.jetbrains.compose.web.css.gap
import org.jetbrains.compose.web.css.height
import org.jetbrains.compose.web.css.justifyContent
import org.jetbrains.compose.web.css.margin
import org.jetbrains.compose.web.css.marginBottom
import org.jetbrains.compose.web.css.marginTop
import org.jetbrains.compose.web.css.opacity
import org.jetbrains.compose.web.css.overflow
import org.jetbrains.compose.web.css.padding
import org.jetbrains.compose.web.css.px
import org.jetbrains.compose.web.css.width
import org.jetbrains.compose.web.dom.Div
import org.jetbrains.compose.web.dom.Text
import r

/**
 * Dialog for selecting a scene
 */
suspend fun selectSceneDialog(
    scope: CoroutineScope,
    onSceneSelected: (sceneId: String) -> Unit
) {
    dialog(
        title = "Scenes",
        confirmButton = null,
        cancelButton = application.appString { cancel },
        content = { resolve ->
            var scenes by remember { mutableStateOf<List<GameScene>>(emptyList()) }
            var isLoading by remember { mutableStateOf(true) }
            var search by remember { mutableStateOf("") }

            fun loadScenes() {
                isLoading = true
                scope.launch {
                    api.gameScenes {
                        scenes = it.filter { scene -> 
                            // Only show published scenes or scenes created by the current user
                            scene.published == true || scene.person == application.me.value?.id
                        }
                        isLoading = false
                    }
                }
            }

            LaunchedEffect(search) {
                loadScenes()
            }

            Div({
                style {
                    display(DisplayStyle.Flex)
                    flexDirection(FlexDirection.Column)
                    gap(1.r)
                    width(32.r)
                    height(24.r)
                }
            }) {
                SearchField(
                    value = search,
                    placeholder = appString { this.search },
                    shadow = false,
                    styles = {
                        margin(.5.r)
                    },
                    onValue = {
                        search = it
                    }
                )

                if (isLoading) {
                    Loading()
                } else if (scenes.isNotEmpty()) {
                    Div({
                        style {
                            display(DisplayStyle.Flex)
                            flexDirection(FlexDirection.Column)
                            gap(.5.r)
                            padding(0.r, .5.r)
                            property("overflow-y", "auto")
                            flex(1)
                        }
                    }) {
                        // Filter scenes based on search
                        val filteredScenes = if (search.isBlank()) {
                            scenes
                        } else {
                            scenes.filter { scene ->
                                scene.name?.contains(search, ignoreCase = true) == true ||
                                scene.description?.contains(search, ignoreCase = true) == true
                            }
                        }

                        filteredScenes.forEach { scene ->
                            Div({
                                classes(AppStyles.scriptItem)

                                onClick {
                                    scene.id?.let { sceneId ->
                                        onSceneSelected(sceneId)
                                        resolve(false)
                                    }
                                }
                            }) {
                                Div({
                                    style {
                                        fontSize(18.px)
                                        fontWeight("bold")
                                        marginBottom(.5.r)
                                    }
                                }) {
                                    Text(scene.name?.notBlank ?: "Untitled Scene")
                                }
                                Div({
                                    style {
                                        opacity(.5)
                                    }
                                }) {
                                    Text(
                                        bulletedString(
                                            if (scene.published == true) "Published" else "Draft",
                                            scene.categories?.firstOrNull(),
                                            scene.id!!
                                        )
                                    )
                                }
                                scene.description?.notBlank?.let { description ->
                                    Div({
                                        style {
                                            marginTop(.5.r)
                                            overflow("auto")
                                        }
                                    }) {
                                        LinkifyText(description)
                                    }
                                }
                            }
                        }
                    }
                } else {
                    Div({
                        style {
                            display(DisplayStyle.Flex)
                            alignItems(AlignItems.Center)
                            justifyContent(JustifyContent.Center)
                            opacity(.5)
                            padding(1.r)
                        }
                    }) {
                        Text("No scenes found")
                    }
                }
            }
        }
    )
}
