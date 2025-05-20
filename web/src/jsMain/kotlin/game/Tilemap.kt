package game

import baseUrl
import com.queatz.db.GameObject
import com.queatz.db.GameObjectOptions
import com.queatz.db.GameTile
import json
import kotlinx.serialization.decodeFromString
import kotlinx.serialization.json.Json
import lib.Scene
import lib.DirectionalLight
import lib.AbstractMesh
import lib.Mesh
import lib.VertexData
import lib.MeshBuilder
import lib.VertexBuffer
import lib.StandardMaterial
import lib.Texture
import lib.Color3
import lib.Vector3
import lib.Quaternion
import lib.Color4
import lib.Math
import lib.CreatePlaneOptions
import kotlin.js.Date
import lib.TextureOptions

enum class Side(val value: String) {
    X("x"),
    Y("y"),
    Z("z"),
    NEGATIVE_X("-x"),
    NEGATIVE_Y("-y"),
    NEGATIVE_Z("-z");

    override fun toString(): String = value

    val abs: Side
        get() = when (this) {
            NEGATIVE_X -> X
            NEGATIVE_Y -> Y
            NEGATIVE_Z -> Z
            else -> this
        }

    companion object {
        fun fromString(value: String): Side {
            return entries.find { it.value == value }
                ?: throw IllegalArgumentException("Invalid side: $value")
        }
    }
}

class Tilemap(
    private val scene: Scene,
    private val sun: DirectionalLight,
    private val addShadowCaster: (AbstractMesh) -> Unit,
    private val removeShadowCaster: (AbstractMesh) -> Unit
) {
    // Flag to determine which sampling mode to use
    private var useLinearSampling: Boolean = false

    // Single mesh for all tiles
    val mesh: Mesh
    private val vertexData: VertexData

    // Base mesh for shadows
    private lateinit var shadowBaseMesh: Mesh

    // { 'x,y,z,d': index }
    private val tiles = mutableMapOf<String, Int>()

    // { 'x,y,z,d': gameTileId }
    private val tileTypes = mutableMapOf<String, String>()

    // { 'x,y,z,d': [mesh, mesh] }
    private val objects = mutableMapOf<String, List<AbstractMesh>>()

    // { 'x,y,z,d': gameObjectId }
    private val objectTypes = mutableMapOf<String, String>()

    // { 'x,y,z,d': options }
    private val objectOptions = mutableMapOf<String, String>()

    // Store base meshes by GameObject id
    private val objectBaseMeshes = mutableMapOf<String, Mesh>()

    // Store textures by GameTile id
    private val tileTextures = mutableMapOf<String, Texture>()

    // Rotation blending state per object node
    private data class RotationBlendInfo(
        var blending: Boolean = true,
        var startTime: Double = 0.0,
        var startRotation: Quaternion = Quaternion.Identity()
    )
    private val rotationBlendInfos = mutableMapOf<AbstractMesh, RotationBlendInfo>()

    // Texture atlas for rendering tiles
    private var textureAtlas = TextureAtlas(scene, useLinearSampling)

    // Current GameTile
    private var currentGameTile: GameTile? = null

    /**
     * Updates the texture sampling mode based on the linearSampling flag
     * @param linearSampling If true, use NEAREST_SAMPLINGMODE, otherwise use TRILINEAR_SAMPLINGMODE
     */
    fun updateTextureSamplingMode(linearSampling: Boolean) {
        if (useLinearSampling != linearSampling) {
            useLinearSampling = linearSampling

            // Create a new texture atlas with the same texture positions and URLs but with the new sampling mode
            val newTextureAtlas = textureAtlas.createWithNewSamplingMode(useLinearSampling)

            // Dispose of the old texture atlas
            textureAtlas.dispose()

            // Use the new texture atlas
            textureAtlas = newTextureAtlas

            // Recreate all textures with the new sampling mode
            val texturesToRecreate = tileTextures.toMap()
            tileTextures.clear()

            // Recreate each texture with the new sampling mode
            texturesToRecreate.forEach { (gameTileId, oldTexture) ->
                val url = oldTexture.url
                oldTexture.dispose()

                tileTextures[gameTileId] = Texture(
                    url = url,
                    scene = scene,
                    options = object : TextureOptions {
                        override val samplingMode = if (useLinearSampling) 
                            Texture.NEAREST_SAMPLINGMODE 
                        else 
                            Texture.TRILINEAR_SAMPLINGMODE
                    }
                )
            }

            // Update the material to use the new textures
            if (tileTextures.isNotEmpty()) {
                updateMeshMaterial()
            }
        }
    }

    init {
        // Create a simple plane for shadow base mesh
        val shadowMesh = MeshBuilder.CreatePlane("ShadowBase", object : CreatePlaneOptions {
            override val width = 1f
            override val height = 1f
        }, scene)

        // Configure the shadow material
        val shadowMaterial = StandardMaterial("Shadow Material", scene)
        shadowMaterial.disableColorWrite = true
        shadowMaterial.disableDepthWrite = true
        shadowMesh.material = shadowMaterial
        shadowMesh.isVisible = false

        // Initialize the shadow base mesh
        this.shadowBaseMesh = shadowMesh

        // Tiles
        val mat = StandardMaterial("mat", scene)
        mat.backFaceCulling = false
        mat.roughness = 0.5f
        mat.specularColor = Color3.Black()
        mat.diffuseColor = Color3.White()
        // Default texture
        mat.diffuseTexture = Texture(
            "/assets/stone.png",
            scene,
            object : TextureOptions {
                override val samplingMode = if (useLinearSampling) 
                    Texture.NEAREST_SAMPLINGMODE 
                else 
                    Texture.TRILINEAR_SAMPLINGMODE
            }
        )

        val vertexData = VertexData()
        vertexData.positions = arrayOf()
        vertexData.indices = arrayOf()
        vertexData.normals = arrayOf()
        vertexData.uvs = arrayOf()
        this.vertexData = vertexData

        val mesh = Mesh("Ground", scene)
        mesh.material = mat
        mesh.receiveShadows = true
        vertexData.applyToMesh(mesh, true)
        this.mesh = mesh

        this.addShadowCaster(this.mesh)
    }

    fun key(position: Vector3, side: Side): String {
        return "${position.x.toInt()},${position.y.toInt()},${position.z.toInt()},$side"
    }

    /**
     * Returns a map of all tile positions to their tile IDs
     * Key format: "x,y,z,side", Value: gameTileId
     */
    fun getAllTileIds(): kotlin.collections.Map<String, String> {
        return tileTypes.toMap()
    }

    /**
     * Returns a map of all object positions to their object IDs
     * Key format: "x,y,z,side", Value: gameObjectId
     */
    fun getAllObjectIds(): kotlin.collections.Map<String, String> {
        return objectTypes.toMap()
    }

    /**
     * Parses a key in the format "x,y,z,side" and returns the position and side
     * @return Pair of Vector3 position and Side
     */
    private fun parseKey(key: String): Pair<Vector3, Side> {
        val parts = key.split(",")
        if (parts.size != 4) {
            throw IllegalArgumentException("Invalid key format: $key")
        }

        val x = parts[0].toFloat()
        val y = parts[1].toFloat()
        val z = parts[2].toFloat()
        val side = Side.fromString(parts[3])

        return Pair(Vector3(x, y, z), side)
    }

    /**
     * Returns a copy of the tileTypes map
     * Key format: "x,y,z,side", Value: gameTileId
     */
    fun getTileTypes(): kotlin.collections.Map<String, String> {
        return tileTypes.toMap()
    }

    /**
     * Returns a copy of the objectTypes map
     * Key format: "x,y,z,side", Value: gameObjectId
     */
    fun getObjectTypes(): kotlin.collections.Map<String, String> {
        return objectTypes.toMap()
    }

    /**
     * Get a map of all object options in the scene
     * Key format: "x,y,z,side", Value: options JSON string
     */
    fun getObjectOptions(): kotlin.collections.Map<String, String> {
        return objectOptions.toMap()
    }

    /**
     * Get options for a specific object
     */
    fun getObjectOptions(key: String): String? {
        return objectOptions[key]
    }

    /**
     * Sets the current GameTile to use for painting
     */
    private fun setCurrentGameTile(gameTile: GameTile?) {
        currentGameTile = gameTile

        if (gameTile?.id != null && gameTile.photo != null) {
            // Create the texture if it doesn't exist
            if (!tileTextures.containsKey(gameTile.id)) {
                val url = "$baseUrl${gameTile.photo}"
                tileTextures[gameTile.id!!] = Texture(
                    url = url,
                    scene = scene,
                    options = object : TextureOptions {
                        override val samplingMode = if (useLinearSampling) 
                            Texture.NEAREST_SAMPLINGMODE 
                        else 
                            Texture.TRILINEAR_SAMPLINGMODE
                    }
                )

                // Add the texture to the atlas with the photo URL
                addTextureToAtlas(gameTile.id!!, gameTile.photo)
            }

            // Update the main mesh's material to use the texture atlas
            updateMeshMaterial()
        }
    }

    /**
     * Adds a texture to the texture atlas
     */
    private fun addTextureToAtlas(gameTileId: String, photoUrl: String? = null) {
        textureAtlas.addTexture(gameTileId, photoUrl)
    }

    /**
     * Updates the mesh's material to use the texture atlas
     */
    private fun updateMeshMaterial() {
        // If there are no textures, return
        if (tileTextures.isEmpty()) {
            return
        }

        // Get the material
        val material = mesh.material as StandardMaterial

        // Create a texture atlas by combining all textures
        createTextureAtlas { atlasTexture ->
            material.diffuseTexture = atlasTexture
        }
    }

    /**
     * Creates a texture atlas by combining all textures
     * @param callback Called when the texture atlas is ready
     */
    private fun createTextureAtlas(callback: (Texture) -> Unit) {
        // Add all textures to the atlas
        tileTextures.keys.forEach { gameTileId ->
            textureAtlas.addTexture(gameTileId)
        }

        // Create the atlas and return it
        val atlas = textureAtlas.createAtlas()
        callback(atlas)
    }

    fun setTile(position: Vector3, side: Side, gameTile: GameTile? = null) {
        setTiles(listOf(Triple(position, side, gameTile)))
    }

    /**
     * Sets multiple tiles at once
     * @param tileList List of Triple containing position, side, and optional GameTile
     */
    fun setTiles(tileList: List<Triple<Vector3, Side, GameTile?>>) {
        if (tileList.isEmpty()) {
            return
        }

        // Precalculate keys for each tile to avoid generating them multiple times
        val keyMap = mutableMapOf<Triple<Vector3, Side, GameTile?>, String>()
        tileList.forEach { tile ->
            val (position, side, _) = tile
            val localSide = side.abs
            keyMap[tile] = key(position, localSide)
        }

        val filteredTileList = tileList.filter { tile ->
            val key = keyMap[tile]!!
            val (_, _, gameTile) = tile
            key !in tiles || tileTypes[key] != (gameTile ?: currentGameTile)?.id
        }

        // First remove any existing tiles that will be replaced
        filteredTileList.forEach { tile ->
            val (position, side, _) = tile
            val key = keyMap[tile]!!
            if (key in tiles) {
                removeTile(position, side)
            }
        }

        // Process all tiles
        val newPositions = mutableListOf<Float>()
        val newUvs = mutableListOf<Float>()
        val newIndices = mutableListOf<Int>()
        val startIndex = vertexData.positions!!.size / 3

        // Process each tile
        filteredTileList.forEachIndexed { index, tile ->
            val (position, side, gameTile) = tile
            val key = keyMap[tile]!!

            // Get the GameTile to use (either the provided one or the current one)
            val tileToUse = gameTile ?: currentGameTile

            // If no GameTile is available, skip this tile
            if (tileToUse?.id == null) {
                return@forEachIndexed
            }

            // If a specific gameTile is provided and it's different from the current one, set it
            if (gameTile != null && gameTile != currentGameTile) {
                setCurrentGameTile(gameTile)
            }

            val localSide = side.abs

            // Store the GameTile ID for this tile position
            tileTypes[key] = tileToUse.id!!

            // Calculate the current vertex count
            val c = startIndex + (index * 4)
            tiles[key] = c

            // Add indices for this tile
            newIndices.add(c + 0)
            newIndices.add(c + 1)
            newIndices.add(c + 2)
            newIndices.add(c + 2)
            newIndices.add(c + 3)
            newIndices.add(c + 0)

            // Add positions based on side
            val positions = when (localSide) {
                Side.Y -> arrayOf(
                    position.x,
                    position.y,
                    position.z,

                    position.x + 1,
                    position.y + 0,
                    position.z + 0,

                    position.x + 1,
                    position.y + 0,
                    position.z + 1,

                    position.x + 0,
                    position.y + 0,
                    position.z + 1
                )
                Side.Z -> arrayOf(
                    position.x,
                    position.y,
                    position.z,

                    position.x + 1,
                    position.y + 0,
                    position.z + 0,

                    position.x + 1,
                    position.y + 1,
                    position.z + 0,

                    position.x + 0,
                    position.y + 1,
                    position.z + 0
                )
                else -> arrayOf(
                    position.x,
                    position.y,
                    position.z,

                    position.x + 0,
                    position.y + 1,
                    position.z + 0,

                    position.x + 0,
                    position.y + 1,
                    position.z + 1,

                    position.x + 0,
                    position.y + 0,
                    position.z + 1
                )
            }

            positions.forEach { newPositions.add(it) }

            // Get the UV coordinates from the texture atlas
            val uvs = textureAtlas.getUVCoordinates(tileToUse.id!!)
            uvs.forEach { newUvs.add(it) }
        }

        // Append all the new data to the mesh
        vertexData.positions = (vertexData.positions!! + newPositions.toTypedArray())
        vertexData.uvs = (vertexData.uvs!! + newUvs.toTypedArray())
        vertexData.indices = (vertexData.indices!! + newIndices.toTypedArray())

        // Compute normals and apply to mesh
        VertexData.ComputeNormals(vertexData.positions!!, vertexData.indices!!, vertexData.normals!!)
        vertexData.applyToMesh(mesh, true)
    }

    fun removeTile(position: Vector3, side: Side) {
        // todo
        val localSide = side.abs

        val key = key(position, localSide)

        if (key !in tiles) return

        // Remove the GameTile ID from the tileTypes map
        tileTypes.remove(key)

        // Update the main mesh
        val c = tiles[key]!! / 4 // index
        val s = c * 6

        vertexData.indices = vertexData.indices!!.filterIndexed { index, _ -> index < s || index >= s + 6 }.toTypedArray()
        vertexData.positions = vertexData.positions!!.filterIndexed { index, _ -> index < c * 4 * 3 || index >= c * 4 * 3 + 4 * 3 }.toTypedArray()
        vertexData.normals = vertexData.normals!!.filterIndexed { index, _ -> index < c * 4 * 3 || index >= c * 4 * 3 + 4 * 3 }.toTypedArray()
        vertexData.uvs = vertexData.uvs!!.filterIndexed { index, _ -> index < c * 4 * 2 || index >= c * 4 * 2 + 4 * 2 }.toTypedArray()

        for (i in s until vertexData.indices!!.size) {
            vertexData.indices!![i] = vertexData.indices!![i] - 4
        }

        vertexData.applyToMesh(mesh, true)

        tiles.remove(key)

        for ((k, v) in tiles) {
            if (v >= c * 4) {
                tiles[k] = v - 4
            }
        }
    }

    fun removeObject(position: Vector3, side: Side) {
        val key = key(position, side)

        val meshes = objects[key]

        meshes?.forEach { mesh ->
            scene.removeMesh(mesh)
            removeShadowCaster(mesh)
        }

        objects.remove(key)
        objectTypes.remove(key)
    }

    fun addObject(position: Vector3, side: Side, gameObject: GameObject) {
        val key = key(position, side)
        if (objects[key] != null) {
            return
        }

        // Store the GameObject ID
        if (gameObject.id != null) {
            objectTypes[key] = gameObject.id!!
        }

        // Store the GameObject options
        if (gameObject.options != null) {
            objectOptions[key] = gameObject.options!!
        }

        // Get or create the base mesh for this object type
        val baseMesh = if (gameObject.id != null && objectBaseMeshes.containsKey(gameObject.id)) {
            // Reuse existing base mesh
            objectBaseMeshes[gameObject.id]!!
        } else {
            // Create a new base mesh for this object type
            val width = gameObject.width?.toFloatOrNull() ?: 1f
            val height = gameObject.height?.toFloatOrNull() ?: 1f

            val objectMesh = MeshBuilder.CreatePlane(
                name = "Object",
                options = object : CreatePlaneOptions {
                    override val width = width
                    override val height = height
                },
                scene = scene
            )

            var bv = objectMesh.getVerticesData(VertexBuffer.PositionKind)!!
            bv = bv.mapIndexed { index, pos -> if (index % 3 == 2) pos else if (index % 3 == 1) pos + height/2 else pos }.toTypedArray()
            objectMesh.setVerticesData(VertexBuffer.PositionKind, bv, false)

            objectMesh.isVisible = false

            // Create material with the GameObject's texture
            val material = StandardMaterial("Object Material", scene)
            material.diffuseTexture = Texture(
                url = "$baseUrl${gameObject.photo}",
                scene = scene,
                options = object : TextureOptions {
                    override val samplingMode = if (useLinearSampling) 
                        Texture.NEAREST_SAMPLINGMODE 
                    else 
                        Texture.TRILINEAR_SAMPLINGMODE
                }
            )
            material.diffuseTexture.wrapU = Texture.CLAMP_ADDRESSMODE
            material.diffuseTexture.wrapV = Texture.CLAMP_ADDRESSMODE
            material.diffuseTexture.hasAlpha = true
            material.diffuseColor = Color3.White()
            material.specularColor = Color3.Black()
            material.backFaceCulling = false

            objectMesh.material = material
            objectMesh.receiveShadows = true

            objectMesh.registerInstancedBuffer("color", 4)
            // Initialize base mesh color buffer to avoid null value during instancing
            objectMesh.instancedBuffers["color"] = Color3.White().toColor4()

            // Store the base mesh for future reuse
            if (gameObject.id != null) {
                objectBaseMeshes[gameObject.id!!] = objectMesh
            }

            objectMesh
        }

        // Create an instance of the base mesh
        val newObject = baseMesh.createInstance("Object")

        // Parse options if available - first try from the provided gameObject
        var options = try {
            gameObject.options?.let { json.decodeFromString<GameObjectOptions>(it) }
        } catch (e: Exception) {
            null
        }

        // If no options in the provided gameObject, check if we should use default options from the key
        if (options == null || (options.scaleVariation == 0f && options.colorVariation == 0f)) {
            try {
                // This is a fallback to ensure we're using the current editor settings
                val currentOptions = gameObject.options ?: objectOptions[key]
                if (currentOptions != null) {
                    options = json.decodeFromString<GameObjectOptions>(currentOptions)
                }
            } catch (e: Exception) {
                console.error("Failed to parse object options", e)
            }
        }

        // Apply scale variation if specified in options
        if (options?.scaleVariation != null && options.scaleVariation > 0f) {
            // Generate random scale factor between (1 - variation) and (1 + variation)
            val randomOffset = ((Math.random() * 2.0) - 1.0).toFloat() * options.scaleVariation
            val scaleFactor = 1f + randomOffset
            // Set uniform scaling across all axes
            newObject.scaling = Vector3(scaleFactor, scaleFactor, scaleFactor)
        }

        // Apply color variation if specified in options
        if (options?.colorVariation != null && options.colorVariation > 0) {
            // Generate random color variation
            val r = 1f + (Math.random() * 2f - 1f) * options.colorVariation
            val g = 1f + (Math.random() * 2f - 1f) * options.colorVariation
            val b = 1f + (Math.random() * 2f - 1f) * options.colorVariation

            // Apply color variation to the instance's color buffer
            newObject.instancedBuffers["color"] = Color4(
                r = r.coerceIn(0f, 1f),
                g = g.coerceIn(0f, 1f),
                b = b.coerceIn(0f, 1f),
                a = 1f
            )
        } else {
            newObject.instancedBuffers["color"] = Color3.White().toColor4()
        }

        fun setupObjectRotation(
            newObject: AbstractMesh,
            position: Vector3,
            offset: Vector3,
            initialRotation: (AbstractMesh) -> Unit,
            upVector: Vector3,
            zeroAxis: (Vector3) -> Unit
        ) {
            newObject.position.copyFrom(position.add(offset))
            initialRotation(newObject)
            newObject.rotationQuaternion = Quaternion.Identity()
            newObject.onAfterWorldMatrixUpdateObservable.add { node ->
                val v = node.position.subtract(scene.activeCamera!!.globalPosition)
                val info = rotationBlendInfos.getOrPut(node) { RotationBlendInfo() }
                val now = Date().getTime()
                if (v.length() > 5f) {
                    zeroAxis(v)
                    val targetQ = Quaternion()
                    val normalizedV = v.normalize()

                    // Calculate if object is behind camera using dot product
                    val cameraDirection = scene.activeCamera!!.getForwardRay().direction
                    val isBehind = Vector3.Dot(normalizedV, cameraDirection) < 0

                    Quaternion.FromLookDirectionRHToRef(
                        direction = normalizedV,
                        up = upVector,
                        result = targetQ
                    )

                    // Add 180-degree rotation if behind camera
                    if (isBehind) {
                        val rotation180 = Quaternion.RotationAxis(upVector, Math.PI)
                        targetQ.multiplyInPlace(rotation180)
                    }

                    if (!info.blending) {
                        info.blending = true
                        info.startTime = now
                        info.startRotation = node.rotationQuaternion!!.clone()
                    }
                    val t = ((now - info.startTime) / 1000.0).toFloat().coerceIn(0f, 1f)
                    Quaternion.SlerpToRef(
                        start = info.startRotation,
                        end = targetQ,
                        amount = t,
                        result = node.rotationQuaternion!!
                    )
                } else {
                    info.blending = false
                    node.rotationQuaternion = node.rotationQuaternion!!
                }
            }
        }

        when (side) {
            Side.Y -> {
                setupObjectRotation(
                    newObject,
                    position,
                    Vector3(0.5f, 0f, 0.5f),
                    { _ -> },
                    Vector3.Up(),
                    { v -> v.y = 0f }
                )
            }

            Side.X -> {
                setupObjectRotation(
                    newObject,
                    position,
                    Vector3(0f, 0.5f, 0.5f),
                    { obj -> obj.rotation.z = -Math.PI / 2 },
                    Vector3.Right(),
                    { v -> v.x = 0f }
                )
            }

            Side.Z -> {
                setupObjectRotation(
                    newObject,
                    position,
                    Vector3(0.5f, 0.5f, 0f),
                    { obj -> obj.rotation.x = Math.PI / 2 },
                    Vector3.Forward(),
                    { v -> v.z = 0f }
                )
            }

            Side.NEGATIVE_Y -> {
                setupObjectRotation(
                    newObject,
                    position,
                    Vector3(0.5f, 0f, 0.5f),
                    { obj -> obj.rotation.z = -Math.PI },
                    Vector3.Down(),
                    { v -> v.y = 0f }
                )
            }

            Side.NEGATIVE_X -> {
                setupObjectRotation(
                    newObject,
                    position,
                    Vector3(0f, 0.5f, 0.5f),
                    { obj -> obj.rotation.z = Math.PI / 2 },
                    Vector3.Left(),
                    { v -> v.x = 0f }
                )
            }

            Side.NEGATIVE_Z -> {
                setupObjectRotation(
                    newObject,
                    position,
                    Vector3(0.5f, 0.5f, 0f),
                    { obj -> obj.rotation.x = -Math.PI / 2 },
                    Vector3.Backward(),
                    { v -> v.z = 0f }
                )
            }
        }

        // Store the object without shadow for now
        objects[key] = listOf(newObject)
    }
}
