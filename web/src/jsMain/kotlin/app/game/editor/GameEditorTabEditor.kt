package app.game.editor

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import api
import app.ailaai.api.updateGameScene
import app.game.GameObjectData
import app.game.GameObjectsData
import app.game.GameTileData
import app.game.GameTilesData
import app.game.json
import com.queatz.db.AnimationMarkerData
import com.queatz.db.CameraKeyframeData
import com.queatz.db.Color4Data
import com.queatz.db.GameScene
import com.queatz.db.GameSceneConfig
import com.queatz.db.Vector3Data
import game.AnimationMarker
import game.CameraKeyframe
import game.Map
import kotlinx.browser.window
import kotlinx.coroutines.launch
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import lib.Engine
import lib.Vector3
import org.jetbrains.compose.web.attributes.disabled
import org.jetbrains.compose.web.css.DisplayStyle
import org.jetbrains.compose.web.css.display
import org.jetbrains.compose.web.css.marginRight
import org.jetbrains.compose.web.css.padding
import org.jetbrains.compose.web.dom.Button
import org.jetbrains.compose.web.dom.Div
import org.jetbrains.compose.web.dom.Text
import r

@Composable
fun GameEditorTabEditor(
    engine: Engine, 
    map: Map, 
    gameScene: GameScene? = null,
    onSceneDeleted: () -> Unit = {},
    onPixelatedChanged: (Boolean) -> Unit = {}
) {
    // Create state variables to track when to clear selections
    var clearTileSelection by remember { mutableStateOf(false) }
    var clearObjectSelection by remember { mutableStateOf(false) }
    var isSaving by remember { mutableStateOf(false) }
    var currentPixelSize by remember { mutableStateOf<Int?>(null) }
    val scope = rememberCoroutineScope()

    // Function to convert Vector3 to Vector3Data
    fun toVector3Data(vector3: Vector3): Vector3Data {
        return Vector3Data(
            x = vector3.x,
            y = vector3.y,
            z = vector3.z
        )
    }

    // Function to convert AnimationMarker to AnimationMarkerData
    fun toAnimationMarkerData(marker: AnimationMarker): AnimationMarkerData {
        return AnimationMarkerData(
            id = marker.id,
            name = marker.name,
            time = marker.time,
            duration = marker.duration
        )
    }

    // Function to convert CameraKeyframe to CameraKeyframeData
    fun toCameraKeyframeData(keyframe: CameraKeyframe): CameraKeyframeData {
        return CameraKeyframeData(
            id = keyframe.id,
            time = keyframe.time,
            position = toVector3Data(keyframe.position),
            target = toVector3Data(keyframe.target),
            alpha = keyframe.alpha,
            beta = keyframe.beta,
            radius = keyframe.radius,
            fov = keyframe.fov
        )
    }

    // Function to save animation data to JSON
    fun saveAnimationConfigToJson(map: Map): String {
        val game = map.game ?: return ""

        // Get the background color from the scene
        val clearColor = game.scene.clearColor
        val colorArray = clearColor.asArray()
        val backgroundColor = if (colorArray.size >= 4) {
            Color4Data(
                r = colorArray[0],
                g = colorArray[1],
                b = colorArray[2],
                a = colorArray[3]
            )
        } else null

        // Create GameSceneConfig with animation data and graphics settings
        val config = GameSceneConfig(
            markers = game.animationData.markers.map { toAnimationMarkerData(it) },
            cameraKeyframes = game.animationData.cameraKeyframes.map { toCameraKeyframeData(it) },
            backgroundColor = backgroundColor,
            cameraFov = game.scene.activeCamera?.fov,
            ssaoEnabled = map.post.ssaoEnabled,
            bloomEnabled = map.post.bloomEnabled,
            sharpenEnabled = map.post.sharpenEnabled,
            colorCorrectionEnabled = map.post.colorCorrectionEnabled,
            linearSamplingEnabled = map.linearSamplingEnabled,
            depthOfFieldEnabled = map.post.depthOfFieldEnabled,
            ambienceIntensity = map.getAmbienceIntensity(),
            sunIntensity = map.getSunIntensity(),
            fogDensity = map.getFogDensity(),
            timeOfDay = map.getTimeOfDay(),
            pixelSize = currentPixelSize,
            brushSize = map.tilemapEditor.brushSize,
            brushDensity = map.tilemapEditor.brushDensity,
            gridSize = map.tilemapEditor.gridSize,
            snowEffectEnabled = map.isSnowEffectEnabled(),
            snowEffectIntensity = map.getSnowEffectIntensity(),
            rainEffectEnabled = map.isRainEffectEnabled(),
            rainEffectIntensity = map.getRainEffectIntensity(),
            dustEffectEnabled = map.isDustEffectEnabled(),
            dustEffectIntensity = map.getDustEffectIntensity()
        )

        // Serialize to JSON
        return json.encodeToString(config)
    }

    // Function to save tiles to JSON
    fun saveTilesToJson(map: Map): String {
        // Create a list to hold all tile data
        val tilesList = mutableListOf<GameTileData>()

        // Get the tile types map using the public method
        val tileTypesMap = map.tilemapEditor.tilemap.getTileTypes()

        if (tileTypesMap.isNotEmpty()) {
            // Iterate over all entries in the map
            for ((key, tileId) in tileTypesMap) {
                // Parse the key to get position and side
                val parts = key.split(",")
                if (parts.size == 4) {
                    val x = parts[0].toIntOrNull() ?: 0
                    val y = parts[1].toIntOrNull() ?: 0
                    val z = parts[2].toIntOrNull() ?: 0
                    val side = parts[3]

                    // Add the tile to the list
                    tilesList.add(
                        GameTileData(
                            x = x,
                            y = y,
                            z = z,
                            side = side,
                            tileId = tileId
                        )
                    )
                }
            }
        } else {
            // Fallback to using the current tile if the map is empty
            val currentTile = map.tilemapEditor.currentGameTile
            if (currentTile?.id != null) {
                val position = map.tilemapEditor.tilePos
                val side = map.tilemapEditor.side
                tilesList.add(
                    GameTileData(
                        x = position.x.toInt(),
                        y = position.y.toInt(),
                        z = position.z.toInt(),
                        side = side.toString(),
                        tileId = currentTile.id
                    )
                )
            }
        }

        // Create a GameTilesData object with the list of tiles
        val tilesData = GameTilesData(tilesList)

        // Serialize to JSON using the json constant
        return json.encodeToString(GameTilesData.serializer(), tilesData)
    }

    // Function to save objects to JSON
    fun saveObjectsToJson(map: Map): String {
        // Create a list to hold all object data
        val objectsList = mutableListOf<GameObjectData>()

        // Get the object types map using the public method
        val objectTypesMap = map.tilemapEditor.tilemap.getObjectTypes()

        if (objectTypesMap.isNotEmpty()) {
            // Iterate over all entries in the map
            for ((key, objectId) in objectTypesMap) {
                // Parse the key to get position and side
                val parts = key.split(",")
                if (parts.size == 4) {
                    val x = parts[0].toIntOrNull() ?: 0
                    val y = parts[1].toIntOrNull() ?: 0
                    val z = parts[2].toIntOrNull() ?: 0
                    val side = parts[3]

                    // Add the object to the list
                    objectsList.add(
                        GameObjectData(
                            x = x,
                            y = y,
                            z = z,
                            side = side,
                            objectId = objectId
                        )
                    )
                }
            }
        } else {
            // Fallback to using the current object if the map is empty
            val currentObject = map.tilemapEditor.currentGameObject
            if (currentObject?.id != null) {
                val position = map.tilemapEditor.tilePos
                val side = map.tilemapEditor.side
                objectsList.add(
                    GameObjectData(
                        x = position.x.toInt(),
                        y = position.y.toInt(),
                        z = position.z.toInt(),
                        side = side.toString(),
                        objectId = currentObject.id
                    )
                )
            }
        }

        // Create a GameObjectsData object with the list of objects
        val objectsData = GameObjectsData(objectsList)

        // Serialize to JSON using the json constant
        return json.encodeToString(GameObjectsData.serializer(), objectsData)
    }

    Div({
        style {
            padding(1.r)
        }
    }) {
        // Add Save button at the top of the editor tab
        Div({
            style {
                display(DisplayStyle.Flex)
                padding(0.r, 0.r, 0.5.r, 0.r)
            }
        }) {
            Button({
                classes(Styles.button)
                style {
                    marginRight(0.5.r)
                }
                // Disable the button when saving is in progress
                if (isSaving) {
                    disabled()
                }
                onClick {
                    if (gameScene?.id != null && !isSaving) {
                        // Set saving state to true
                        isSaving = true
                        scope.launch {
                            try {
                                // Save the current state of tiles, objects, and animation data
                                val tilesJson = saveTilesToJson(map)
                                val objectsJson = saveObjectsToJson(map)
                                val configJson = saveAnimationConfigToJson(map)

                                // Update the GameScene with the new data
                                val updatedGameScene = gameScene.copy(
                                    tiles = tilesJson,
                                    objects = objectsJson,
                                    config = configJson
                                )

                                api.updateGameScene(gameScene.id!!, updatedGameScene) {
                                    console.log("GameScene saved successfully")
                                    // Set saving state back to false after successful save
                                }
                            } catch (e: Exception) {
                                console.error("Error saving game scene: ${e.message}")
                                // Set saving state back to false if there's an error
                            }
                            isSaving = false
                        }
                    } else {
                        if (gameScene?.id == null) {
                            console.error("Cannot save: GameScene ID is null")
                        }
                    }
                }
            }) {
                Text(if (isSaving) "Saving..." else "Save")
            }

            Button({
                classes(Styles.outlineButton, Styles.outlineButtonTonal)
                style {
                    marginRight(0.5.r)
                }
                onClick {
                    if (gameScene?.id != null) {
                        // Determine the URL based on whether the scene has a custom URL or just an ID
                        val url = if (gameScene.url != null && gameScene.url!!.isNotBlank()) {
                            "/scene/${gameScene.url}"
                        } else {
                            "/scene/${gameScene.id}"
                        }
                        // Open the URL in a new tab
                        window.open(url, "_blank")
                    } else {
                        console.error("Cannot open: GameScene ID is null")
                    }
                }
            }) {
                Text("Launch scene")
            }
        }
        CurrentToolSection(map)
        BrushSection(map)

        // Pass callbacks to clear selections when the other type is selected
        TilesSection(
            map = map,
            onTileSelected = {
                // When a tile is selected, clear object selection
                clearObjectSelection = true
            },
            clearSelection = clearTileSelection
        )

        ObjectsSection(
            map = map,
            onObjectSelected = {
                // When an object is selected, clear tile selection
                clearTileSelection = true
            },
            clearSelection = clearObjectSelection
        )

        // Reset the clear flags after they've been processed
        LaunchedEffect(clearTileSelection) {
            if (clearTileSelection) {
                clearTileSelection = false
            }
        }

        LaunchedEffect(clearObjectSelection) {
            if (clearObjectSelection) {
                clearObjectSelection = false
            }
        }

        PortalsSection()
        AnimationSection(map.game)
        CameraSection(map)
        EnvironmentSection(map)
        WeatherSection(map)
        GraphicsSection(
            engine = engine,
            map = map,
            onPixelatedChanged = onPixelatedChanged,
            initialPixelSize = currentPixelSize,
            onPixelSizeChanged = { size -> currentPixelSize = size }
        )
        MusicSection(map.game, map)
        SceneSection(
            gameScene = gameScene,
            onSceneDeleted = onSceneDeleted
        )
        ExportSection(map.game)
    }
}
