package game

import lib.*
import kotlin.random.Random

/**
 * Implements a rain effect for the game scene.
 * Creates a system of small meshes that simulate falling rain.
 */
class RainEffect(private val scene: Scene) {
    private val raindrops = mutableListOf<Mesh>()
    private var enabled: Boolean = false
    private val material = StandardMaterial("rainMaterial", scene)

    // Intensity value between 0 and 1
    var intensity: Float = 0.5f
        set(value) {
            field = value.coerceIn(0f, 1f)
            updateRaindrops()
        }

    init {
        // Configure the material for raindrops
        material.diffuseColor = Color3(0.7f, 0.7f, 0.9f)
        material.specularColor = Color3(0.2f, 0.2f, 0.3f)
        material.emissiveColor = Color3(0.1f, 0.1f, 0.3f)
        material.alpha = 0.5f
        material.backFaceCulling = false

        // Register a render loop to animate the raindrops
        scene.registerBeforeRender {
            if (enabled) {
                animateRaindrops()
            }
        }
    }

    /**
     * Creates the raindrop meshes
     */
    private fun createRaindrops() {
        // Clear any existing raindrops
        raindrops.forEach { scene.removeMesh(it) }
        raindrops.clear()

        // Calculate number of raindrops based on intensity
        val count = (1000 * intensity).toInt().coerceIn(1, 1000)

        for (i in 0 until count) {
            // Create a small plane for each raindrop
            val options = object : CreatePlaneOptions {
                override val width = 0.03f
                override val height = 0.3f
                override val updatable = false
            }

            val raindrop = MeshBuilder.CreatePlane("raindrop$i", options, scene)
            raindrop.material = material
            raindrop.billboardMode = Mesh.BILLBOARDMODE_Y // Only rotate around Y axis to maintain the elongated shape

            // Tilt the raindrop to simulate falling angle
            raindrop.rotation = Vector3(0.2f, 0f, 0f)

            // Position randomly in the scene
            resetRaindropPosition(raindrop)

            // Add to our list
            raindrops.add(raindrop)
        }
    }

    /**
     * Resets a raindrop to a random position above the scene
     */
    private fun resetRaindropPosition(raindrop: Mesh) {
        // Random position above the scene
        val x = Random.nextFloat() * 100f - 50f
        val y = 20f + Random.nextFloat() * 10f
        val z = Random.nextFloat() * 100f - 50f

        raindrop.position = Vector3(x, y, z)

        // Random scale based on intensity
        val scale = 0.8f + (Random.nextFloat() * 0.4f * intensity)
        raindrop.scaling = Vector3(scale, scale, scale)
    }

    /**
     * Animates the raindrops by moving them downward
     */
    private fun animateRaindrops() {
        val deltaTime = scene.deltaTime / 1000f

        raindrops.forEach { raindrop ->
            // Move downward and slightly to the side to simulate wind
            val fallSpeed = 8f + (Random.nextFloat() * 4f * intensity)
            raindrop.position.y -= fallSpeed * deltaTime
            raindrop.position.x -= 1f * intensity * deltaTime // Slight sideways movement

            // Reset if below the scene
            if (raindrop.position.y < -5f) {
                resetRaindropPosition(raindrop)
            }
        }
    }

    /**
     * Updates the raindrops based on the current intensity
     */
    private fun updateRaindrops() {
        // Recreate raindrops with new intensity
        if (enabled) {
            createRaindrops()
        }
    }

    /**
     * Enables the rain effect
     */
    fun enable() {
        if (!enabled) {
            enabled = true
            createRaindrops()
        }
    }

    /**
     * Disables the rain effect
     */
    fun disable() {
        if (enabled) {
            enabled = false
            raindrops.forEach { scene.removeMesh(it) }
            raindrops.clear()
        }
    }

    /**
     * Sets the enabled state of the rain effect
     */
    fun setEnabled(value: Boolean) {
        if (value) enable() else disable()
    }

    /**
     * Gets the enabled state of the rain effect
     */
    fun isEnabled(): Boolean {
        return enabled
    }

    /**
     * Disposes of resources used by the rain effect
     */
    fun dispose() {
        disable()
        material.dispose()
    }
}
