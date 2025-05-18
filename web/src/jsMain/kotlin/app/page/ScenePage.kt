package app.page

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import api
import app.ailaai.api.gameScene
import app.ailaai.api.gameScenes
import app.AppStyles
import app.FullPageLayout
import app.GamePage
import app.PageTopBar
import app.components.TopBarSearch
import app.nav.SceneNav
import appString
import appText
import baseUrl
import bulletedString
import com.queatz.db.GameScene
import components.IconButton
import components.LinkifyText
import components.Loading
import kotlinx.coroutines.launch
import notBlank
import org.jetbrains.compose.web.css.AlignItems
import org.jetbrains.compose.web.css.DisplayStyle
import org.jetbrains.compose.web.css.FlexDirection
import org.jetbrains.compose.web.css.JustifyContent
import org.jetbrains.compose.web.css.alignItems
import org.jetbrains.compose.web.css.borderRadius
import org.jetbrains.compose.web.css.display
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
import org.jetbrains.compose.web.css.paddingBottom
import org.jetbrains.compose.web.css.paddingLeft
import org.jetbrains.compose.web.css.paddingRight
import org.jetbrains.compose.web.css.paddingTop
import org.jetbrains.compose.web.css.percent
import org.jetbrains.compose.web.css.px
import org.jetbrains.compose.web.css.textAlign
import org.jetbrains.compose.web.css.width
import org.jetbrains.compose.web.dom.Div
import org.jetbrains.compose.web.dom.Img
import org.jetbrains.compose.web.dom.Text
import r

@Composable
fun ScenePage(
    nav: SceneNav,
    onBackClick: () -> Unit,
    onSceneSelected: (SceneNav) -> Unit = {}
) {
    val scope = rememberCoroutineScope()

    var scene by remember {
        mutableStateOf<GameScene?>(null)
    }

    LaunchedEffect(nav) {
        scene = when (nav) {
            is SceneNav.Selected -> nav.scene
            else -> null
        }
    }

    when (nav) {
        is SceneNav.None -> {
            Div({
                style {
                    display(DisplayStyle.Flex)
                    flexDirection(FlexDirection.Column)
                    alignItems(AlignItems.Center)
                    justifyContent(JustifyContent.Center)
                    padding(1.r)
                    gap(1.r)
                    height(100.percent)
                    textAlign("center")
                }
            }) {
                Text("No scene selected")
            }
        }
        is SceneNav.Explore -> {
            var scenes by remember { mutableStateOf<List<GameScene>>(emptyList()) }
            var isLoading by remember { mutableStateOf(true) }
            var search by remember { mutableStateOf("") }

            LaunchedEffect(search) {
                api.gameScenes {
                    scenes = it.filter { scene -> 
                        scene.published == true && 
                        (search.isBlank() || scene.name?.contains(search, ignoreCase = true) == true)
                    }
                    isLoading = false
                }
            }

            FullPageLayout {
                Div({
                    style {
                        display(DisplayStyle.Flex)
                        flexDirection(FlexDirection.Column)
                        paddingLeft(1.r)
                        paddingRight(1.r)
                        paddingBottom(1.r)
                    }
                }) {
                    TopBarSearch(
                        value = search,
                        onValue = { search = it },
                        styles = {
                            margin(1.r, 0.r)
                        }
                    )

                    if (isLoading) {
                        Loading()
                    } else if (scenes.isNotEmpty()) {
                        Div({
                            style {
                                display(DisplayStyle.Flex)
                                flexDirection(FlexDirection.Column)
                            }
                        }) {
                            scenes.forEach { scene ->
                                Div({
                                    classes(AppStyles.scriptItem)

                                    style {
                                        marginBottom(1.r)
                                    }

                                    onClick {
                                        // Navigate to the selected scene
                                        onSceneSelected(SceneNav.Selected(scene))
                                    }
                                }) {
                                    // Display scene photo if it exists
                                    if (scene.photo != null) {
                                        Img(src = "$baseUrl${scene.photo!!}", attrs = {
                                            style {
                                                width(100.percent)
                                                property("aspect-ratio", "2")
                                                property("object-fit", "cover")
                                                marginBottom(.5.r)
                                                borderRadius(.5.r)
                                            }
                                        })
                                    }

                                    Div({
                                        style {
                                            fontSize(18.px)
                                            fontWeight("bold")
                                            marginBottom(.5.r)
                                        }
                                    }) {
                                        Text(scene.name?.notBlank ?: appString { newScript })
                                    }
                                    Div({
                                        style {
                                            opacity(.5)
                                        }
                                    }) {
                                        Text(
                                            bulletedString(
                                                scene.categories?.firstOrNull(),
                                                scene.url,
                                                scene.id!!
                                            )
                                        )
                                    }
                                }
                            }
                        }
                    } else if (search.isNotBlank()) {
                        Div({
                            style {
                                display(DisplayStyle.Flex)
                                alignItems(AlignItems.Center)
                                justifyContent(JustifyContent.Center)
                                opacity(.5)
                                padding(1.r)
                            }
                        }) {
                            appText { noScripts }
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
                            appText { noScripts }
                        }
                    }
                }
            }
        }
        is SceneNav.Selected -> {
            if (scene == null) {
                Div({
                    style {
                        display(DisplayStyle.Flex)
                        justifyContent(JustifyContent.Center)
                        padding(1.r)
                    }
                }) {
                    Loading()
                }
            } else {
                // Render the GamePage with the selected scene
                GamePage(
                    gameScene = scene,
                    onSceneDeleted = {
                        onSceneSelected(SceneNav.None)
                    },
                    onScenePublished = {
                        // Reload the scene via the API
                        scope.launch {
                            // Use a local copy of the scene to access its id
                            val currentScene = scene
                            if (currentScene?.id != null) {
                                api.gameScene(currentScene.id!!) { updatedScene ->
                                    scene = updatedScene
                                }
                            }
                        }
                    }
                )
            }
        }
    }
}
