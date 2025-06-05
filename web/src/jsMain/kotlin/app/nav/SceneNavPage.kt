package app.nav

import Styles
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import api
import app.ailaai.api.createGameScene
import app.ailaai.api.gameScenes
import app.components.Empty
import app.components.FlexInput
import app.components.Spacer
import app.dialog.inputDialog
import app.menu.Menu
import appString
import application
import com.queatz.db.GameScene
import components.IconButton
import components.Loading
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch
import notBlank
import org.jetbrains.compose.web.css.AlignItems
import org.jetbrains.compose.web.css.DisplayStyle
import org.jetbrains.compose.web.css.FlexDirection
import org.jetbrains.compose.web.css.JustifyContent
import org.jetbrains.compose.web.css.alignItems
import org.jetbrains.compose.web.css.display
import org.jetbrains.compose.web.css.flexDirection
import org.jetbrains.compose.web.css.gap
import org.jetbrains.compose.web.css.justifyContent
import org.jetbrains.compose.web.css.marginRight
import org.jetbrains.compose.web.css.padding
import org.jetbrains.compose.web.css.percent
import org.jetbrains.compose.web.css.width
import org.jetbrains.compose.web.dom.Button
import org.jetbrains.compose.web.dom.Div
import org.jetbrains.compose.web.dom.Text
import org.w3c.dom.DOMRect
import org.w3c.dom.HTMLElement
import r
import sortedDistinct

@kotlinx.serialization.Serializable
sealed class SceneNav {
    @kotlinx.serialization.Serializable
    data object None : SceneNav()

    @kotlinx.serialization.Serializable
    data object Explore : SceneNav()

    @kotlinx.serialization.Serializable
    data class Selected(val scene: GameScene) : SceneNav()
}
@Composable
fun SceneNavPage(
    selected: SceneNav = SceneNav.None,
    onSelected: (SceneNav) -> Unit,
    onBackClick: () -> Unit,
    updates: MutableSharedFlow<GameScene> = MutableSharedFlow()
) {
    val me by application.me.collectAsState()
    val scope = rememberCoroutineScope()

    var scenes by remember {
        mutableStateOf<List<GameScene>?>(null)
    }

    var loadError by remember {
        mutableStateOf(false)
    }

    var showSearch by remember {
        mutableStateOf(false)
    }

    var searchText by remember(showSearch) {
        mutableStateOf("")
    }

    var filterMenuTarget by remember {
        mutableStateOf<DOMRect?>(null)
    }

    val categories by remember(scenes) {
        mutableStateOf(scenes?.mapNotNull { it.categories }.orEmpty().flatten().sortedDistinct())
    }

    var selectedCategory by remember {
        mutableStateOf<String?>(null)
    }

    val shownScenes = remember(scenes, searchText, selectedCategory) {
        val search = searchText.trim()
        if (search.isBlank() && selectedCategory == null) {
            scenes
        } else {
            scenes?.filter {
                (selectedCategory == null || it.categories?.contains(selectedCategory) == true) &&
                (search.isBlank() || it.name?.contains(search, true) == true)
            }
        }
    }

    suspend fun loadScenes() {
        loadError = false
        try {
            api.gameScenes {
                scenes = it
            }
        } catch (e: Exception) {
            loadError = true
        }
    }

    LaunchedEffect(me) {
        if (me != null) {
            loadScenes()
        }
    }

    LaunchedEffect(selected) {
        searchText = ""
        showSearch = false
        loadScenes()
    }

    LaunchedEffect(Unit) {
        updates.collectLatest { updatedScene ->
            // Reload the scenes list when a scene is updated (e.g., renamed)
            loadScenes()
        }
    }

    if (filterMenuTarget != null) {
        Menu(
            onDismissRequest = { filterMenuTarget = null },
            target = filterMenuTarget!!
        ) {
            categories.forEach { category ->
                item(category, icon = if (category == selectedCategory) "check" else null) {
                    selectedCategory = if (category == selectedCategory) {
                        null
                    } else {
                        category
                    }
                }
            }
        }
    }

    NavTopBar(me, "Scenes", onProfileClick = onBackClick) {
        IconButton("search", appString { search }, styles = {
        }) {
            showSearch = !showSearch
        }

        IconButton("filter_list", appString { filter }, count = selectedCategory?.let { 1 } ?: 0, styles = {
        }) {
            filterMenuTarget = if (filterMenuTarget == null) {
                (it.target as HTMLElement).getBoundingClientRect()
            } else {
                null
            }
        }

        IconButton("add", "Create Scene", styles = {
            marginRight(.5.r)
        }) {
            scope.launch {
                val result = inputDialog(
                    "Create Scene",
                    "Scene Name",
                    application.appString { create }
                )

                if (result == null) return@launch

                val newScene = GameScene(
                    name = result
                )
                api.createGameScene(
                    gameScene = newScene,
                    onSuccess = { createdScene ->
                        loadScenes()
                        createdScene.id?.let { _ ->
                            onSelected(SceneNav.Selected(createdScene))
                        }
                    }
                )
            }
        }
    }

    if (showSearch) {
        FlexInput(
            value = searchText,
            onChange = { searchText = it },
            defaultMargins = true,
            autoFocus = true,
            onDismissRequest = {
                searchText = ""
                showSearch = false
            }
        )
    }

    NavMenu {
        if (!showSearch && selectedCategory == null) {
            NavMenuItem(
                icon = "explore",
                title = "Explore",
                selected = selected is SceneNav.Explore
            ) {
                onSelected(SceneNav.Explore)
            }

            Spacer()
        }

        if (loadError) {
            Div({
                style {
                    display(DisplayStyle.Flex)
                    justifyContent(JustifyContent.Center)
                    padding(1.r)
                }
            }) {
                Empty {
                    Text("Failed to load.")
                }
            }
        } else if (scenes == null) {
            Div({
                style {
                    display(DisplayStyle.Flex)
                    justifyContent(JustifyContent.Center)
                    padding(1.r)
                }
            }) {
                Loading()
            }
        } else if (shownScenes?.isEmpty() == true) {
            Div({
                style {
                    display(DisplayStyle.Flex)
                    flexDirection(FlexDirection.Column)
                    alignItems(AlignItems.Center)
                    gap(1.r)
                    padding(1.r)
                }
            }) {
                if (searchText.isNotBlank() || selectedCategory != null) {
                    Text("No scenes found matching your criteria")
                } else {
                    Text("No scenes found")
                    Button({
                        classes(Styles.button)
                        onClick {
                            scope.launch {
                                val newScene = GameScene(
                                    name = "Create a Scene"
                                )
                                api.createGameScene(
                                    gameScene = newScene,
                                    onSuccess = { createdScene ->
                                        loadScenes()
                                        createdScene.id?.let { _ ->
                                            onSelected(SceneNav.Selected(createdScene))
                                        }
                                    }
                                )
                            }
                        }
                    }) {
                        Text("Create a scene")
                    }
                }
            }
        } else {
            shownScenes?.forEach { scene ->
                NavMenuItem(
                    icon = "landscape", 
                    title = scene.name?.notBlank ?: "New scene",
                    selected = (selected as? SceneNav.Selected)?.scene?.id == scene.id
                ) {
                    onSelected(SceneNav.Selected(scene))
                }
            }
        }
    }
}
