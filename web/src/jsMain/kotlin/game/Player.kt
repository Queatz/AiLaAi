package game

import lib.*

class Player(private val scene: Scene, private var useLinearSampling: Boolean = false) {
    val mesh: Mesh

    init {
        val mesh = MeshBuilder.CreatePlane("Player", object : CreatePlaneOptions {
            override val size = 0.5f
            override val updatable = true
        }, scene)

        // todo hard-coded
        val mat = StandardMaterial("Player", scene)
        mat.diffuseTexture = Texture("/assets/player.png", scene, object : TextureOptions {
            override val samplingMode = if (useLinearSampling) 
                Texture.NEAREST_SAMPLINGMODE 
            else 
                Texture.TRILINEAR_SAMPLINGMODE
        })
        mat.diffuseTexture.hasAlpha = true
        mat.diffuseTexture.wrapU = Texture.CLAMP_ADDRESSMODE
        mat.diffuseTexture.wrapV = Texture.CLAMP_ADDRESSMODE
        mesh.material = mat
        mesh.material.backFaceCulling = false
        mesh.billboardMode = Mesh.BILLBOARDMODE_Y
        mesh.position.y += 0.25f
        mesh.receiveShadows = true

        this.mesh = mesh
    }

    /**
     * Updates the texture sampling mode based on the linearSampling flag
     * @param linearSampling If true, use NEAREST_SAMPLINGMODE, otherwise use TRILINEAR_SAMPLINGMODE
     */
    fun updateTextureSamplingMode(linearSampling: Boolean) {
        if (useLinearSampling != linearSampling) {
            useLinearSampling = linearSampling

            // Get the material
            val material = mesh.material as StandardMaterial

            // Get the old texture
            val oldTexture = material.diffuseTexture

            // Get the URL from the old texture
            val url = oldTexture.url

            // Dispose of the old texture
            oldTexture.dispose()

            try {
                // Create a new texture with the updated sampling mode
                val newTexture = Texture(
                    url = url,
                    scene = scene,
                    options = object : TextureOptions {
                        override val samplingMode = if (useLinearSampling) 
                            Texture.NEAREST_SAMPLINGMODE 
                        else 
                            Texture.TRILINEAR_SAMPLINGMODE
                    }
                )

                // Set the texture properties
                newTexture.hasAlpha = true
                newTexture.wrapU = Texture.CLAMP_ADDRESSMODE
                newTexture.wrapV = Texture.CLAMP_ADDRESSMODE

                // Assign the new texture to the material
                material.diffuseTexture = newTexture

                // Log success
                console.log("Player texture updated successfully with sampling mode: ${if (useLinearSampling) "NEAREST" else "TRILINEAR"}")
            } catch (e: Exception) {
                // Log error
                console.error("Error updating player texture: ${e.message}")

                // Fallback to a default texture if there's an error
                material.diffuseTexture = Texture(
                    url = "/assets/player.png",
                    scene = scene,
                    options = object : TextureOptions {
                        override val samplingMode = if (useLinearSampling) 
                            Texture.NEAREST_SAMPLINGMODE 
                        else 
                            Texture.TRILINEAR_SAMPLINGMODE
                    }
                )
                material.diffuseTexture.hasAlpha = true
                material.diffuseTexture.wrapU = Texture.CLAMP_ADDRESSMODE
                material.diffuseTexture.wrapV = Texture.CLAMP_ADDRESSMODE
            }
        }
    }

    fun walk(walk: Vector3) {
        val walkForward = scene.activeCamera!!.getDirection(Vector3.Forward()).multiply(Vector3(1f, 0f, 1f)).normalize().scale(walk.y)
        val walkRight = scene.activeCamera!!.getDirection(Vector3.Right()).multiply(Vector3(1f, 0f, 1f)).normalize().scale(walk.x)
        mesh.position.addInPlace(walkForward.add(walkRight).normalize().scale(2f).scale(scene.deltaTime / 1000f))
    }
}
