package game

import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import com.queatz.db.MarkerEvent
import com.queatz.db.SavedPositionData
import com.queatz.db.Vector3Data
import lib.Vector3
import kotlin.js.Date

/**
 * Data class representing an animation marker
 * @param id Unique identifier for the marker
 * @param name Name of the marker
 * @param time Time in seconds where the marker is placed
 * @param duration Duration in seconds (0 = play until `end)
 */
data class AnimationMarker(
    val id: String,
    var name: String,
    var time: Double,
    var duration: Double = 0.0,
    var visible: Boolean = true,
    var event: MarkerEvent? = null
)

/**
 * Data class representing a camera keyframe
 * @param id Unique identifier for the keyframe
 * @param time Time in seconds where the keyframe is placed
 * @param position Camera position
 * @param target Camera target
 * @param alpha Camera horizontal rotation
 * @param beta Camera vertical rotation
 * @param radius Camera distance from target
 * @param fov Camera field of view
 */
data class CameraKeyframe(
    val id: String,
    var time: Double,
    val position: Vector3,
    val target: Vector3,
    val alpha: Float,
    val beta: Float,
    val radius: Float,
    var fov: Float
)

/**
 * Class to manage animation data
 */
class AnimationData {
    // Captions attached to the animation timeline (composable)
    private val _captions = mutableStateListOf<GameCaption>()
    val captions: List<GameCaption> @Composable get() = _captions.sortedBy { it.time }
    /** Non-composable accessor for all captions */
    fun getAllCaptions(): List<GameCaption> = _captions.toList()

    /**
     * Add a new caption at the current time
     * @param text The caption text to display
     * @param duration Duration in seconds to display after typing (default 5 seconds)
     */
    fun addCaption(text: String, duration: Double = 5.0): GameCaption {
        val id = "caption_${Date().getTime().toLong()}"
        val caption = GameCaption(id, currentTime, text, duration)
        _captions.add(caption)
        return caption
    }

    /**
     * Remove a caption by id
     */
    fun removeCaption(id: String) {
        _captions.removeAll { it.id == id }
    }
    /**
     * Add a caption from serialized data
     * @param id Unique identifier
     * @param time Time in seconds when the caption appears
     * @param text Caption text
     * @param duration Duration in seconds to display after typing
     */
    fun addCaptionFromData(id: String, time: Double, text: String, duration: Double) {
        val caption = GameCaption(id, time, text, duration)
        _captions.add(caption)
    }
    val _markers = mutableStateListOf<AnimationMarker>()
    val markers: List<AnimationMarker> @Composable get() = _markers.sortedBy { it.time }

    var _cameraKeyframes by mutableStateOf(mutableListOf<CameraKeyframe>())
    val cameraKeyframes: List<CameraKeyframe> @Composable get() = _cameraKeyframes.sortedBy { it.time }

    var _savedPositions by mutableStateOf(mutableListOf<SavedPositionData>())
    var savedPositions: List<SavedPositionData>
        get() = _savedPositions
        set(value) {
            _savedPositions = value.toMutableList()
        }

    val _currentTime = mutableStateOf(0.0)

    @Composable
    fun collectCurrentTime() = _currentTime.value

    var currentTime: Double
        get() = _currentTime.value
        set(value) {
            _currentTime.value = value
        }

    val totalDuration: Double
        get() = _cameraKeyframes.maxByOrNull { it.time }?.time ?: 60.0

    /**
     * No-op: list is already a state list, modifying elements or list will trigger recomposition
     */
    fun updateMarkers() {}

    /**
     * Update a caption's properties and ensure UI recomposition
     * @param id ID of the caption to update
     * @param text New text (or null to keep existing)
     * @param time New time (or null to keep existing)
     * @param duration New duration (or null to keep existing)
     * @return True if the caption was found and updated, false otherwise
     */
    fun updateCaption(id: String, text: String? = null, time: Double? = null, duration: Double? = null): Boolean {
        val index = _captions.indexOfFirst { it.id == id }
        if (index >= 0) {
            val caption = _captions[index]

            // Update properties if provided
            if (text != null) caption.text = text
            if (time != null) caption.time = time
            if (duration != null) caption.duration = duration

            // Force recomposition by removing and re-adding the caption
            _captions.removeAt(index)
            _captions.add(caption)

            return true
        }
        return false
    }

    /**
     * Force update of camera keyframes list to trigger UI recomposition
     */
    fun updateCameraKeyframes() {
        _cameraKeyframes = _cameraKeyframes.toMutableList()
    }

    /**
     * Add a new marker at the current time
     * @param name Name of the marker
     * @return The created marker
     */
    fun addMarker(name: String): AnimationMarker {
        val id = "marker_${Date().getTime().toLong()}"
        val marker = AnimationMarker(id, name, currentTime)
        _markers.add(marker)
        return marker
    }

    /**
     * Remove a marker by id
     * @param id ID of the marker to remove
     */
    fun removeMarker(id: String) {
        _markers.removeAll { marker -> marker.id == id }
    }

    /**
     * Add a camera keyframe at the current time
     * @param camera The camera to capture
     * @return The created keyframe
     */
    fun addCameraKeyframe(camera: Camera): CameraKeyframe {
        val id = "keyframe_${Date().getTime().toLong()}"
        val keyframe = CameraKeyframe(
            id = id,
            time = currentTime,
            position = camera.camera.position.clone(),
            target = camera.camera.target.clone(),
            alpha = camera.camera.alpha,
            beta = camera.camera.beta,
            radius = camera.camera.radius,
            fov = camera.camera.fov
        )
        val newList = _cameraKeyframes.toMutableList()
        newList.add(keyframe)
        _cameraKeyframes = newList
        return keyframe
    }

    /**
     * Add a camera keyframe with specific values
     * @param id Unique identifier for the keyframe
     * @param time Time in seconds where the keyframe is placed
     * @param position Camera position
     * @param target Camera target
     * @param alpha Camera horizontal rotation
     * @param beta Camera vertical rotation
     * @param radius Camera distance from target
     * @param fov Camera field of view
     * @return The created keyframe
     */
    fun addCameraKeyframeFromData(
        id: String,
        time: Double,
        position: Vector3,
        target: Vector3,
        alpha: Float,
        beta: Float,
        radius: Float,
        fov: Float
    ): CameraKeyframe {
        val keyframe = CameraKeyframe(
            id = id,
            time = time,
            position = position,
            target = target,
            alpha = alpha,
            beta = beta,
            radius = radius,
            fov = fov
        )
        val newList = _cameraKeyframes.toMutableList()
        newList.add(keyframe)
        _cameraKeyframes = newList
        return keyframe
    }

    /**
     * Remove a camera keyframe by id
     * @param id ID of the keyframe to remove
     */
    fun removeCameraKeyframe(id: String) {
        val newList = _cameraKeyframes.toMutableList()
        newList.removeAll { keyframe -> keyframe.id == id }
        _cameraKeyframes = newList
    }

    /**
     * Apply camera keyframe at the current time
     * @param camera The camera to update
     */
    fun applyCameraKeyframeAtCurrentTime(camera: Camera) {
        // Find the nearest keyframe before and after current time
        val keyframesBefore = _cameraKeyframes.filter { it.time <= currentTime }.maxByOrNull { it.time }
        val keyframesAfter = _cameraKeyframes.filter { it.time > currentTime }.minByOrNull { it.time }

        if (keyframesBefore != null && keyframesAfter != null) {
            // Interpolate between keyframes
            val t = (currentTime - keyframesBefore.time) / (keyframesAfter.time - keyframesBefore.time)
            interpolateCamera(camera, keyframesBefore, keyframesAfter, t)
        } else if (keyframesBefore != null) {
            // Use the keyframe before
            applyKeyframeToCamera(camera, keyframesBefore)
        } else if (keyframesAfter != null) {
            // Use the keyframe after
            applyKeyframeToCamera(camera, keyframesAfter)
        }
    }

    /**
     * Apply a specific keyframe to the camera without changing the current time
     * @param camera The camera to update
     * @param keyframeId The ID of the keyframe to apply
     * @return True if the keyframe was found and applied, false otherwise
     */
    fun applyCameraKeyframeById(camera: Camera, keyframeId: String): Boolean {
        val keyframe = _cameraKeyframes.find { it.id == keyframeId }
        if (keyframe != null) {
            applyKeyframeToCamera(camera, keyframe)
            return true
        }
        return false
    }

    /**
     * Apply a specific keyframe to the camera
     * @param camera The camera to update
     * @param keyframe The keyframe to apply
     */
    private fun applyKeyframeToCamera(camera: Camera, keyframe: CameraKeyframe) {
        camera.camera.position = keyframe.position.clone()
        camera.camera.target = keyframe.target.clone()
        camera.camera.alpha = keyframe.alpha
        camera.camera.beta = keyframe.beta
        camera.camera.radius = keyframe.radius
        camera.camera.fov = keyframe.fov
    }

    /**
     * Interpolate between two keyframes
     * @param camera The camera to update
     * @param keyframe1 First keyframe
     * @param keyframe2 Second keyframe
     * @param t Interpolation factor (0-1)
     */
    private fun interpolateCamera(camera: Camera, keyframe1: CameraKeyframe, keyframe2: CameraKeyframe, t: Double) {
        // Linear interpolation between keyframes
        val position = keyframe1.position.add(keyframe2.position.subtract(keyframe1.position).scale(t.toFloat()))
        val target = keyframe1.target.add(keyframe2.target.subtract(keyframe1.target).scale(t.toFloat()))
        val alpha = keyframe1.alpha + (keyframe2.alpha - keyframe1.alpha) * t.toFloat()
        val beta = keyframe1.beta + (keyframe2.beta - keyframe1.beta) * t.toFloat()
        val radius = keyframe1.radius + (keyframe2.radius - keyframe1.radius) * t.toFloat()
        val fov = keyframe1.fov + (keyframe2.fov - keyframe1.fov) * t.toFloat()

        camera.camera.position = position
        camera.camera.target = target
        camera.camera.alpha = alpha
        camera.camera.beta = beta
        camera.camera.radius = radius
        camera.camera.fov = fov
    }

    /**
     * Add a new saved position
     * @param name Name of the position
     * @param position The 3D position
     * @return The created saved position
     */
    fun addSavedPosition(name: String, position: Vector3): SavedPositionData {
        val id = "position_${Date().getTime().toLong()}"
        val savedPosition = SavedPositionData(
            id = id,
            name = name,
            position = Vector3Data(position.x, position.y, position.z)
        )
        val newList = _savedPositions.toMutableList()
        newList.add(savedPosition)
        _savedPositions = newList
        return savedPosition
    }

    /**
     * Remove a saved position by id
     * @param id ID of the saved position to remove
     */
    fun removeSavedPosition(id: String) {
        val newList = _savedPositions.toMutableList()
        newList.removeAll { position -> position.id == id }
        _savedPositions = newList
    }

    /**
     * Update a saved position
     * @param id ID of the saved position to update
     * @param name New name (or null to keep existing)
     * @param position New position (or null to keep existing)
     * @return True if the position was found and updated, false otherwise
     */
    fun updateSavedPosition(id: String, name: String? = null, position: Vector3? = null): Boolean {
        val index = _savedPositions.indexOfFirst { it.id == id }
        if (index >= 0) {
            val currentPosition = _savedPositions[index]
            val updatedPosition = currentPosition.copy(
                name = name ?: currentPosition.name,
                position = if (position != null) {
                    Vector3Data(position.x, position.y, position.z)
                } else {
                    currentPosition.position
                }
            )
            val newList = _savedPositions.toMutableList()
            newList[index] = updatedPosition
            _savedPositions = newList
            return true
        }
        return false
    }
}

/**
 * Data class representing a caption in the animation
 * @param id Unique identifier
 * @param time Time in seconds when the caption appears
 * @param text The caption text
 */
data class GameCaption(
    val id: String,
    var time: Double,
    var text: String,
    /** Duration in seconds to display after typing (default 5 seconds) */
    var duration: Double = 5.0
)
