package app.game.editor

import androidx.compose.runtime.State
import androidx.compose.runtime.mutableStateOf

/**
 * A class to track edits to a scene.
 * This provides a centralized way to track whether a scene has been edited
 * and what type of edit was made.
 */
class SceneEditTracker {
    private val _isEdited = mutableStateOf(false)
    val isEdited: State<Boolean> = _isEdited
    
    /**
     * Mark the scene as edited.
     * @param editType The type of edit that was made. Defaults to Generic.
     */
    fun markEdited(editType: EditType = EditType.Generic) {
        _isEdited.value = true
    }
    
    /**
     * Mark the scene as saved, resetting the edited flag.
     */
    fun markSaved() {
        _isEdited.value = false
    }
    
    /**
     * Different types of edits for logging/debugging.
     */
    enum class EditType {
        Generic, Tiles, Objects, Animation, Sketch, Properties, 
        Camera, Environment, Weather, Graphics, Music, Captions
    }
}
