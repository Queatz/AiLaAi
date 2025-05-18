package game

import androidx.compose.runtime.mutableStateOf
import lib.Vector3
import kotlin.js.Date

/**
 * Data class representing an animation marker
 * @param id Unique identifier for the marker
 * @param name Name of the marker
 * @param time Time in seconds where the marker is placed
 * @param duration Duration in seconds (0 = play until end)
 */
data class AnimationMarker(
    val id: String,
    var name: String,
    var time: Double,
    var duration: Double = 0.0
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
    val fov: Float
)

/**
 * Class to manage animation data
 */
class AnimationData {
    private val _markers = mutableStateOf(mutableListOf<AnimationMarker>())
    val markers: List<AnimationMarker> get() = _markers.value.sortedBy { it.time }

    private val _cameraKeyframes = mutableStateOf(mutableListOf<CameraKeyframe>())
    val cameraKeyframes: List<CameraKeyframe> get() = _cameraKeyframes.value.sortedBy { it.time }

    private val _currentTime = mutableStateOf(0.0)
    var currentTime: Double
        get() = _currentTime.value
        set(value) {
            _currentTime.value = value
            onTimeUpdate?.invoke(value)
        }

    // Callback for when the current time changes
    var onTimeUpdate: ((Double) -> Unit)? = null

    val totalDuration: Double
        get() = _cameraKeyframes.value.maxByOrNull { it.time }?.time ?: 60.0

    /**
     * Force update of markers list to trigger UI recomposition
     */
    fun updateMarkers() {
        _markers.value = _markers.value.toMutableList()
    }

    /**
     * Force update of camera keyframes list to trigger UI recomposition
     */
    fun updateCameraKeyframes() {
        _cameraKeyframes.value = _cameraKeyframes.value.toMutableList()
    }

    /**
     * Add a new marker at the current time
     * @param name Name of the marker
     * @return The created marker
     */
    fun addMarker(name: String): AnimationMarker {
        val id = "marker_${Date().getTime().toLong()}"
        val marker = AnimationMarker(id, name, currentTime)
        val newList = _markers.value.toMutableList()
        newList.add(marker)
        _markers.value = newList
        return marker
    }

    /**
     * Remove a marker by id
     * @param id ID of the marker to remove
     */
    fun removeMarker(id: String) {
        val newList = _markers.value.toMutableList()
        newList.removeAll { marker -> marker.id == id }
        _markers.value = newList
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
        val newList = _cameraKeyframes.value.toMutableList()
        newList.add(keyframe)
        _cameraKeyframes.value = newList
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
        val newList = _cameraKeyframes.value.toMutableList()
        newList.add(keyframe)
        _cameraKeyframes.value = newList
        return keyframe
    }

    /**
     * Remove a camera keyframe by id
     * @param id ID of the keyframe to remove
     */
    fun removeCameraKeyframe(id: String) {
        val newList = _cameraKeyframes.value.toMutableList()
        newList.removeAll { keyframe -> keyframe.id == id }
        _cameraKeyframes.value = newList
    }

    /**
     * Apply camera keyframe at the current time
     * @param camera The camera to update
     */
    fun applyCameraKeyframeAtCurrentTime(camera: Camera) {
        // Find the nearest keyframe before and after current time
        val keyframesBefore = cameraKeyframes.filter { it.time <= currentTime }.maxByOrNull { it.time }
        val keyframesAfter = cameraKeyframes.filter { it.time > currentTime }.minByOrNull { it.time }

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
}
