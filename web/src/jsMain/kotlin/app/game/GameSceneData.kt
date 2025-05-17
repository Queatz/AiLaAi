package app.game

import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json

// Global JSON instance with lenient configuration
val json = Json {
    ignoreUnknownKeys = true
    isLenient = true
    prettyPrint = false
}

// Data class for a tile in the game scene
@Serializable
data class GameTileData(
    val x: Int = 0,
    val y: Int = 0,
    val z: Int = 0,
    val side: String = "y",
    val tileId: String? = null
)

// Data class for an object in the game scene
@Serializable
data class GameObjectData(
    val x: Int = 0,
    val y: Int = 0,
    val z: Int = 0,
    val side: String = "y",
    val objectId: String? = null
)

// Wrapper class for a collection of tiles
@Serializable
data class GameTilesData(
    val tiles: List<GameTileData> = emptyList()
)

// Wrapper class for a collection of objects
@Serializable
data class GameObjectsData(
    val objects: List<GameObjectData> = emptyList()
)
