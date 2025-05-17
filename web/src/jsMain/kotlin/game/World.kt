package game

import lib.*
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.sin

class World(private val scene: Scene) {

    val ambience: HemisphericLight
    val sun: DirectionalLight
    val shadows: CascadedShadowGenerator

    // Time of day value between 0 (midnight) and 1 (end of day)
    var timeOfDay: Float = 0.5f
        set(value) {
            field = value.coerceIn(0f, 1f)
            updateSunDirection()
        }

    init {
        // todo hard-coded
        scene.fogMode = Scene.FOGMODE_EXP2
        scene.fogDensity = 0.02f
        // Set fogColor to match the background color
        scene.fogColor = Color3(scene.clearColor.r, scene.clearColor.g, scene.clearColor.b)
        // Only set clearColor if it hasn't been set already (default is black)
        if (scene.clearColor.r == 0f && scene.clearColor.g == 0f && scene.clearColor.b == 0f) {
            scene.clearColor = scene.fogColor.toColor4().toLinearSpace()
        }
        scene.ambientColor = scene.fogColor

        // todo hard-coded
        val ambience = HemisphericLight("Ambience", Vector3(0f, -1f, 0f).normalize(), scene)
        ambience.diffuse = Color3.White()
        ambience.groundColor = Color3.White()
        ambience.intensity = 0.5f

        // Initialize sun with default direction
        val sun = DirectionalLight("sun", Vector3(0.25f, -1f, 0.5f).normalize(), scene)
        sun.intensity = 1f
        sun.diffuse = Color3.White()
        sun.shadowMinZ = scene.activeCamera?.minZ ?: 0f
        sun.shadowMaxZ = scene.activeCamera?.maxZ ?: 0f

        this.sun = sun
        this.ambience = ambience

        val shadowGenerator = CascadedShadowGenerator(512, sun)
        shadowGenerator.lambda = 0.5f
        shadowGenerator.bias = 0.005f
        shadowGenerator.normalBias = 0.005f
        shadowGenerator.transparencyShadow = true
        shadowGenerator.stabilizeCascades = true
        shadowGenerator.shadowMaxZ = sun.shadowMaxZ
        shadowGenerator.splitFrustum()

        this.shadows = shadowGenerator

        // Set initial sun direction based on default time of day
        updateSunDirection()
    }

    private fun updateSunDirection() {
        // Convert time of day to angle in radians (0 to 2π)
        // Offset by π/2 so that noon (0.5) is at the highest point
        val angle = (timeOfDay * 2 * PI - PI/2).toFloat()

        // Calculate sun direction based on angle
        // At noon (timeOfDay = 0.5), sun is high in the sky (y is most negative)
        // At midnight (timeOfDay = 0 or 1), sun is below horizon (y is positive)
        val x = sin(angle)
        val y = -cos(angle)
        val z = 0.5f  // Keep some constant z component for angled light

        // Update sun direction
        sun.direction = Vector3(x, y, z).normalize()
    }

    fun update() {
        scene.activeCamera?.let { camera ->
            sun.position.copyFrom(camera.position)
        }

        // Update fog color to match the background color
        scene.fogColor = Color3(scene.clearColor.r, scene.clearColor.g, scene.clearColor.b).toGammaSpace()
        scene.ambientColor = scene.fogColor
    }

    fun addShadowCaster(mesh: AbstractMesh) {
        shadows.addShadowCaster(mesh)
    }

    fun removeShadowCaster(mesh: AbstractMesh) {
        shadows.removeShadowCaster(mesh)
    }
}
