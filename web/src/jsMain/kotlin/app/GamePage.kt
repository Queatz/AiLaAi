package app

import Styles
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
import app.ailaai.api.createGameObject
import app.ailaai.api.gameObject
import app.ailaai.api.gameTile
import app.ailaai.api.uploadPhotos
import app.dialog.dialog
import app.game.GameEditorPanel
import app.game.GameMusicPlayer
import app.game.GameObjectsData
import app.game.GameTilesData
import app.game.editor.Seekbar
import app.game.editor.assetManager
import app.game.json
import app.softwork.routingcompose.Router
import appText
import application
import com.queatz.db.Color4Data
import com.queatz.db.GameObject
import com.queatz.db.GameScene
import com.queatz.db.GameSceneConfig
import components.IconButton
import game.Game
import game.Side
import getImageDimensions
import kotlinx.browser.document
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch
import lib.FullscreenApi
import lib.ResizeObserver
import lib.Vector3
import org.jetbrains.compose.web.ExperimentalComposeWebApi
import org.jetbrains.compose.web.css.AlignItems
import org.jetbrains.compose.web.css.DisplayStyle
import org.jetbrains.compose.web.css.JustifyContent
import org.jetbrains.compose.web.css.Position
import org.jetbrains.compose.web.css.StyleScope
import org.jetbrains.compose.web.css.alignItems
import org.jetbrains.compose.web.css.backgroundColor
import org.jetbrains.compose.web.css.borderRadius
import org.jetbrains.compose.web.css.bottom
import org.jetbrains.compose.web.css.boxSizing
import org.jetbrains.compose.web.css.color
import org.jetbrains.compose.web.css.display
import org.jetbrains.compose.web.css.flex
import org.jetbrains.compose.web.css.flexShrink
import org.jetbrains.compose.web.css.fontSize
import org.jetbrains.compose.web.css.height
import org.jetbrains.compose.web.css.justifyContent
import org.jetbrains.compose.web.css.left
import org.jetbrains.compose.web.css.maxWidth
import org.jetbrains.compose.web.css.overflow
import org.jetbrains.compose.web.css.padding
import org.jetbrains.compose.web.css.percent
import org.jetbrains.compose.web.css.position
import org.jetbrains.compose.web.css.px
import org.jetbrains.compose.web.css.rgba
import org.jetbrains.compose.web.css.right
import org.jetbrains.compose.web.css.top
import org.jetbrains.compose.web.css.transform
import org.jetbrains.compose.web.css.unaryMinus
import org.jetbrains.compose.web.css.width
import org.jetbrains.compose.web.dom.Button
import org.jetbrains.compose.web.dom.Canvas
import org.jetbrains.compose.web.dom.Div
import org.jetbrains.compose.web.dom.Text
import org.w3c.dom.HTMLAnchorElement
import org.w3c.dom.HTMLCanvasElement
import org.w3c.dom.HTMLElement
import org.w3c.dom.clipboard.ClipboardEvent
import org.w3c.dom.events.Event
import org.w3c.dom.events.EventListener
import org.w3c.dom.get
import org.w3c.files.File
import r
import toBytes
import toggleFullscreen
import web.cssom.ImageRendering
import kotlin.js.Date

// Function to load tiles, objects, and animation data from gameScene
private fun loadGameSceneData(scope: CoroutineScope, game: Game, gameScene: GameScene, onPixelatedChanged: (Boolean) -> Unit = {}) {
    // Load tiles if available
    if (gameScene.tiles != null) {
        try {
            // Process each tile in the array
            val tilesData = json.decodeFromString<GameTilesData>(gameScene.tiles!!)
            console.log("Loading ${tilesData.tiles.size} tiles from JSON")

            // Group tiles by their tileId to fetch each GameTile only once
            val tilesByTileId = tilesData.tiles.filter { it.tileId != null }.groupBy { it.tileId!! }

            // Process each group of tiles with the same tileId
            tilesByTileId.forEach { (tileId, tilesWithSameId) ->
                scope.launch {
                    api.gameTile(tileId, onSuccess = { gameTile ->
                        // Create a list of triples (position, side, gameTile) for all tiles with this tileId
                        val tileList = tilesWithSameId.map { tile ->
                            val position = Vector3(tile.x.toFloat(), tile.y.toFloat(), tile.z.toFloat())
                            val side = Side.fromString(tile.side)
                            Triple(position, side, gameTile)
                        }

                        // Set all tiles at once
                        game.map.tilemapEditor.tilemap.setTiles(tileList)
                    })
                }
            }
        } catch (e: Exception) {
            console.error("Error loading tiles: ${e.message}")
        }
    }

    // Load objects if available
    if (gameScene.objects != null) {
        try {
            // Process each object in the array
            val objectsData = json.decodeFromString<GameObjectsData>(gameScene.objects!!)
            console.log("Loading ${objectsData.objects.size} objects from JSON")

            for (obj in objectsData.objects) {
                if (obj.objectId != null && obj.objectId != "default") {
                    // Fetch the GameObject by ID and add it to the map
                    scope.launch {
                        api.gameObject(obj.objectId, onSuccess = { gameObject ->
                            // Set the object at the specified position
                            val position = Vector3(obj.x.toFloat(), obj.y.toFloat(), obj.z.toFloat())
                            val side = Side.fromString(obj.side)
                            // Now we can access the tilemap via map.tilemapEditor.tilemap
                            game.map.tilemapEditor.tilemap.addObject(position, side, gameObject)
                        })
                    }
                }
            }
        } catch (e: Exception) {
            console.error("Error loading objects: ${e.message}")
        }
    }

    // Load animation data if available
    if (gameScene.config != null) {
        try {
            val config = json.decodeFromString<GameSceneConfig>(gameScene.config!!)

            // Load saved positions
            config.savedPositions.forEach { positionData ->
                // Add the saved position to the animation data
                game.animationData.savedPositions += positionData
                console.log("Loaded saved position: ${positionData.name} at position (${positionData.position.x}, ${positionData.position.y}, ${positionData.position.z})")
            }

            // Load markers (including any PlayMusicEvent)
            config.markers.forEach { markerData ->
                // Create a new marker with the same name and time
                val marker = game.animationData.addMarker(markerData.name)
                // Update the marker properties
                marker.time = markerData.time
                marker.duration = markerData.duration
                // Set visibility if available (with fallback to true)
                marker.visible = markerData.visible
                // Preserve any associated event (e.g., PlayMusicEvent)
                marker.event = markerData.event
                console.log(
                    "Loaded marker: ${markerData.name} at time ${markerData.time} " +
                    "with duration ${markerData.duration} and event=${markerData.event}"
                )
            }
            // Load captions
            config.captions.forEach { captionData ->
                game.animationData.addCaptionFromData(
                    captionData.id,
                    captionData.time,
                    captionData.text,
                    captionData.duration
                )
                console.log("Loaded caption: ${captionData.text} at time ${captionData.time} with duration ${captionData.duration}")
            }

            // Load camera keyframes
            config.cameraKeyframes.forEach { keyframeData ->
                // Convert Vector3Data to Vector3
                val position = Vector3(keyframeData.position.x, keyframeData.position.y, keyframeData.position.z)
                val target = Vector3(keyframeData.target.x, keyframeData.target.y, keyframeData.target.z)

                // Add the keyframe to the animation data
                val keyframe = game.animationData.addCameraKeyframeFromData(
                    id = keyframeData.id,
                    time = keyframeData.time,
                    position = position,
                    target = target,
                    alpha = keyframeData.alpha,
                    beta = keyframeData.beta,
                    radius = keyframeData.radius,
                    fov = keyframeData.fov
                )
            }

            console.log("Animation data loaded successfully")

            // Set camera position to the first keyframe if any exist
            if (game.animationData._cameraKeyframes.isNotEmpty()) {
                val firstKeyframe = game.animationData._cameraKeyframes.minByOrNull { it.time }
                firstKeyframe?.let { keyframe ->
                    // Apply the first keyframe to the camera
                    game.setTime(keyframe.time)
                }
            }

            // Set background color
            val bgColor = config.backgroundColor ?: Color4Data(.8f, .8f, .8f, 1f)
            game.scene.clearColor = lib.Color4(
                bgColor.r,
                bgColor.g,
                bgColor.b,
                bgColor.a
            )
            console.log("Background color set to (${bgColor.r}, ${bgColor.g}, ${bgColor.b}, ${bgColor.a})")

            config.snowEffectEnabled?.let { enabled -> game.map.setSnowEffectEnabled(enabled) }
            config.rainEffectEnabled?.let { enabled -> game.map.setRainEffectEnabled(enabled) }
            config.dustEffectEnabled?.let { enabled -> game.map.setDustEffectEnabled(enabled) }

            console.log("Weather configured")

            // Set camera FOV if available
            if (config.cameraFov != null) {
                game.scene.activeCamera?.let { 
                    it.fov = config.cameraFov!!
                    console.log("Camera FOV set to ${config.cameraFov}")
                }
            }
            // Set camera orthographic mode if available
            config.cameraOrthographic?.let { enabled ->
                game.map.set("orthographic", enabled.toString())
                console.log("Camera orthographic set to $enabled")
            }

            // Apply graphics settings if available
            // Set SSAO
            config.ssaoEnabled?.let { enabled ->
                game.map.post.ssaoEnabled = enabled
                console.log("SSAO enabled set to $enabled")
            }

            // Set Bloom
            config.bloomEnabled?.let { enabled ->
                game.map.post.bloomEnabled = enabled
                console.log("Bloom enabled set to $enabled")
            }

            // Set Sharpen
            config.sharpenEnabled?.let { enabled ->
                game.map.post.sharpenEnabled = enabled
                console.log("Sharpen enabled set to $enabled")
            }

            // Set Color Correction
            config.colorCorrectionEnabled?.let { enabled ->
                game.map.post.colorCorrectionEnabled = enabled
                console.log("Color Correction enabled set to $enabled")
            }

            // Set Linear Sampling
            config.linearSamplingEnabled?.let { enabled ->
                game.map.linearSamplingEnabled = enabled
                console.log("Linear Sampling enabled set to $enabled")
                onPixelatedChanged(enabled)
            }

            // Set Depth of Field
            config.depthOfFieldEnabled?.let { enabled ->
                game.map.post.depthOfFieldEnabled = enabled
                console.log("Depth of Field enabled set to $enabled")
            }

            // Set Ambience Intensity
            config.ambienceIntensity?.let { intensity ->
                game.map.set("ambienceIntensity", intensity)
                console.log("Ambience Intensity set to $intensity")
            }

            // Set Sun Intensity
            config.sunIntensity?.let { intensity ->
                game.map.set("sunIntensity", intensity)
                console.log("Sun Intensity set to $intensity")
            }

            // Set Fog Density
            config.fogDensity?.let { density ->
                game.map.set("fogDensity", density)
                console.log("Fog Density set to $density")
            }

            // Set Time of Day
            config.timeOfDay?.let { time ->
                game.map.set("timeOfDay", time)
                console.log("Time of Day set to $time")
            }

            // Set Brush Size
            config.brushSize?.let { size ->
                game.map.set("brushSize", size)
                console.log("Brush Size set to $size")
            }

            // Set Brush Density
            config.brushDensity?.let { density ->
                game.map.set("brushDensity", density)
                console.log("Brush Density set to $density")
            }

            // Set Grid Size
            config.gridSize?.let { size ->
                game.map.set("gridSize", size)
                console.log("Grid Size set to $size")
            }

            // Set Pixel Size
            config.pixelSize?.let { size ->
                game.map.pixelSize = size
                console.log("Pixel Size set to $size")
            }

            // Set Snow Effect
            config.snowEffectEnabled?.let { enabled ->
                game.map.set("snowEffectEnabled", enabled.toString())
                console.log("Snow Effect enabled set to $enabled")
            }
            config.snowEffectIntensity?.let { intensity ->
                game.map.set("snowEffectIntensity", intensity)
                console.log("Snow Effect intensity set to $intensity")
            }

            // Set Rain Effect
            config.rainEffectEnabled?.let { enabled ->
                game.map.set("rainEffectEnabled", enabled.toString())
                console.log("Rain Effect enabled set to $enabled")
            }
            config.rainEffectIntensity?.let { intensity ->
                game.map.set("rainEffectIntensity", intensity)
                console.log("Rain Effect intensity set to $intensity")
            }

            // Set Dust Effect
            config.dustEffectEnabled?.let { enabled ->
                game.map.set("dustEffectEnabled", enabled.toString())
                console.log("Dust Effect enabled set to $enabled")
            }
            config.dustEffectIntensity?.let { intensity ->
                game.map.set("dustEffectIntensity", intensity)
                console.log("Dust Effect intensity set to $intensity")
            }
            // Load sketch layers from config
            game.map.sketchManager.loadData(config.sketchLayers)
        } catch (e: Exception) {
            console.error("Error loading animation data: ${e.message}")
        }
    }
}

@OptIn(ExperimentalComposeWebApi::class)
@Composable
fun GamePage(
    gameScene: GameScene? = null,
    onSceneDeleted: () -> Unit = {},
    onScenePublished: () -> Unit = {},
    onSceneForked: (GameScene) -> Unit = {},
    onSceneUpdated: (GameScene) -> Unit = {},
    onSceneSaved: (GameScene) -> Unit = {},
    onGameCreated: (Game) -> Unit = {},
    styles: StyleScope.() -> Unit = {},
    content: @Composable () -> Unit = {},
    showSidePanel: Boolean = true,
    showScreenshot: Boolean = true,
    editable: Boolean = true,
) {
    val me by application.me.collectAsState()
    val scope = rememberCoroutineScope()
    var canvasRef by remember { mutableStateOf<HTMLCanvasElement?>(null) }
    var fullscreenContainerRef by remember { mutableStateOf<HTMLElement?>(null) }
    val router = Router.current

    // Create a mutable state for gameScene to update it when renamed
    var localGameScene by remember(gameScene) { mutableStateOf(gameScene) }

    // Make game state depend on localGameScene to ensure recreation when gameScene changes
    var game by remember(localGameScene) {
        mutableStateOf<Game?>(null)
    }

    // If editable, hide the centered play overlay initially; otherwise show it
    var showPlayButton by remember(game) { mutableStateOf(!editable) }

    // Track fullscreen, playing, and side-panel visibility states
    var isFullscreen by remember { mutableStateOf(false) }
    var isPlaying by remember { mutableStateOf(false) }
    var isPixelated by remember { mutableStateOf(false) }
    var allowPaste by remember { mutableStateOf(false) }

    var showPanel by remember { mutableStateOf(showSidePanel) }
    // Hide play button when animation is playing
    LaunchedEffect(isPlaying) {
        if (isPlaying) {
            showPlayButton = false
        }
    }

    // Listen for fullscreen change events
    DisposableEffect(Unit) {
        val fullscreenChangeListener = EventListener { 
            isFullscreen = FullscreenApi.isFullscreen
        }

        FullscreenApi.addFullscreenChangeListener(fullscreenChangeListener)

        onDispose {
            FullscreenApi.removeFullscreenChangeListener(fullscreenChangeListener)
        }
    }

    DisposableEffect(localGameScene, canvasRef, editable) {
        localGameScene ?: return@DisposableEffect onDispose { }
        canvasRef ?: return@DisposableEffect onDispose { }

        val localGame = Game(canvasRef!!, editable).also { gameImpl ->
            // Load existing scene data (tiles, objects, config)
            if (localGameScene?.tiles != null || localGameScene?.objects != null || localGameScene?.config != null) {
                loadGameSceneData(scope, gameImpl, localGameScene!!) { pixelated ->
                    isPixelated = pixelated
                }
            } else {
                // Default scene setup
                gameImpl.scene.clearColor = lib.Color4(
                    r = .8f,
                    g = .8f,
                    b = .8f,
                    a = 1f
                )
            }
        }
        game = localGame

        // Notify that the game instance has been created
        onGameCreated(localGame)

        onDispose {
            localGame.dispose()
        }
    }

    // Set up flow collection to update playing state
    LaunchedEffect(game) {
        game?.playStateFlow?.collectLatest { playing ->
            isPlaying = playing
        }
    }

    DisposableEffect(canvasRef, allowPaste) {
        if (!allowPaste) return@DisposableEffect onDispose {  }

        val pasteListener = EventListener { event: Event ->
            console.log(event)

            val event = event as? ClipboardEvent ?: return@EventListener

            // Only process if we have a game instance
            val gameInstance = game ?: return@EventListener

            // Get clipboard data
            val clipboardData = event.clipboardData ?: return@EventListener

            // Check for images
            val items = clipboardData.items
            val photos = mutableListOf<File>()

            for (i in 0 until items.length) {
                val item = items[i] ?: continue
                val type = item.type

                if (type.startsWith("image/")) {
                    val file = item.getAsFile() ?: continue
                    photos.add(file)
                }
            }

            if (photos.isNotEmpty()) {
                // Handle pasted image
                event.preventDefault() // Prevent default only if we're handling the paste

                // Process all photos
                photos.forEach { photo ->
                    scope.launch {
                        try {
                            // Get image dimensions
                            val dimensions = photo.getImageDimensions()

                            // Convert File to ByteArray
                            val photoBytes = photo.toBytes()

                            // Upload the image
                            api.uploadPhotos(
                                photos = listOf(photoBytes),
                                onSuccess = { response ->
                                    val photoUrl = response.urls.firstOrNull() ?: return@uploadPhotos

                                    // Create a new game object with the uploaded image
                                    // Use dimensions divided by 100
                                    scope.launch {
                                        api.createGameObject(
                                            gameObject = GameObject(
                                                photo = photoUrl,
                                                width = (dimensions.width / 1000.0).toString(),
                                                height = (dimensions.height / 1000.0).toString()
                                            ),
                                            onSuccess = { newObject ->
                                                // Add the new object to the asset manager
                                                assetManager.addObject(newObject)

                                                // Set the current game object
                                                gameInstance.map.setCurrentGameObject(newObject)

                                                gameInstance.map.tilemapEditor.draw(false)
                                            }
                                        )
                                    }
                                }
                            )
                        } catch (e: Exception) {
                            console.error("Error processing pasted image", e)
                        }
                    }
                }
            } else {
                // Check for text
                val text = clipboardData.getData("text/plain")
                if (text.isNotEmpty()) {
                    // Handle pasted text
                    event.preventDefault() // Prevent default only if we're handling the paste

                    gameInstance.map.tilemapEditor?.let { editor ->
                        // Get the current cursor position
                        val position = editor.pickedPoint

                        // Add a new saved position with the pasted text
                        gameInstance.animationData.addSavedPosition(text, position)
                        scope.launch {
                            gameInstance.animationDataChanged.emit(Unit)
                        }
                    }
                } else {
                    // No image or text found
                    scope.launch {
                        dialog(
                            title = "Nothing found to paste.",
                            cancelButton = null
                        )
                    }
                }
            }
        }
        document.addEventListener("paste", pasteListener)

        onDispose {
            document.removeEventListener("paste", pasteListener)
        }
    }

    Div({
        // Add a class to identify the game container for fullscreen
        classes(Styles.fullscreenContainer)
        style {
            display(DisplayStyle.Flex)
            width(100.percent)
            height(100.percent)
            boxSizing("border-box")
            if (!isFullscreen) {
                padding(1.r)
            } else {
                // Hide scrollbars without using position: fixed or overflow: hidden
                // which can break other functionality
                property("scrollbar-width", "none") // Firefox
            }
            styles()
        }

        ref {
            fullscreenContainerRef = it

            onDispose {
                fullscreenContainerRef = null
            }
        }
    }) {
        Div(
            attrs = {
                style {
                    position(Position.Relative)
                    height(100.percent)
                    flex(1)
                }
            }
        ) {
            Canvas(
                attrs = {
                    onFocus {
                        allowPaste = true
                    }

                    onBlur {
                        allowPaste = false
                    }

                    style {
                        height(100.percent)
                        width(100.percent)
                        overflow("hidden")
                        if (!isFullscreen) {
                            borderRadius(1.r)
                        }

                        if (isPixelated) {
                            property("image-rendering", ImageRendering.pixelated.toString())
                        }
                    }

                    tabIndex(0)

                    // Add keyboard event listener for animation control and tool-aware play/pause
                    onKeyDown { event ->
                        when {
                            (event.key == " " || event.code == "Space") -> {
                                // Always prevent default for spacebar to avoid scrolling
                                event.preventDefault()
                                // Stop propagation for repeat events to prevent constant toggling
                                if (event.repeat) {
                                    event.stopPropagation()
                                }
                            }
                            game?.isPlaying() == true -> {
                                game?.pause()
                                isPlaying = false
                                event.preventDefault()
                                event.stopPropagation()
                            }
                            event.key == "Escape" -> {
                                showPlayButton = false
                                // Cancel line drawing if active
                                game?.map?.tilemapEditor?.cancelLineDrawing()
                                event.preventDefault()
                                event.stopPropagation()
                            }
                            event.key == "ArrowLeft" && event.altKey && event.shiftKey -> {
                                game?.let {
                                    val newTime = (it.animationData.currentTime - 1.0)
                                        .coerceIn(0.0, it.animationData.totalDuration)
                                    it.setTime(newTime)
                                }
                                event.preventDefault()
                                event.stopPropagation()
                            }
                            event.key == "ArrowLeft" && event.altKey -> {
                                game?.let {
                                    val newTime = (it.animationData.currentTime - 0.1)
                                        .coerceIn(0.0, it.animationData.totalDuration)
                                    it.setTime(newTime)
                                }
                                event.preventDefault()
                                event.stopPropagation()
                            }
                            event.key == "ArrowRight" && event.altKey && event.shiftKey -> {
                                game?.let {
                                    val newTime = (it.animationData.currentTime + 1.0)
                                        .coerceIn(0.0, it.animationData.totalDuration)
                                    it.setTime(newTime)
                                }
                                event.preventDefault()
                                event.stopPropagation()
                            }
                            event.key == "ArrowRight" && event.altKey -> {
                                game?.let {
                                    val newTime = (it.animationData.currentTime + 0.1)
                                        .coerceIn(0.0, it.animationData.totalDuration)
                                    it.setTime(newTime)
                                }
                                event.preventDefault()
                                event.stopPropagation()
                            }
                        }
                    }

                    onMouseEnter {
                        canvasRef?.focus()
                    }

                    // Add mouse event listener to stop animation on click
                    onClick { event ->
                        if (game?.isPlaying() == true) {
                            game?.pause()
                            isPlaying = false
                            event.preventDefault()
                        }
                    }

                    ref { canvas ->
                        canvasRef = canvas
                        canvas.focus()

                        canvas.width = canvas.clientWidth
                        canvas.height = canvas.clientHeight

                        val observer = ResizeObserver { _, _ ->
                            canvas.width = canvas.clientWidth
                            canvas.height = canvas.clientHeight
                            game?.resize()
                        }.apply {
                            observe(canvas)
                        }

                        onDispose {
                            game?.dispose()
                            observer.disconnect()
                        }
                    }
                }
            )
            // Caption overlay: display active captions at bottom, above seekbar
            game?.let { g ->
                val currentTime = g.animationData.collectCurrentTime()
                val activeCaptions = g.animationData.captions.filter { cap ->
                    currentTime >= cap.time && currentTime <= cap.time + cap.duration
                }
                if (activeCaptions.isNotEmpty()) {
                    Div({
                        style {
                            position(Position.Absolute)
                            bottom(6.r)
                            left(50.percent)
                            transform { translateX(-50.percent) }
                            backgroundColor(rgba(0, 0, 0, 0.7f))
                            color(Styles.colors.white)
                            padding(2.r)
                            borderRadius(2.r)
                            fontSize(28.px)
                            property("text-align", "center")
                            property("z-index", "50")
                            property("pointer-events", "none")
                        }
                    }) {
                        activeCaptions.forEach { cap ->
                            val elapsed = currentTime - cap.time
                            val revealCount = (elapsed * 15).toInt().coerceIn(0, cap.text.length)
                            Text(cap.text.take(revealCount))
                        }
                    }
                }
            }

            game?.let { GameMusicPlayer(it) }

            // Show sign in button if editable is false and me is null
            if (!editable && me == null && !isPlaying && !isFullscreen) {
                Button({
                    classes(Styles.outlineButton)
                    style {
                        top(1.r)
                        left(1.r)
                        position(Position.Absolute)
                        property("z-index", "10")
                    }
                    onClick {
                        router.navigate("/signin")
                    }
                }) {
                    appText { signUp }
                }
            }

            if (showPlayButton) {
                Div({
                    style {
                        position(Position.Absolute)
                        left(50.percent)
                        top(3.r)
                        transform {
                            translateX(-50.percent)
                        }
                        backgroundColor(rgba(255, 255, 255, 0.5))
                        padding(1.r, 2.r)
                        borderRadius(1.r)
                        property("z-index", "10")
                        color(Styles.colors.black)
                        property("pointer-events", "none")
                        fontSize(32.px)
                    }
                }) {
                    Text(localGameScene?.name ?: "Location Name")
                }

                Div({
                    style {
                        position(Position.Absolute)
                        left(50.percent)
                        top(50.percent)
                        transform {
                            translateX(-50.percent)
                            translateY(-50.percent)
                        }
                        property("z-index", "10")
                    }
                }) {
                    IconButton(
                        name = "play_arrow",
                        title = "Play",
                        background = true,
                        styles = {
                            width(12.r)
                            height(12.r)
                            borderRadius(50.percent)
                            backgroundColor(rgba(0, 0, 0, 0.5))
                            color(Styles.colors.white)
                            display(DisplayStyle.Flex)
                            alignItems(AlignItems.Center)
                            justifyContent(JustifyContent.Center)
                        },
                        iconStyles = {
                            fontSize(96.px)
                        }
                    ) {
                        showPlayButton = false
                        game?.play()
                        isPlaying = true
                    }
                }
            }

            // Control buttons (Screenshot and Fullscreen)
            Div({
                style {
                    position(Position.Absolute)
                    right(1.r)
                    top(1.r)
                    property("z-index", "10")
                    display(DisplayStyle.Flex)
                    overflow("auto")
                    maxWidth("calc(${100.percent} - ${2.r})")
                }
            }) {
                // Fullscreen button
                IconButton(
                    name = if (isFullscreen) "fullscreen_exit" else "fullscreen",
                    title = if (isFullscreen) "Exit Fullscreen" else "Enter Fullscreen",
                    background = true,
                    styles = {
                        backgroundColor(rgba(0, 0, 0, 0.5))
                        color(Styles.colors.white)
                        padding(0.5.r)
                        borderRadius(0.5.r)
                        property("margin-right", "0.5rem")
                    }
                ) {
                    fullscreenContainerRef.toggleFullscreen()
                }

                // Play/Pause button
                IconButton(
                    name = if (isPlaying) "pause" else "play_arrow",
                    title = if (isPlaying) "Pause" else "Play",
                    background = true,
                    styles = {
                        backgroundColor(rgba(0, 0, 0, 0.5))
                        color(Styles.colors.white)
                        padding(0.5.r)
                        borderRadius(0.5.r)
                        property("margin-right", "0.5rem")
                    }
                ) {
                    game?.togglePlayback()
                }
                // Toggle side panel visibility when not editable
                if (!editable) {
                    IconButton(
                        name = "forum",
                        title = "Show discussion",
                        background = true,
                        styles = {
                            backgroundColor(rgba(0, 0, 0, 0.5))
                            color(Styles.colors.white)
                            padding(0.5.r)
                            borderRadius(0.5.r)
                            property("margin-right", "0.5rem")
                        }
                    ) {
                        showPanel = !showPanel
                    }
                }

                // Screenshot button
                if (showScreenshot) {
                    IconButton(
                        name = "photo_camera",
                        title = "Take Screenshot",
                        background = true,
                        styles = {
                            backgroundColor(rgba(0, 0, 0, 0.5))
                            color(Styles.colors.white)
                            padding(0.5.r)
                            borderRadius(0.5.r)
                        }
                    ) {
                        canvasRef?.let { canvas ->
                            // Get the data URL of the canvas using the extension function
                            val dataUrl = canvas.toDataURL("image/png")

                            // Create a timestamp for the filename
                            val timestamp = Date().toISOString().replace(":", "-").replace(".", "-")
                            val filename = "screenshot-$timestamp.png"

                            // Create a temporary link element to trigger the download
                            val link = document.createElement("a") as HTMLAnchorElement
                            link.href = dataUrl
                            link.download = filename

                            // Append to the document, click to download, then remove
                            document.body?.appendChild(link)
                            link.click()
                            document.body?.removeChild(link)
                        }
                    }
                }
            }

            Div(
                attrs = {
                    style {
                        position(Position.Absolute)
                        left(0.r)
                        right(0.r)
                        bottom(0.r)
                    }
                }
            ) {
                game?.let { game ->
                    Seekbar(
                        currentPosition = game.animationData.collectCurrentTime(),
                        markers = game.animationData.markers.map { marker ->
                            app.game.editor.SeekbarMarker(
                                position = marker.time,
                                name = marker.name,
                                duration = marker.duration,
                                visible = marker.visible
                            )
                        },
                        keyframes = if (editable) game.animationData.cameraKeyframes.map { keyframe ->
                            app.game.editor.SeekbarKeyframe(keyframe.time)
                        } else emptyList(),
                        onPositionChange = { time ->
                            game.setTime(time)
                        }
                    )
                }
            }
        }
        if (showPanel) {
            game?.let { game ->
                GameEditorPanel(
                    engine = game.engine,
                    map = game.map,
                    editable = editable,
                    gameScene = localGameScene,
                    onSceneDeleted = onSceneDeleted,
                    onScenePublished = onScenePublished,
                    onSceneForked = onSceneForked,
                    onSceneUpdated = { updatedScene ->
                        localGameScene = updatedScene
                        onSceneUpdated(updatedScene)
                    },
                    onSceneSaved = { savedScene ->
                        localGameScene = savedScene
                        onSceneSaved(savedScene)
                    },
                    styles = {
                        flexShrink(0)
                    },
                    onPixelatedChanged = {
                        isPixelated = it
                    }
                )
            }
        }
        content()
    }
}
