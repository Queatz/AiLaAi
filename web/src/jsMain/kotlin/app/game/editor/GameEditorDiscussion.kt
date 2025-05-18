package app.game.editor

import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import api
import app.AppStyles
import app.ailaai.api.commentOnGameDiscussion
import app.ailaai.api.createGameDiscussion
import app.ailaai.api.gameDiscussion
import app.ailaai.api.gameSceneDiscussions
import app.ailaai.api.resolveGameDiscussion
import app.components.EditField
import app.dialog.inputDialog
import application
import com.queatz.db.Comment
import com.queatz.db.GameDiscussion
import com.queatz.db.GameDiscussionExtended
import com.queatz.db.GameScene
import com.queatz.db.Vector3Data
import components.GroupPhoto
import components.GroupPhotoItem
import components.LinkifyText
import components.ProfilePhoto
import game.DiscussionMarkers
import game.Map
import stories.StoryComments
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch
import lib.Color3
import lib.CreateSphereOptions
import lib.Engine
import lib.KeyboardEventTypes
import lib.KeyboardInfo
import lib.Matrix
import lib.Mesh
import lib.MeshBuilder
import lib.PickingInfo
import lib.PointerEventTypes
import lib.PointerInfo
import lib.Ray
import lib.StandardMaterial
import lib.Vector3
import org.jetbrains.compose.web.css.AlignItems
import org.jetbrains.compose.web.css.backgroundColor
import org.jetbrains.compose.web.css.borderRadius
import org.jetbrains.compose.web.css.cursor
import org.jetbrains.compose.web.css.flex
import org.jetbrains.compose.web.css.marginBottom
import org.jetbrains.compose.web.css.marginLeft
import org.jetbrains.compose.web.css.opacity
import org.jetbrains.compose.web.css.padding
import org.jetbrains.compose.web.css.percent
import org.jetbrains.compose.web.css.rgba
import org.jetbrains.compose.web.css.width
import org.jetbrains.compose.web.css.StyleScope
import org.jetbrains.compose.web.css.DisplayStyle
import org.jetbrains.compose.web.css.FlexDirection
import org.jetbrains.compose.web.css.JustifyContent
import org.jetbrains.compose.web.css.alignItems
import org.jetbrains.compose.web.css.display
import org.jetbrains.compose.web.css.flexDirection
import org.jetbrains.compose.web.css.justifyContent
import org.jetbrains.compose.web.dom.Button
import org.jetbrains.compose.web.dom.Div
import org.jetbrains.compose.web.dom.Text
import r

@Composable
fun GameEditorTabDiscussion(engine: Engine, map: Map, gameScene: GameScene? = null) {
    val scope = rememberCoroutineScope()
    val me by application.me.collectAsState()
    var discussions by remember { mutableStateOf<List<GameDiscussionExtended>?>(null) }
    var isPlacingDiscussion by remember { mutableStateOf(false) }
    var selectedDiscussionId by remember { mutableStateOf<String?>(null) }
    var selectedDiscussion by remember { mutableStateOf<GameDiscussionExtended?>(null) }
    var previewMarker by remember { mutableStateOf<Mesh?>(null) }
    var discussionComment by remember { mutableStateOf("") }

    // Create a discussion markers manager
    val discussionMarkers = remember { 
        DiscussionMarkers(map.game!!.scene).apply {
            // Set up the click handler for markers
            onMarkerClicked = { clickedDiscussionId ->
                // Select this discussion
                selectedDiscussionId = clickedDiscussionId

                // Load the full discussion details
                scope.launch {
                    api.gameDiscussion(clickedDiscussionId) {
                        selectedDiscussion = it
                    }
                }

                // Focus the camera on this discussion
                focusOnMarker(clickedDiscussionId, map.camera)
            }
        }
    }

    // Function to create a discussion at the specified position
    suspend fun createDiscussion(position: Vector3, gameSceneId: String?, scope: CoroutineScope, map: Map, discussionMarkers: DiscussionMarkers, comment: String) {
        if (gameSceneId == null) return

        // Create the discussion
        api.createGameDiscussion(
            sceneId = gameSceneId,
            discussion = GameDiscussion(
                position = Vector3Data(position.x, position.y, position.z),
                comment = comment
            )
        ) { newDiscussion ->
            // Add a marker for the new discussion
            discussionMarkers.addMarker(newDiscussion.id!!, Vector3Data(position.x, position.y, position.z), comment)

            // Reload discussions
            api.gameSceneDiscussions(gameSceneId) {
                discussions = it
            }
        }
    }

    // Deselect tile and object when navigating to the Discussion tab
    LaunchedEffect(Unit) {
        // Deselect any tile or object that is currently selected
        map.setCurrentGameTile(null)
        map.setCurrentGameObject(null)
    }

    // Load discussions when the component is first rendered or when the scene changes
    LaunchedEffect(gameScene?.id) {
        if (gameScene?.id != null) {
            api.gameSceneDiscussions(gameScene.id!!) {
                discussions = it

                // Update markers in the 3D scene
                discussionMarkers.updateMarkers(it)
            }
        }
    }

    // Clean up resources when the component is unmounted
    DisposableEffect(Unit) {
        onDispose {
            // Dispose of the discussion markers and glow layer
            discussionMarkers.dispose()
        }
    }

    // Set up event listeners for placing discussions
    DisposableEffect(isPlacingDiscussion) {
        if (isPlacingDiscussion) {
            val scene = map.game?.scene ?: return@DisposableEffect onDispose {}

            // Create a preview marker (semi-transparent yellow sphere)
            val marker = MeshBuilder.CreateSphere("preview-marker", object : CreateSphereOptions {
                override val diameter = 0.5f
                override val segments = 16
            }, scene)

            // Create a yellow material with 50% alpha
            val material = StandardMaterial("preview-material", scene)
            material.diffuseColor = Color3(1f, 1f, 0f) // Yellow color
            material.specularColor = Color3(0.1f, 0.1f, 0.1f)
            material.alpha = 0.5f // 50% transparency
            marker.material = material

            previewMarker = marker

            // Function to update the preview marker position
            val updatePreviewPosition = fun() {
                val camera = scene.activeCamera ?: return

                // Create a picking ray from the current pointer position
                val ray = scene.createPickingRay(
                    scene.pointerX,
                    scene.pointerY,
                    Matrix.Identity(),
                    camera
                )

                // Intersect with the tilemap mesh
                val pickInfo = ray.intersectsMesh(map.tilemapEditor.tilemap.mesh)
                val position = pickInfo.pickedPoint

                // Update the marker position if we have a valid pick point
                if (position != null) {
                    marker.position = position
                }
            }

            // Register pointer move observer to update preview position
            val pointerMoveObserver = scene.onPointerObservable.add(fun(info: PointerInfo) {
                if (info.type == PointerEventTypes.POINTERMOVE) {
                    updatePreviewPosition()
                }
            })

            // Register pointer down observer to place the discussion
            val pointerDownObserver = scene.onPointerObservable.add(fun(info: PointerInfo) {
                // Only process clicks when isPlacingDiscussion is true
                if (info.type == PointerEventTypes.POINTERDOWN && isPlacingDiscussion && !info.event.shiftKey) {
                    // Get the current position of the preview marker
                    val position = marker.position.clone()

                    // Show input dialog to get user's comment after clicking
                    scope.launch {
                        try {
                            val comment = inputDialog(
                                title = "New Discussion",
                                placeholder = "Enter your thoughts...",
                                singleLine = false
                            )

                            if (!comment.isNullOrBlank()) {
                                // Create the discussion at this position with the comment
                                createDiscussion(position, gameScene?.id, scope, map, discussionMarkers, comment)
                            }
                        } finally {
                            // Reset state regardless of success or failure
                            isPlacingDiscussion = false
                            // Explicitly remove the preview marker
                            if (previewMarker != null) {
                                map.game?.scene?.removeMesh(previewMarker!!)
                                previewMarker = null
                            }
                        }
                    }
                }
            })

            // Register keyboard observer to handle cancellation
            val keyboardObserver = scene.onKeyboardObservable.add(fun(info: KeyboardInfo) {
                if (info.type == KeyboardEventTypes.KEYDOWN && info.event.key == "Escape") {
                    // Cancel placing discussion
                    isPlacingDiscussion = false

                    // Explicitly remove the preview marker when Escape is pressed
                    if (previewMarker != null) {
                        map.game?.scene?.removeMesh(previewMarker!!)
                        previewMarker = null
                    }
                }
            })

            // Initial position update
            updatePreviewPosition()

            onDispose {
                // We can't remove observers in the current API, but we can clean up the preview marker
                if (previewMarker != null) {
                    scene.removeMesh(previewMarker!!)
                    previewMarker = null
                }
            }
        } else {
            // Clean up the preview marker if we're not placing a discussion
            if (previewMarker != null) {
                map.game?.scene?.removeMesh(previewMarker!!)
                previewMarker = null
            }

            onDispose { }
        }
    }

    // Clean up markers when the component is disposed
    DisposableEffect(Unit) {
        onDispose {
            discussionMarkers.clearMarkers()

            // Make sure to clean up the preview marker
            if (previewMarker != null) {
                map.game?.scene?.removeMesh(previewMarker!!)
            }
        }
    }

    // Function to start placing a new discussion
    fun startNewDiscussion() {
        if (gameScene?.id == null || me == null) return

        // Deselect any tile or object that is currently selected to prevent drawing when adding a comment
        map.setCurrentGameTile(null)
        map.setCurrentGameObject(null)

        // Set the flag to indicate we're in discussion placement mode immediately
        isPlacingDiscussion = true
    }

    Div({
        style {
            padding(1.r)
        }
    }) {
        // Discussions section
        PanelSection(
            title = "Discussions",
            icon = "forum",
            initiallyExpanded = true
        ) {
            // Start new discussion button
            if (me != null && gameScene?.id != null) {
                Button({
                    classes(Styles.button)
                    style {
                        marginBottom(1.r)
                        width(100.percent)
                    }
                    onClick {
                        startNewDiscussion()
                    }
                    if (isPlacingDiscussion) {
                        attr("disabled", "true")
                    }
                }) {
                    if (isPlacingDiscussion) {
                        Text("Click on the scene to place discussion...")
                    } else {
                        Text("Start New Discussion")
                    }
                }
            }

            // Loading state
            if (discussions == null) {
                Div({
                    style {
                        padding(1.r)
                    }
                }) {
                    Text("Loading discussions...")
                }
            } else if (discussions.isNullOrEmpty()) {
                // No discussions yet
                Div({
                    style {
                        padding(1.r)
                        opacity(0.7f)
                    }
                }) {
                    Text("No discussions yet. Click 'Start New Discussion' to add one.")
                }
            } else {
                // List of discussions
                discussions?.forEach { discussion ->
                    val discussionId = discussion.discussion?.id
                    val isSelected = discussionId == selectedDiscussionId

                    Div({
                        classes(AppStyles.groupItem, AppStyles.groupItemOnSurface)
                        if (isSelected) {
                            classes(AppStyles.groupItemSelected)
                        }
                        onClick {
                            if (discussionId != null) {
                                // Select this discussion
                                selectedDiscussionId = discussionId

                                // Load the full discussion details
                                scope.launch {
                                    api.gameDiscussion(discussionId) {
                                        selectedDiscussion = it
                                    }
                                }

                                // Focus the camera on this discussion
                                discussionMarkers.focusOnMarker(discussionId, map.camera)
                            }
                        }
                    }) {
                        // Show the photo of the person who started the discussion
                        discussion.person?.let { person ->
                            GroupPhoto(
                                items = listOf(
                                    GroupPhotoItem(
                                        photo = person.photo,
                                        name = person.name
                                    )
                                )
                            )
                        }

                        Div({
                            style {
                                marginLeft(1.r)
                                flex(1)
                            }
                        }) {
                            // Discussion title/initial comment
                            Div({
                                classes(AppStyles.groupItemName)
                            }) {
                                Text(discussion.discussion?.comment ?: "")
                            }

                            // Show latest comment if available
                            discussion.comments?.lastOrNull()?.let { latestComment ->
                                Div({
                                    classes(AppStyles.groupItemMessage)
                                }) {
                                    Text(latestComment.comment?.comment ?: "")
                                }
                            }
                        }
                    }
                }
            }
        }

        // Selected discussion details and comments
        selectedDiscussion?.let { discussion ->
            PanelSection(
                title = "Discussion",
                icon = "chat",
                initiallyExpanded = true
            ) {
                // Discussion creator and initial comment
                Div({
                    style {
                        padding(0.5.r)
                        marginBottom(1.r)
                    }
                }) {
                    // Creator info
                    discussion.person?.let { person ->
                        Div({
                            style {
                                marginBottom(0.5.r)
                                display(DisplayStyle.Flex)
                                alignItems(AlignItems.Center)
                            }
                        }) {
                            ProfilePhoto(person, size = 2.r)
                            Div({
                                style {
                                    padding(0.5.r)
                                }
                            }) {
                                Text(person.name ?: "Anonymous")
                            }
                        }
                    }

                    // Initial comment
                    Div({
                        style {
                            padding(0.5.r)
                            marginBottom(1.r)
                        }
                    }) {
                        LinkifyText(discussion.discussion?.comment ?: "")
                    }

                    // Resolve button - only show if user is the creator and discussion is not resolved
                    if (me != null && discussion.person?.id == me?.id && discussion.discussion?.resolved != true) {
                        Button({
                            classes(Styles.button)
                            style {
                                marginBottom(1.r)
                                width(100.percent)
                            }
                            onClick {
                                discussion.discussion?.id?.let { discussionId ->
                                    scope.launch {
                                        api.resolveGameDiscussion(discussionId) {
                                            // Update the local state to show the discussion as resolved
                                            selectedDiscussion = selectedDiscussion?.copy(
                                                discussion = selectedDiscussion?.discussion?.copy(
                                                    resolved = true
                                                )
                                            )

                                            // Reload the discussions list to remove this discussion
                                            gameScene?.id?.let { sceneId ->
                                                api.gameSceneDiscussions(sceneId) {
                                                    discussions = it

                                                    // If the resolved discussion was selected, deselect it
                                                    if (selectedDiscussionId == discussionId) {
                                                        selectedDiscussionId = null
                                                        selectedDiscussion = null
                                                    }

                                                    // Update markers in the 3D scene
                                                    discussionMarkers.updateMarkers(it)
                                                }
                                            }
                                        }
                                    }
                                }
                            }
                        }) {
                            Text("Resolve Discussion")
                        }
                    }

                    // Show resolved status if the discussion is resolved
                    if (discussion.discussion?.resolved == true) {
                        Div({
                            style {
                                padding(0.5.r)
                                marginBottom(1.r)
                                backgroundColor(rgba(200, 255, 200, 0.5))
                                borderRadius(0.5.r)
                            }
                        }) {
                            Text("✓ This discussion has been resolved")
                        }
                    }

                    // Comment input field
                    if (me != null) {
                        EditField(
                            placeholder = "Add a comment...",
                            styles = {
                                width(100.percent)
                            },
                            buttonBarStyles = {
                                width(100.percent)
                                display(DisplayStyle.Flex)
                                flexDirection(FlexDirection.RowReverse)
                            },
                            resetOnSubmit = true,
                            button = "Post"
                        ) { commentText ->
                            // Return true to indicate success and reset the input field
                            discussion.discussion?.id?.let { discussionId ->
                                scope.launch {
                                    try {
                                        api.commentOnGameDiscussion(
                                            discussionId,
                                            Comment(comment = commentText)
                                        ) {
                                            // Reload the discussion after adding a new comment
                                            // Add a small delay to ensure the backend has processed the new comment
                                            kotlinx.coroutines.delay(300)
                                            api.gameDiscussion(discussionId) {
                                                selectedDiscussion = it
                                            }
                                        }
                                    } catch (e: Exception) {
                                        console.error("Error posting comment: ${e.message}")
                                    }
                                }
                            }
                            // Always return true to reset the input field
                            true
                        }
                    } else {
                        Div({
                            style {
                                padding(0.5.r)
                                opacity(0.7f)
                            }
                        }) {
                            Text("Sign in to comment")
                        }
                    }

                    // Comments section
                    discussion.comments?.let { comments ->
                        if (comments.isNotEmpty()) {
                            Div({
                                style {
                                    marginBottom(1.r)
                                }
                            }) {
                                StoryComments(
                                    comments = comments,
                                    loadRepliesInline = true,
                                    onReply = { comment ->
                                        // Reload the discussion after a new comment is added
                                        selectedDiscussionId?.let { id ->
                                            api.gameDiscussion(id) {
                                                selectedDiscussion = it
                                            }
                                        }
                                    }
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}
