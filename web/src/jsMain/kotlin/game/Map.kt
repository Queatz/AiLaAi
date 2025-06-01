package game

import com.queatz.db.GameObject
import com.queatz.db.GameMusic
import com.queatz.db.GameTile
import lib.*
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlin.js.asDynamic

class Map(private val scene: Scene) {
    val tilemapEditor: TilemapEditor
    val post: Post
    val camera: Camera
    var world: World = World(scene)
    /** Manager for sketch tool lines and layers */
    val sketchManager: SketchManager
    /** Single source of truth for tool state */
    val toolState = ToolState()

    /** Compatibility property for isSketching - delegates to toolState */
    var isSketching: Boolean
        get() = toolState.isSketching
        set(value) {
            if (value) {
                toolState.selectTool(ToolType.SKETCH)
            } else {
                // Only change sketching state, not the selected tool
                if (toolState.isSketching) {
                    toolState.selectTool(null as ToolType?)
                }
            }
        }

    // Weather effects
    private val snowEffect = SnowEffect(scene)
    private val rainEffect = RainEffect(scene)
    private val dustEffect = DustEffect(scene)
    // Compose-observable state for weather effect toggles
    private var snowEffectEnabledState by mutableStateOf(false)
    private var rainEffectEnabledState by mutableStateOf(false)
    private var dustEffectEnabledState by mutableStateOf(false)
    // Whether camera uses orthographic projection (FOV slider acts as scale in orthographic mode)
    private var orthographicEnabledState by mutableStateOf(false)

    var pixelSize = 1
        set(value) {
            if (field != value) {
                field = value
                game?.engine?.setHardwareScalingLevel(value)
            }
        }

    // Reference to the parent Game instance
    var game: Game? = null
        set(value) {
            field = value
            // Update the tilemapEditor's editable property based on the game's editable property
            value?.let { tilemapEditor.editable = it.editable }
        }

    // Reference to the player
    private lateinit var player: Player

    // Flow to track tilemap changes
    private val _tilemapChanges = MutableSharedFlow<Unit>(extraBufferCapacity = 1)
    val tilemapChanges: SharedFlow<Unit> = _tilemapChanges.asSharedFlow()

    // Controls whether to use linear (NEAREST) or trilinear sampling for textures
    var linearSamplingEnabled: Boolean = false
        set(value) {
            if (field != value) {
                field = value
                // Notify the tilemap to update its textures if needed
                tilemapEditor.tilemap.updateTextureSamplingMode(value)
                // Notify the player to update its textures if needed
                player.updateTextureSamplingMode(value)
                // Notify the post-processing effects to update their textures if needed
                post.updateTextureSamplingMode(value)
            }
        }

    init {
        val tilemap = Tilemap(scene, world.sun, { mesh ->
            world.addShadowCaster(mesh)
        }, { mesh ->
            world.removeShadowCaster(mesh)
        })
        val camera = Camera(scene) { tilemap.mesh }
        val post = Post(scene, camera)
        val tilemapEditor = TilemapEditor(scene, tilemap, toolState, this)
        // Initialize TilemapEditor properties with values from toolState
        tilemapEditor.side = toolState.side
        tilemapEditor.drawPlane = toolState.drawPlane
        tilemapEditor.drawPlaneOffset = toolState.drawPlaneOffset
        // No need to set drawMode as it now directly delegates to toolState.drawMode
        val player = Player(scene, linearSamplingEnabled)
        world.addShadowCaster(player.mesh) // todo move into Player class, also shadow needs to face light

        this.tilemapEditor = tilemapEditor
        this.post = post
        this.camera = camera
        this.player = player
        sketchManager = SketchManager(scene)

        var isDrawing = false
        val walk = Vector3.Zero()

        // Input
        scene.onKeyboardObservable.add(fun(event: KeyboardInfo) {
            // Cancel sketch line on Escape before pointer up
            if (toolState.isSketching && event.type == KeyboardEventTypes.KEYDOWN && (event.event.key == "Escape" || event.event.key == "Esc")) {
                sketchManager.cancelCurrentLine()
                return
            }
            // Stop animation if it's playing on keyboard input (except spacebar)
            if (event.event.key == " " || event.event.key == "Space") {
                if (event.type == KeyboardEventTypes.KEYDOWN && !event.event.repeat) {
                    if (tilemapEditor.toolState.selectedToolType == null) {
                        game?.togglePlayback()
                    }
                }
            } else {
                game?.let { g ->
                    if (g.isPlaying()) {
                        g.pause()
                    }
                }
            }

            // todo if Tab, toggle camera view
            if (listOf("w", "a", "s", "d", "q", "r").contains(event.event.key)) {
                if (event.type == KeyboardEventTypes.KEYDOWN) {
                    when (event.event.key) {
                        "w" -> walk.y = 1f
                        "s" -> walk.y = -1f
                        "a" -> walk.x = -1f
                        "d" -> walk.x = 1f
                        "r" -> tilemapEditor.toggleAutoRotate()
                    }
                } else if (event.type == KeyboardEventTypes.KEYUP) {
                    when (event.event.key) {
                        "w", "s" -> walk.y = 0f
                        "a", "d" -> walk.x = 0f
                    }
                }
                return
            }

            if ((event.event.key == "ArrowLeft" || event.event.key == "ArrowRight") && event.event.altKey) {
                if (event.type == KeyboardEventTypes.KEYDOWN) {
                    // Detach camera control when Alt + Arrow is pressed
                    camera.camera.detachControl()
                } else if (event.type == KeyboardEventTypes.KEYUP) {
                    // Reattach camera control when Alt + Arrow is released
                    camera.camera.attachControl()
                }
                event.event.preventDefault()
                return
            }

            if (event.type == KeyboardEventTypes.KEYDOWN) {
                // todo press A to switch side of tile (6 sides total)
                when (event.event.key) {
                    "Tab" -> {
                        camera.toggleView(event.event.shiftKey)
                        event.event.preventDefault()
                    }

                    " " -> {
                        // Only handle spacebar for editor functions if we're in edit mode
                        // and have a tile or object selected, or if sketch tool is active
                        // (otherwise let it be used for play/pause)
                        if (game?.editable == true &&
                            (tilemapEditor.currentGameTile != null || tilemapEditor.currentGameObject != null || toolState.isSketching)
                        ) {
                            if (event.event.ctrlKey) {
                                tilemapEditor.toggleSide(event.event.shiftKey)
                            } else {
                                tilemapEditor.togglePlane(event.event.shiftKey)
                            }
                            // Prevent the event from being used for play/pause
                            event.event.preventDefault()
                        }
                    }

                    "[" -> tilemapEditor.adjustPlane(-1)
                    "]" -> tilemapEditor.adjustPlane(1)
                }
            }
        })

        scene.onPointerObservable.add(fun(event: PointerInfo) {
            // Sketch tool captures all pointer events
            if (toolState.isSketching) {
                // Handle ctrl+click to set drawing plane offset
                if (event.event.ctrlKey && event.type == PointerEventTypes.POINTERDOWN) {
                    tilemapEditor.pickAdjust()
                    return
                }

                // Allow camera manipulation when holding shift
                if (event.event.shiftKey) {
                    camera.camera.attachControl()
                    camera.isMoving = true
                    camera.recenter()
                    return
                }

                // On sketch start, detach camera control
                if (event.type == PointerEventTypes.POINTERDOWN) {
                    camera.camera.detachControl()
                }
                // Handle sketch drawing or erasing
                sketchManager.handlePointer(
                    event = event,
                    drawPlane = tilemapEditor.drawPlane,
                    offset = tilemapEditor.drawPlaneOffset,
                    eraser = event.event.altKey  // Pass ALT key state for eraser mode
                )
                // On pointer up, re-enable camera control
                if (event.type == PointerEventTypes.POINTERUP) {
                    camera.camera.attachControl()
                }
                return
            }

            when (event.type) {
                PointerEventTypes.POINTERWHEEL -> {
                    // Auto recenter camera target on mousewheel zoom
                    if (camera.view == CameraView.Free) {
                        camera.recenter()
                    }

                    // Stop animation if it's playing
                    game?.let { g ->
                        if (g.isPlaying()) {
                            g.pause()
                        }
                    }
                    return
                }

                PointerEventTypes.POINTERUP -> {
                    camera.isMoving = false
                    isDrawing = false
                    camera.camera.attachControl()
                    return
                }

                PointerEventTypes.POINTERDOWN, PointerEventTypes.POINTERMOVE -> {
                    if (camera.isMoving) return

                    if (event.type != PointerEventTypes.POINTERDOWN && !isDrawing) {
                        return
                    }

                    if (event.event.shiftKey) {
                        camera.camera.attachControl()
                        camera.isMoving = true
                        camera.recenter()
                        return
                    } else {
                        camera.camera.detachControl()
                        isDrawing = true
                    }

                    // Only allow drawing and adjusting if the game is editable
                    if (game?.editable == true) {
                        if (event.event.ctrlKey) {
                            tilemapEditor.pickAdjust()
                            return
                        }

                        // For Clone mode and LINE tool, only call draw on POINTERDOWN to prevent unwanted operations during mouse movement
                        if (event.type == PointerEventTypes.POINTERDOWN ||
                            (event.type == PointerEventTypes.POINTERMOVE && 
                             tilemapEditor.drawMode != DrawMode.Clone && 
                             toolState.selectedToolType != ToolType.LINE)
                        ) {
                            tilemapEditor.draw(event.event.altKey)
                        }
                    }
                }

                else -> {} // Handle other pointer events if needed
            }
        })

        scene.registerBeforeRender {
            if (scene.getFrameId() <= 1) {
                return@registerBeforeRender
            }

            if (scene.deltaTime < 1000) {
                when (camera.view) {
                    CameraView.Free -> {
                        player.mesh.isVisible = false
                        camera.walk(walk)
                    }

                    CameraView.Player -> {
                        camera.walk(walk)
                        player.walk(walk)
                        player.mesh.isVisible = true
                        camera.camera.setTarget(player.mesh.position.add(Vector3(0f, .25f, 0f)))
                    }

                    CameraView.Eye -> {
                        player.mesh.isVisible = false
                        camera.walk(walk)
                        player.walk(walk)
                        camera.camera.setTarget(player.mesh.position.add(Vector3(0f, .25f, 0f)))
                        camera.camera.radius = .1f
                    }
                }
            }

            camera.update()
            world.update()
            // Update the tilemap editor which will handle cursor visibility based on sketching state
            tilemapEditor.update()
            post.update()
        }
    }

    /** Returns whether camera is in orthographic mode */
    fun isOrthographicEnabled(): Boolean = orthographicEnabledState

    /** Enable or disable orthographic projection for the camera */
    fun setOrthographicEnabled(enabled: Boolean) {
        orthographicEnabledState = enabled
        scene.activeCamera?.let { cam ->
            val dynamicCam = cam.asDynamic()
            if (enabled) {
                dynamicCam.mode = 1
                val scale = (dynamicCam.orthoRight as? Float) ?: (cam.fov * 10f)
                dynamicCam.orthoLeft = -scale
                dynamicCam.orthoRight = scale
                dynamicCam.orthoTop = scale
                dynamicCam.orthoBottom = -scale
            } else {
                dynamicCam.mode = 0
            }
        }
    }

    // todo refactor
    fun set(property: String, value: String) {
        when (property) {
            "brushSize" -> tilemapEditor.brushSize = value.toIntOrNull()?.coerceIn(1, 100) ?: 1
            "brushDensity" -> tilemapEditor.brushDensity = value.toIntOrNull()?.coerceIn(1, 100) ?: 50
            "gridSize" -> tilemapEditor.gridSize = value.toIntOrNull()?.coerceIn(11, 101) ?: 51
            "fov" -> {
                val floatVal = (value.toFloatOrNull()?.coerceIn(0.25f, 2f) ?: 0.5f)
                if (orthographicEnabledState) {
                    scene.activeCamera?.let { cam ->
                        val floatVal = floatVal * 10f
                        val dynamicCam = cam.asDynamic()
                        dynamicCam.orthoLeft = -floatVal
                        dynamicCam.orthoRight = floatVal
                        dynamicCam.orthoTop = floatVal
                        dynamicCam.orthoBottom = -floatVal
                    }
                } else {
                    scene.activeCamera?.let { it.fov = floatVal }
                }
            }
            "orthographic" -> setOrthographicEnabled(value.lowercase() == "true")
            "backgroundColor" -> {
                // Parse the color string in format "r,g,b,a" where each component is a float between 0 and 1
                val parts = value.split(",").mapNotNull { it.trim().toFloatOrNull()?.coerceIn(0f, 1f) }
                if (parts.size >= 3) {
                    val r = parts[0]
                    val g = parts[1]
                    val b = parts[2]
                    val a = if (parts.size >= 4) parts[3] else 1f
                    scene.clearColor = Color4(r, g, b, a)
                }
            }
            "snowEffectEnabled" -> {
                val enabled = value.lowercase() == "true"
                setSnowEffectEnabled(enabled)
            }
            "rainEffectEnabled" -> {
                val enabled = value.lowercase() == "true"
                setRainEffectEnabled(enabled)
            }
            "dustEffectEnabled" -> {
                val enabled = value.lowercase() == "true"
                setDustEffectEnabled(enabled)
            }
        }
    }

    // todo refactor
    fun set(property: String, value: Number) {
        when (property) {
            "brushSize" -> tilemapEditor.brushSize = value.toInt().coerceIn(1, 100)
            "brushDensity" -> tilemapEditor.brushDensity = value.toInt().coerceIn(1, 100)
            "gridSize" -> tilemapEditor.gridSize = value.toInt().coerceIn(11, 101)
            "gridLineAlpha" -> tilemapEditor.gridLineAlpha = value.toInt().coerceIn(1, 10)
            "fov" -> {
                val floatVal = value.toFloat().coerceIn(0.25f, 2f)
                if (orthographicEnabledState) {
                    scene.activeCamera?.let { cam ->
                        val floatVal = floatVal * 10f
                        val dynamicCam = cam.asDynamic()
                        dynamicCam.orthoLeft = -floatVal
                        dynamicCam.orthoRight = floatVal
                        dynamicCam.orthoTop = floatVal
                        dynamicCam.orthoBottom = -floatVal
                    }
                } else {
                    scene.activeCamera?.let { it.fov = floatVal }
                }
            }
            "backgroundColorR" -> {
                val r = value.toFloat().coerceIn(0f, 1f)
                scene.clearColor = Color4(r, scene.clearColor.g, scene.clearColor.b, scene.clearColor.a)
            }
            "backgroundColorG" -> {
                val g = value.toFloat().coerceIn(0f, 1f)
                scene.clearColor = Color4(scene.clearColor.r, g, scene.clearColor.b, scene.clearColor.a)
            }
            "backgroundColorB" -> {
                val b = value.toFloat().coerceIn(0f, 1f)
                scene.clearColor = Color4(scene.clearColor.r, scene.clearColor.g, b, scene.clearColor.a)
            }
            "backgroundColorA" -> {
                val a = value.toFloat().coerceIn(0f, 1f)
                scene.clearColor = Color4(scene.clearColor.r, scene.clearColor.g, scene.clearColor.b, a)
            }
            "ambienceIntensity" -> {
                val intensity = value.toFloat().coerceIn(0f, 1f)
                world.ambience.intensity = intensity
            }
            "sunIntensity" -> {
                val intensity = value.toFloat().coerceIn(0f, 10f)
                world.sun.intensity = intensity
            }
            "fogDensity" -> {
                val density = value.toFloat().coerceIn(0f, 0.1f)
                scene.fogDensity = density
            }
            "timeOfDay" -> {
                val time = value.toFloat().coerceIn(0f, 1f)
                world.timeOfDay = time
            }
            "snowEffectIntensity" -> {
                val intensity = value.toFloat().coerceIn(0f, 1f)
                setSnowEffectIntensity(intensity)
            }
            "rainEffectIntensity" -> {
                val intensity = value.toFloat().coerceIn(0f, 1f)
                setRainEffectIntensity(intensity)
            }
            "dustEffectIntensity" -> {
                val intensity = value.toFloat().coerceIn(0f, 1f)
                setDustEffectIntensity(intensity)
            }
        }
    }

    /**
     * Sets the current GameTile to paint with
     */
    fun setCurrentGameTile(gameTile: GameTile?) {
        toolState.setCurrentGameTile(gameTile)
        // TilemapEditor still needs to know about the current tile
        tilemapEditor.currentGameTile = gameTile
    }

    /**
     * Sets the current GameObject to place
     */
    fun setCurrentGameObject(gameObject: GameObject?) {
        toolState.setCurrentGameObject(gameObject)
        // TilemapEditor still needs to know about the current object
        tilemapEditor.currentGameObject = gameObject
    }

    /**
     * Gets the red component of the background color
     */
    fun getBackgroundColorR(): Float {
        return scene.clearColor.r
    }

    /**
     * Gets the green component of the background color
     */
    fun getBackgroundColorG(): Float {
        return scene.clearColor.g
    }

    /**
     * Gets the blue component of the background color
     */
    fun getBackgroundColorB(): Float {
        return scene.clearColor.b
    }

    /**
     * Gets the alpha component of the background color
     */
    fun getBackgroundColorA(): Float {
        return scene.clearColor.a
    }

    /**
     * Gets the ambience intensity
     */
    fun getAmbienceIntensity(): Float {
        return world.ambience.intensity
    }

    /**
     * Gets the sun intensity
     */
    fun getSunIntensity(): Float {
        return world.sun.intensity
    }

    /**
     * Gets the fog density
     */
    fun getFogDensity(): Float {
        return scene.fogDensity
    }

    /**
     * Gets the time of day (0 = midnight, 0.5 = noon, 1 = end of day)
     */
    fun getTimeOfDay(): Float {
        return world.timeOfDay
    }

    /**
     * Sets whether the snow effect is enabled
     */
    fun setSnowEffectEnabled(enabled: Boolean) {
        snowEffect.setEnabled(enabled)
        snowEffectEnabledState = enabled
    }

    /**
     * Gets whether the snow effect is enabled
     */
    fun isSnowEffectEnabled(): Boolean {
        return snowEffectEnabledState
    }

    /**
     * Sets the intensity of the snow effect (0-1)
     */
    fun setSnowEffectIntensity(intensity: Float) {
        snowEffect.intensity = intensity
    }

    /**
     * Gets the intensity of the snow effect
     */
    fun getSnowEffectIntensity(): Float {
        return snowEffect.intensity
    }

    /**
     * Sets whether the rain effect is enabled
     */
    fun setRainEffectEnabled(enabled: Boolean) {
        rainEffect.setEnabled(enabled)
        rainEffectEnabledState = enabled
    }

    /**
     * Gets whether the rain effect is enabled
     */
    fun isRainEffectEnabled(): Boolean {
        return rainEffectEnabledState
    }

    /**
     * Sets the intensity of the rain effect (0-1)
     */
    fun setRainEffectIntensity(intensity: Float) {
        rainEffect.intensity = intensity
    }

    /**
     * Gets the intensity of the rain effect
     */
    fun getRainEffectIntensity(): Float {
        return rainEffect.intensity
    }

    /**
     * Sets whether the dust effect is enabled
     */
    fun setDustEffectEnabled(enabled: Boolean) {
        dustEffect.setEnabled(enabled)
        dustEffectEnabledState = enabled
    }

    /**
     * Gets whether the dust effect is enabled
     */
    fun isDustEffectEnabled(): Boolean {
        return dustEffectEnabledState
    }

    /**
     * Sets the intensity of the dust effect (0-1)
     */
    fun setDustEffectIntensity(intensity: Float) {
        dustEffect.intensity = intensity
    }

    /**
     * Gets the intensity of the dust effect
     */
    fun getDustEffectIntensity(): Float {
        return dustEffect.intensity
    }

    /**
     * Sets the current GameMusic to play
     */
    fun setCurrentGameMusic(gameMusic: GameMusic?) {
        toolState.setCurrentGameMusic(gameMusic)
        // TilemapEditor still needs to know about the current music
        tilemapEditor.currentGameMusic = gameMusic
        tilemapEditor.currentGameTile = null
        tilemapEditor.currentGameObject = null
    }

    /**
     * Notifies that the tilemap has changed.
     * This should be called whenever the tilemap is modified in any way.
     */
    fun notifyTilemapChanged() {
        _tilemapChanges.tryEmit(Unit)
    }
}
