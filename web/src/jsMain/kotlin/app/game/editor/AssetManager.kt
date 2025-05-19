package app.game.editor

import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import com.queatz.db.GameObject
import com.queatz.db.GameMusic
import com.queatz.db.GameTile
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update

/**
 * A manager for game assets (tiles, objects, music)
 * This class keeps track of all assets and provides methods to update them
 * It uses StateFlow to notify subscribers when assets are updated
 */
class AssetManager {
    // State flows for each asset type
    private val _tiles = MutableStateFlow<List<GameTile>>(emptyList())
    private val _objects = MutableStateFlow<List<GameObject>>(emptyList())
    private val _music = MutableStateFlow<List<GameMusic>>(emptyList())

    // Public getters for the state flows
    val tiles: StateFlow<List<GameTile>> = _tiles.asStateFlow()
    val objects: StateFlow<List<GameObject>> = _objects.asStateFlow()
    val music: StateFlow<List<GameMusic>> = _music.asStateFlow()

    /**
     * Set the list of tiles
     */
    fun setTiles(tiles: List<GameTile>) {
        _tiles.value = tiles
    }

    /**
     * Set the list of objects
     */
    fun setObjects(objects: List<GameObject>) {
        _objects.value = objects
    }

    /**
     * Set the list of music
     */
    fun setMusic(music: List<GameMusic>) {
        _music.value = music
    }

    /**
     * Update a tile
     * @param tile The updated tile
     */
    fun updateTile(tile: GameTile) {
        _tiles.update { tiles ->
            tiles.map { if (it.id == tile.id) tile else it }
        }
    }

    /**
     * Update an object
     * @param gameObject The updated object
     */
    fun updateObject(gameObject: GameObject) {
        _objects.update { objects ->
            objects.map { if (it.id == gameObject.id) gameObject else it }
        }
    }

    /**
     * Update a music
     * @param music The updated music
     */
    fun updateMusic(music: GameMusic) {
        _music.update { musicList ->
            musicList.map { if (it.id == music.id) music else it }
        }
    }

    /**
     * Add a tile
     * @param tile The tile to add
     */
    fun addTile(tile: GameTile) {
        _tiles.update { tiles ->
            listOf(tile) + tiles
        }
    }

    /**
     * Add an object
     * @param gameObject The object to add
     */
    fun addObject(gameObject: GameObject) {
        _objects.update { objects ->
            listOf(gameObject) + objects
        }
    }

    /**
     * Add a music
     * @param music The music to add
     */
    fun addMusic(music: GameMusic) {
        _music.update { musicList ->
            listOf(music) + musicList
        }
    }

    /**
     * Remove a tile
     * @param id The ID of the tile to remove
     */
    fun removeTile(id: String) {
        _tiles.update { tiles ->
            tiles.filter { it.id != id }
        }
    }

    /**
     * Remove an object
     * @param id The ID of the object to remove
     */
    fun removeObject(id: String) {
        _objects.update { objects ->
            objects.filter { it.id != id }
        }
    }

    /**
     * Remove a music
     * @param id The ID of the music to remove
     */
    fun removeMusic(id: String) {
        _music.update { musicList ->
            musicList.filter { it.id != id }
        }
    }
}

/**
 * Singleton instance of the AssetManager
 */
val assetManager = AssetManager()

/**
 * Composable function to get the current list of tiles
 */
@Composable
fun rememberTiles(): List<GameTile> {
    val tiles by assetManager.tiles.collectAsState()
    return tiles
}

/**
 * Composable function to get the current list of objects
 */
@Composable
fun rememberObjects(): List<GameObject> {
    val objects by assetManager.objects.collectAsState()
    return objects
}

/**
 * Composable function to get the current list of music
 */
@Composable
fun rememberMusic(): List<GameMusic> {
    val music by assetManager.music.collectAsState()
    return music
}
