package game

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import lib.*
import org.w3c.dom.HTMLCanvasElement
import web.timers.setTimeout
import kotlin.js.Date

class Game(
    val canvas: HTMLCanvasElement,
    val editable: Boolean = true
) {
    val scene: Scene
    val map: Map
    val engine: Engine
    val animationData = AnimationData()

    // Animation playback state
    private var isPlaying = false
    private var lastFrameTime = 0.0

    // StateFlow for play state changes
    private val _playStateFlow = MutableStateFlow(false)

    /**
     * StateFlow that emits the current play state
     */
    val playStateFlow: StateFlow<Boolean> = _playStateFlow.asStateFlow()


    fun resize() {
        engine.resize()
    }

    fun dispose() {
        map.game = null
        engine.dispose()
        scene.dispose()
    }

    /**
     * Start animation playback
     */
    fun play() {
        isPlaying = true
        lastFrameTime = Date().getTime()
        _playStateFlow.value = true
    }

    /**
     * Pause animation playback
     */
    fun pause() {
        isPlaying = false
        _playStateFlow.value = false
    }

    /**
     * Toggle animation playback
     */
    fun togglePlayback(): Boolean {
        if (isPlaying) {
            pause()
        } else {
            play()
        }
        return isPlaying
    }

    /**
     * Check if animation is currently playing
     */
    fun isPlaying(): Boolean {
        return isPlaying
    }

    /**
     * Set the current animation time
     * @param time Time in seconds
     */
    fun setTime(time: Double) {
        animationData.currentTime = time.coerceAtLeast(0.0)
        // Apply camera keyframe at the current time
        animationData.applyCameraKeyframeAtCurrentTime(map.camera)
    }

    init {
        engine = Engine(canvas, true, js("{ preserveDrawingBuffer: true, disableWebGL2Support: false }"))
        scene = Scene(engine)
        map = Map(scene)

        // Set reference to this Game instance in the Map
        map.game = this

        engine.runRenderLoop {
            // Update animation if playing
            if (isPlaying) {
                val currentTime = Date().getTime()
                val deltaTime = (currentTime - lastFrameTime) / 1000.0 // Convert to seconds
                lastFrameTime = currentTime

                // Update current time
                animationData.currentTime += deltaTime

                // Loop back to start if we reach the end
                if (animationData.currentTime > animationData.totalDuration) {
                    animationData.currentTime = 0.0
                    // Change the Pause button back to Play when animation reaches the end
                    isPlaying = false
                    _playStateFlow.value = false
                }

                // Apply camera keyframe at the current time
                animationData.applyCameraKeyframeAtCurrentTime(map.camera)
            }

            scene.render()
        }

        setTimeout({ canvas.focus() })
    }
}
