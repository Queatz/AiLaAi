package app.game.editor

import com.queatz.db.GameTile
import com.queatz.db.GameObject
import com.queatz.db.GameMusic
import kotlin.time.Instant

/**
 * Generic interface for game assets that can be displayed in the AssetSection
 */
interface Asset {
    val id: String
    val name: String
    val content: String
    val createdAt: Instant?
    val description: String
}

/**
 * Adapter for GameTile to implement the Asset interface
 */
class GameTileAsset(private val gameTile: GameTile) : Asset {
    override val id: String get() = gameTile.id ?: ""
    override val name: String get() = gameTile.name ?: "Tile"
    override val content: String get() = gameTile.photo ?: ""
    override val createdAt: Instant? get() = gameTile.createdAt
    override val description: String get() = "Created: ${gameTile.createdAt}"
}

/**
 * Adapter for GameObject to implement the Asset interface
 */
class GameObjectAsset(private val gameObject: GameObject) : Asset {
    override val id: String get() = gameObject.id ?: ""
    override val name: String get() = gameObject.name ?: "Object"
    override val content: String get() = gameObject.photo ?: ""
    override val createdAt: Instant? get() = gameObject.createdAt
    override val description: String get() = "Size: ${gameObject.width}x${gameObject.height}"
}

/**
 * Adapter for GameMusic to implement the Asset interface
 */
class GameMusicAsset(private val gameMusic: GameMusic) : Asset {
    override val id: String get() = gameMusic.id ?: ""
    override val name: String get() = gameMusic.name ?: "Music"
    override val content: String get() = gameMusic.audio ?: ""
    override val createdAt: Instant? get() = gameMusic.createdAt
    override val description: String get() = "Duration: ${gameMusic.duration ?: "Unknown"} seconds"
}
