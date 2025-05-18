package game

import lib.*
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.sin
import kotlin.random.Random

/**
 * Implements a snow effect for the game scene.
 * Creates a system of small meshes that simulate falling snow.
 */
class SnowEffect(private val scene: Scene) {
    private val snowflakes = mutableListOf<Mesh>()
    private var enabled: Boolean = false
    private val material = StandardMaterial("snowMaterial", scene)

    // Intensity value between 0 and 1
    var intensity: Float = 0.5f
        set(value) {
            field = value.coerceIn(0f, 1f)
            updateSnowflakes()
        }

    init {
        // Configure the material for snowflakes
        material.diffuseColor = Color3(1f, 1f, 1f)
        material.specularColor = Color3(0.1f, 0.1f, 0.1f)
        material.emissiveColor = Color3(0.8f, 0.8f, 0.8f)
        material.alpha = 0.7f
        material.backFaceCulling = false

        // Register a render loop to animate the snowflakes
        scene.registerBeforeRender {
            if (enabled) {
                animateSnowflakes()
            }
        }
    }

    /**
     * Creates the snowflake meshes
     */
    private fun createSnowflakes() {
        // Clear any existing snowflakes
        snowflakes.forEach { scene.removeMesh(it) }
        snowflakes.clear()

        // Calculate number of snowflakes based on intensity
        val count = (750 * intensity).toInt().coerceIn(1, 750)

        for (i in 0 until count) {
            // Create a small plane for each snowflake
            val options = object : CreatePlaneOptions {
                override val width = 0.1f
                override val height = 0.1f
                override val updatable = false
            }

            val snowflake = MeshBuilder.CreatePlane("snowflake$i", options, scene)
            snowflake.material = material
            snowflake.billboardMode = Mesh.BILLBOARDMODE_ALL

            // Position randomly in the scene
            resetSnowflakePosition(snowflake)

            // Add to our list
            snowflakes.add(snowflake)
        }
    }

    /**
     * Resets a snowflake to a random position above the scene
     */
    private fun resetSnowflakePosition(snowflake: Mesh) {
        // Random position above the scene
        val x = Random.nextFloat() * 100f - 50f
        val y = 20f + Random.nextFloat() * 10f
        val z = Random.nextFloat() * 100f - 50f

        snowflake.position = Vector3(x, y, z)

        // Random scale based on intensity
        val scale = 0.05f + (Random.nextFloat() * 0.15f * intensity)
        snowflake.scaling = Vector3(scale, scale, scale)
    }

    /**
     * Animates the snowflakes by moving them downward
     */
    private fun animateSnowflakes() {
        val deltaTime = scene.deltaTime / 1000f

        snowflakes.forEach { snowflake ->
            // Move downward
            val fallSpeed = 1f + (Random.nextFloat() * 2f * intensity)
            snowflake.position.y -= fallSpeed * deltaTime

            // Add some horizontal drift
            val driftSpeed = 0.3f * intensity
            snowflake.position.x += sin(snowflake.position.y * 0.5f) * driftSpeed * deltaTime
            snowflake.position.z += cos(snowflake.position.y * 0.5f) * driftSpeed * deltaTime

            // Rotate the snowflake
            snowflake.rotation.z += 0.1f * deltaTime

            // Reset if below the scene
            if (snowflake.position.y < -5f) {
                resetSnowflakePosition(snowflake)
            }
        }
    }

    /**
     * Updates the snowflakes based on the current intensity
     */
    private fun updateSnowflakes() {
        // Recreate snowflakes with new intensity
        if (enabled) {
            createSnowflakes()
        }
    }

    /**
     * Enables the snow effect
     */
    fun enable() {
        if (!enabled) {
            enabled = true
            createSnowflakes()
        }
    }

    /**
     * Disables the snow effect
     */
    fun disable() {
        if (enabled) {
            enabled = false
            snowflakes.forEach { scene.removeMesh(it) }
            snowflakes.clear()
        }
    }

    /**
     * Sets the enabled state of the snow effect
     */
    fun setEnabled(value: Boolean) {
        if (value) enable() else disable()
    }

    /**
     * Gets the enabled state of the snow effect
     */
    fun isEnabled(): Boolean {
        return enabled
    }

    /**
     * Disposes of resources used by the snow effect
     */
    fun dispose() {
        disable()
        material.dispose()
    }
}
