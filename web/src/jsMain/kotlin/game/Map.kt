package game

import com.queatz.db.GameObject
import com.queatz.db.GameMusic
import com.queatz.db.GameTile
import lib.*
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue

class Map(private val scene: Scene) {
    val tilemapEditor: TilemapEditor
    val post: Post
    val camera: Camera
    var world: World = World(scene)

    // Weather effects
    private val snowEffect = SnowEffect(scene)
    private val rainEffect = RainEffect(scene)
    private val dustEffect = DustEffect(scene)
    // Compose-observable state for weather effect toggles
    private var snowEffectEnabledState by mutableStateOf(false)
    private var rainEffectEnabledState by mutableStateOf(false)
    private var dustEffectEnabledState by mutableStateOf(false)

    // Reference to the parent Game instance
    var game: Game? = null
        set(value) {
            field = value
            // Update the tilemapEditor's editable property based on the game's editable property
            value?.let { tilemapEditor.editable = it.editable }
        }

    // Reference to the player
    private lateinit var player: Player

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
        val tilemapEditor = TilemapEditor(scene, tilemap)
        val player = Player(scene, linearSamplingEnabled)
        world.addShadowCaster(player.mesh) // todo move into Player class, also shadow needs to face light

        this.tilemapEditor = tilemapEditor
        this.post = post
        this.camera = camera
        this.player = player

        var isDrawing = false
        val walk = Vector3.Zero()

        // Input
        scene.onKeyboardObservable.add(fun(event: KeyboardInfo) {
            // Stop animation if it's playing on keyboard input (except spacebar)
            if (event.event.key == " " || event.event.key == "Space") {
                if (event.type == KeyboardEventTypes.KEYDOWN && !event.event.repeat) {
                    if (tilemapEditor.currentGameTile == null && tilemapEditor.currentGameObject == null) {
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
                        // and have a tile or object selected (otherwise let it be used for play/pause)
                        if (game?.editable == true &&
                            (tilemapEditor.currentGameTile != null || tilemapEditor.currentGameObject != null)
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
            if (event.type == PointerEventTypes.POINTERWHEEL) {
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

            if (event.type == PointerEventTypes.POINTERUP) {
                camera.isMoving = false
                isDrawing = false
                camera.camera.attachControl()
                return
            }

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

                // For Clone mode, only call draw on POINTERDOWN to prevent unwanted selections during mouse movement
                if (event.type == PointerEventTypes.POINTERDOWN || 
                    (event.type == PointerEventTypes.POINTERMOVE && tilemapEditor.drawMode != DrawMode.Clone)) {
                    tilemapEditor.draw(event.event.altKey)
                }
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
            tilemapEditor.update()
            post.update()
        }
    }

    // todo refactor
    fun set(property: String, value: String) {
        when (property) {
            "brushSize" -> tilemapEditor.brushSize = value.toIntOrNull()?.coerceIn(1, 100) ?: 1
            "brushDensity" -> tilemapEditor.brushDensity = value.toIntOrNull()?.coerceIn(1, 100) ?: 50
            "gridSize" -> tilemapEditor.gridSize = value.toIntOrNull()?.coerceIn(11, 101) ?: 51
            "fov" -> scene.activeCamera?.let { it.fov = value.toFloatOrNull()?.coerceIn(0.25f, 2f) ?: 0.5f }
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
            "fov" -> scene.activeCamera?.let { it.fov = value.toFloat().coerceIn(0.25f, 2f) }
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
        tilemapEditor.currentGameTile = gameTile
        // Switch to tile mode when an tile is selected
        if (gameTile != null) {
            tilemapEditor.drawMode = DrawMode.Tile
            // Clear the current game object when a tile is selected
            tilemapEditor.currentGameObject = null
        }
    }

    /**
     * Sets the current GameObject to place
     */
    fun setCurrentGameObject(gameObject: GameObject?) {
        tilemapEditor.currentGameObject = gameObject
        // Switch to object mode when an object is selected
        if (gameObject != null) {
            tilemapEditor.drawMode = DrawMode.Object
            // Clear the current game tile when an object is selected
            tilemapEditor.currentGameTile = null
        }
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
        tilemapEditor.currentGameMusic = gameMusic
        tilemapEditor.currentGameTile = null
        tilemapEditor.currentGameObject = null
    }
}
