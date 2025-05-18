package game

import lib.*
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.sin
import kotlin.random.Random

/**
 * Implements a dust effect for the game scene.
 * Creates a system of small meshes that simulate dust particles floating in the air.
 */
class DustEffect(private val scene: Scene) {
    private val dustParticles = mutableListOf<Mesh>()
    private var enabled: Boolean = false
    private val material = StandardMaterial("dustMaterial", scene)

    // Intensity value between 0 and 1
    var intensity: Float = 0.5f
        set(value) {
            field = value.coerceIn(0f, 1f)
            updateDustParticles()
        }

    init {
        // Configure the material for dust particles
        material.diffuseColor = Color3(0.9f, 0.8f, 0.5f) // Brighter, more yellow color
        material.specularColor = Color3(0.2f, 0.2f, 0.1f)
        material.emissiveColor = Color3(0.4f, 0.3f, 0.1f) // More emissive to be more visible
        material.alpha = 0.5f // Higher alpha for better visibility
        material.backFaceCulling = false

        // Register a render loop to animate the dust particles
        scene.registerBeforeRender {
            if (enabled) {
                animateDustParticles()
            }
        }
    }

    /**
     * Creates the dust particle meshes
     */
    private fun createDustParticles() {
        // Clear any existing dust particles
        dustParticles.forEach { scene.removeMesh(it) }
        dustParticles.clear()

        // Calculate number of dust particles based on intensity
        val count = (500 * intensity).toInt().coerceIn(1, 500)

        for (i in 0 until count) {
            // Create a small plane for each dust particle
            val options = object : CreatePlaneOptions {
                override val width = 0.1f  // Increased size for better visibility
                override val height = 0.1f // Increased size for better visibility
                override val updatable = false
            }

            val dustParticle = MeshBuilder.CreatePlane("dustParticle$i", options, scene)
            dustParticle.material = material
            dustParticle.billboardMode = Mesh.BILLBOARDMODE_ALL

            // Position randomly in the scene
            resetDustParticlePosition(dustParticle)

            // Add to our list
            dustParticles.add(dustParticle)
        }
    }

    /**
     * Resets a dust particle to a random position in the scene
     */
    private fun resetDustParticlePosition(dustParticle: Mesh) {
        // Random position throughout the scene (not just above)
        val x = Random.nextFloat() * 100f - 50f
        val y = Random.nextFloat() * 20f - 5f // Some particles low, some high
        val z = Random.nextFloat() * 100f - 50f

        dustParticle.position = Vector3(x, y, z)

        // Random scale based on intensity - increased for better visibility
        val scale = 0.5f + (Random.nextFloat() * 0.5f * intensity)
        dustParticle.scaling = Vector3(scale, scale, scale)
    }

    /**
     * Animates the dust particles with gentle floating motion
     */
    private fun animateDustParticles() {
        val deltaTime = scene.deltaTime / 1000f
        val time = (scene.getFrameId() % 10000) / 100f // Cycling time value

        dustParticles.forEach { dustParticle ->
            // Movement in all directions - increased speed for better visibility
            val moveSpeed = 0.5f * intensity  // Increased from 0.2f to 0.6f

            // Use particle's position to create varied movement patterns
            val xOffset = sin(dustParticle.position.x * 0.1f + time * 0.3f)  // Faster oscillation
            val yOffset = cos(dustParticle.position.y * 0.1f + time * 0.25f) // Faster oscillation
            val zOffset = sin(dustParticle.position.z * 0.1f + time * 0.2f)  // Faster oscillation

            dustParticle.position.x += xOffset * moveSpeed * deltaTime
            dustParticle.position.y += yOffset * moveSpeed * deltaTime
            dustParticle.position.z += zOffset * moveSpeed * deltaTime

            // Upward drift to simulate hot air rising - increased for better visibility
            dustParticle.position.y += 0.1f * intensity * deltaTime  // Increased from 0.05f to 0.15f

            // Rotation - increased for better visibility
            dustParticle.rotation.z += 0.1f * deltaTime  // Increased from 0.02f to 0.1f

            // Reset if out of bounds
            if (dustParticle.position.y > 20f || 
                dustParticle.position.x > 60f || dustParticle.position.x < -60f ||
                dustParticle.position.z > 60f || dustParticle.position.z < -60f) {
                resetDustParticlePosition(dustParticle)
            }
        }
    }

    /**
     * Updates the dust particles based on the current intensity
     */
    private fun updateDustParticles() {
        // Recreate dust particles with new intensity
        if (enabled) {
            createDustParticles()
        }
    }

    /**
     * Enables the dust effect
     */
    fun enable() {
        if (!enabled) {
            enabled = true
            createDustParticles()
        }
    }

    /**
     * Disables the dust effect
     */
    fun disable() {
        if (enabled) {
            enabled = false
            dustParticles.forEach { scene.removeMesh(it) }
            dustParticles.clear()
        }
    }

    /**
     * Sets the enabled state of the dust effect
     */
    fun setEnabled(value: Boolean) {
        if (value) enable() else disable()
    }

    /**
     * Gets the enabled state of the dust effect
     */
    fun isEnabled(): Boolean {
        return enabled
    }

    /**
     * Disposes of resources used by the dust effect
     */
    fun dispose() {
        disable()
        material.dispose()
    }
}
