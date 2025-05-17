package game

import baseUrl
import lib.Scene
import lib.Texture
import lib.DynamicTexture
import lib.TextureOptions
import lib.Color3
import lib.DynamicTextureOptions
import lib.Math
import org.w3c.dom.CanvasRenderingContext2D
import org.w3c.dom.HTMLImageElement
import kotlin.js.Promise

/**
 * A utility class for creating texture atlases
 */
class TextureAtlas(private val scene: Scene, private val useLinearSampling: Boolean = false) {
    // Texture atlas size (number of tiles in each dimension)
    val atlasSize = 4 // 4x4 grid of textures

    // Map of GameTile id to position in the texture atlas (x, y coordinates)
    val atlasPositions = mutableMapOf<String, Pair<Int, Int>>()

    // Map of GameTile id to texture URL
    private val textureUrls = mutableMapOf<String, String>()

    // Next available position in the texture atlas
    private var nextAtlasPosition = Pair(0, 0)

    // The dynamic texture used for the atlas
    private var dynamicTexture: DynamicTexture? = null

    /**
     * Creates a new TextureAtlas with the same texture positions and URLs but with a different sampling mode
     * @param newSamplingMode Whether to use linear sampling for the new atlas
     * @return A new TextureAtlas with the same texture positions and URLs
     */
    fun createWithNewSamplingMode(newSamplingMode: Boolean): TextureAtlas {
        val newAtlas = TextureAtlas(scene, newSamplingMode)

        // Copy the atlas positions
        newAtlas.atlasPositions.putAll(this.atlasPositions)

        // Copy the texture URLs
        this.textureUrls.forEach { (id, url) ->
            newAtlas.textureUrls[id] = url
        }

        // Set the next position
        newAtlas.nextAtlasPosition = this.nextAtlasPosition

        return newAtlas
    }

    /**
     * Adds a texture to the texture atlas
     */
    fun addTexture(id: String, photoUrl: String? = null) {
        // If this texture is already in the atlas, return
        if (atlasPositions.containsKey(id)) {
            return
        }

        // Calculate the next position in the atlas
        val x = nextAtlasPosition.first
        val y = nextAtlasPosition.second

        // Store the position in the atlas
        atlasPositions[id] = Pair(x, y)

        // Store the texture URL if provided
        if (photoUrl != null) {
            textureUrls[id] = photoUrl
        }

        // Update the next position
        nextAtlasPosition = if (x + 1 >= atlasSize) {
            Pair(0, y + 1)
        } else {
            Pair(x + 1, y)
        }
    }

    /**
     * Gets the UV coordinates for a texture in the atlas
     */
    fun getUVCoordinates(id: String): Array<Float> {
        val position = atlasPositions[id] ?: Pair(0, 0)
        val atlasX = position.first.toFloat() / atlasSize
        val atlasY = position.second.toFloat() / atlasSize
        val atlasStep = 1f / atlasSize

        return arrayOf(
            atlasX, atlasY,
            atlasX + atlasStep, atlasY,
            atlasX + atlasStep, atlasY + atlasStep,
            atlasX, atlasY + atlasStep
        )
    }

    /**
     * Disposes of the current dynamic texture if it exists
     */
    fun dispose() {
        dynamicTexture?.dispose()
        dynamicTexture = null
    }

    /**
     * Creates a texture atlas with the added textures
     */
    fun createAtlas(): Texture {
        // If there are no textures, return a default texture
        if (atlasPositions.isEmpty()) {
            return Texture("/assets/stone.png", scene, object : TextureOptions {
                override val samplingMode = if (useLinearSampling) 
                    Texture.NEAREST_SAMPLINGMODE 
                else 
                    Texture.TRILINEAR_SAMPLINGMODE
            })
        }

        // Dispose of the old texture if it exists
        dispose()

        // Calculate the size of the atlas
        val tileSize = 256
        val atlasWidth = atlasSize * tileSize
        val atlasHeight = atlasSize * tileSize

        // Create a dynamic texture for the atlas
        val texture = DynamicTexture(
            "textureAtlas",
            options = object : DynamicTextureOptions {
                override val width = atlasWidth
                override val height = atlasHeight
            },
            scene,
            true,
            if (useLinearSampling) Texture.NEAREST_SAMPLINGMODE else Texture.TRILINEAR_SAMPLINGMODE
        )

        // Get the 2D context from the dynamic texture
        val context = texture.getContext()

        // Track how many textures have been loaded
        var loadedTextures = 0
        val totalTextures = atlasPositions.size

        // Function to check if all textures are loaded and update the atlas
        fun checkAllTexturesLoaded() {
            loadedTextures++
            if (loadedTextures >= totalTextures) {
                // All textures loaded, update the dynamic texture
                texture.update(false)
                console.log("All textures loaded and atlas updated")
            }
        }

        // Draw each tile in the atlas
        atlasPositions.forEach { (id, position) ->
            val x = position.first * tileSize
            val y = position.second * tileSize

            // Check if we have a texture URL for this ID
            if (id in textureUrls) {
                // Get the full URL for the texture
                val fullUrl = "$baseUrl${textureUrls[id]}"

                // Create an image element to load the texture
                val img = js("new Image()") as HTMLImageElement

                // Set crossOrigin to anonymous to avoid CORS issues
                img.crossOrigin = "anonymous"

                // When the image loads, draw it on the canvas
                img.onload = { _ ->
                    try {
                        // Draw the image on the canvas
                        context.drawImage(
                            img,
                            x.toDouble(),
                            y.toDouble(),
                            tileSize.toDouble(),
                            tileSize.toDouble()
                        )

                        // Check if all textures are loaded
                        checkAllTexturesLoaded()
                    } catch (e: Exception) {
                        console.error("Error drawing texture to atlas: ${e.message}")
                        // Still count this texture as loaded even if there was an error
                        checkAllTexturesLoaded()
                    }

                    // Return undefined to satisfy the onload type
                    undefined
                }

                // Handle image loading errors
                img.onerror = { event, source, lineno, colno, error ->
                    console.error("Error loading image: $fullUrl")

                    // Draw a placeholder for failed images
                    context.fillStyle = "red"
                    context.fillRect(
                        x.toDouble(),
                        y.toDouble(),
                        tileSize.toDouble(),
                        tileSize.toDouble()
                    )

                    // Add error text
                    context.fillStyle = "white"
                    context.font = "20px Arial"
                    context.fillText(
                        "Error loading",
                        (x + 10).toDouble(),
                        (y + 30).toDouble()
                    )

                    // Check if all textures are loaded
                    checkAllTexturesLoaded()

                    // Return undefined to satisfy the onerror type
                    undefined
                }

                // Start loading the image
                img.src = fullUrl
            } else {
                // No texture URL available, draw a placeholder
                // Generate a unique color for this tile based on its position
                val colorHue = ((position.first + position.second * atlasSize) * 137.5) % 360
                val color = "hsl($colorHue, 75%, 50%)"

                // Fill the tile area with the color
                context.fillStyle = color
                context.fillRect(
                    x.toDouble(),
                    y.toDouble(),
                    tileSize.toDouble(),
                    tileSize.toDouble()
                )

                // Add a border to make the tiles more visible
                context.strokeStyle = "white"
                context.lineWidth = 2.0
                context.strokeRect(
                    x.toDouble(),
                    y.toDouble(),
                    tileSize.toDouble(),
                    tileSize.toDouble()
                )

                // Add the tile ID as text for debugging
                context.fillStyle = "white"
                context.font = "20px Arial"
                context.fillText(
                    id.substring(0, Math.min(8f, id.length.toFloat()).toInt()), // Show first 8 chars of ID
                    (x + 10).toDouble(),
                    (y + 30).toDouble()
                )

                // Count this texture as loaded
                checkAllTexturesLoaded()
            }
        }

        // Update the dynamic texture initially (will be updated again when all images are loaded)
        texture.update(false)

        // Store the texture for later use
        dynamicTexture = texture

        return texture
    }

    /**
     * Gets the texture atlas
     */
    fun getAtlas(): Texture {
        return dynamicTexture ?: createAtlas()
    }
}
