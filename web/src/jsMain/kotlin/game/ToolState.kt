package game

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import com.queatz.db.GameObject
import com.queatz.db.GameTile
import com.queatz.db.GameMusic

/**
 * A single data class that holds the entire state of the current tool.
 */
data class ToolState(
    // The selected tool type, or null if no tool is selected
    private val _selectedToolType: ToolType? = null,

    // The current game tile to paint with (null if not in tile mode)
    private val _currentGameTile: GameTile? = null,

    // The current game object to place (null if not in object mode)
    private val _currentGameObject: GameObject? = null,

    // The current game music to play (null if not selected)
    private val _currentGameMusic: GameMusic? = null,

    // The current draw mode
    private val _drawMode: DrawMode = DrawMode.Tile,

    // Whether sketching mode is active
    private val _isSketching: Boolean = false,

    // The current drawing plane
    private val _drawPlane: Side = Side.Y,

    // The offset of the drawing plane
    private val _drawPlaneOffset: Int = 0,

    // The side to draw on
    private val _side: Side = Side.Y
) {
    // Observable state properties
    var selectedToolType by mutableStateOf(_selectedToolType)
        private set

    var currentGameTile by mutableStateOf(_currentGameTile)
        private set

    var currentGameObject by mutableStateOf(_currentGameObject)
        private set

    var currentGameMusic by mutableStateOf(_currentGameMusic)
        private set

    var drawMode by mutableStateOf(_drawMode)
        private set

    var isSketching by mutableStateOf(_isSketching)
        private set

    var drawPlane by mutableStateOf(_drawPlane)
        private set

    var drawPlaneOffset by mutableStateOf(_drawPlaneOffset)
        private set

    var side by mutableStateOf(_side)
        private set

    // Helper methods to update the state

    /**
     * Selects a tool by type
     */
    fun selectTool(toolType: ToolType?) {
        selectedToolType = toolType

        when (toolType) {
            ToolType.DRAW -> {
                drawMode = DrawMode.Tile
                isSketching = false
            }
            ToolType.CLONE -> {
                drawMode = DrawMode.Clone
                isSketching = false
            }
            ToolType.SKETCH -> {
                isSketching = true
            }
            null -> {
                // Deselect current tool
                currentGameTile = null
                currentGameObject = null
                drawMode = DrawMode.Tile
                isSketching = false
            }
        }
    }

    /**
     * Selects a tool by ID (string)
     * Backward compatibility method
     */
    fun selectTool(toolId: String?) {
        selectTool(ToolType.fromString(toolId))
    }

    /**
     * Sets the current game tile and updates the draw mode
     */
    fun setCurrentGameTile(gameTile: GameTile?) {
        currentGameTile = gameTile

        if (gameTile != null) {
            drawMode = DrawMode.Tile
            currentGameObject = null
            currentGameMusic = null
            selectedToolType = ToolType.DRAW
        }
    }

    /**
     * Sets the current game object and updates the draw mode
     */
    fun setCurrentGameObject(gameObject: GameObject?) {
        currentGameObject = gameObject

        if (gameObject != null) {
            drawMode = DrawMode.Object
            currentGameTile = null
            currentGameMusic = null
            selectedToolType = ToolType.DRAW
        }
    }

    /**
     * Sets the current game music
     */
    fun setCurrentGameMusic(gameMusic: GameMusic?) {
        currentGameMusic = gameMusic

        if (gameMusic != null) {
            currentGameTile = null
            currentGameObject = null
        }
    }

    /**
     * Updates the drawing plane
     */
    fun setDrawPlane(plane: Side, offset: Int) {
        drawPlane = plane
        drawPlaneOffset = offset
    }

    /**
     * Updates the drawing plane offset
     */
    fun adjustDrawPlaneOffset(distance: Int) {
        drawPlaneOffset += distance
    }

    /**
     * Updates the side to draw on
     */
    fun setSide(newSide: Side) {
        side = newSide
    }

    /**
     * Toggles between drawing planes in sequence
     */
    fun togglePlane(isReverse: Boolean) {
        drawPlane = when (drawPlane.abs) {
            Side.X -> if (isReverse) Side.Z else Side.Y
            Side.Y -> if (isReverse) Side.X else Side.Z
            Side.Z -> if (isReverse) Side.Y else Side.X
            Side.NEGATIVE_X -> if (isReverse) Side.NEGATIVE_Z else Side.NEGATIVE_Y
            Side.NEGATIVE_Y -> if (isReverse) Side.NEGATIVE_X else Side.NEGATIVE_Z
            Side.NEGATIVE_Z -> if (isReverse) Side.NEGATIVE_Y else Side.NEGATIVE_X
        }
    }

    /**
     * Toggles between sides in sequence
     */
    fun toggleSide(isReverse: Boolean) {
        if (side != drawPlane) {
            side = when (drawPlane.abs) {
                Side.Y -> if (side == Side.Y) Side.Z else Side.Y
                Side.X -> if (side == Side.X) Side.Y else Side.X
                Side.Z -> if (side == Side.Z) Side.X else Side.Z
                Side.NEGATIVE_Y -> if (side == Side.NEGATIVE_Y) Side.NEGATIVE_Z else Side.NEGATIVE_Y
                Side.NEGATIVE_X -> if (side == Side.NEGATIVE_X) Side.NEGATIVE_Y else Side.NEGATIVE_X
                Side.NEGATIVE_Z -> if (side == Side.NEGATIVE_Z) Side.NEGATIVE_X else Side.NEGATIVE_Z
            }
        } else if (isReverse) {
            side = when (side) {
                Side.Y -> Side.X
                Side.Z -> Side.Y
                Side.X -> Side.Z
                Side.NEGATIVE_Y -> Side.NEGATIVE_X
                Side.NEGATIVE_Z -> Side.NEGATIVE_Y
                Side.NEGATIVE_X -> Side.NEGATIVE_Z
            }
        } else {
            side = when (side) {
                Side.Y -> Side.Z
                Side.Z -> Side.X
                Side.X -> Side.Y
                Side.NEGATIVE_Y -> Side.NEGATIVE_Z
                Side.NEGATIVE_Z -> Side.NEGATIVE_X
                Side.NEGATIVE_X -> Side.NEGATIVE_Y
            }
        }
    }
}
