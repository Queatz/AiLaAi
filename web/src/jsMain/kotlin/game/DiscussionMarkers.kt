package game

import com.queatz.db.GameDiscussionExtended
import com.queatz.db.Vector3Data
import lib.ActionManager
import lib.Color3
import lib.CreatePlaneOptions
import lib.CreateSphereOptions
import lib.DynamicTexture
import lib.DynamicTextureOptions
import lib.Engine
import lib.ExecuteCodeAction
import lib.GlowLayer
import lib.Material
import lib.Mesh
import lib.MeshBuilder
import lib.PointerEventTypes
import lib.Scene
import lib.StandardMaterial
import lib.Vector3
import lib.VertexBuffer

/**
 * Class to manage discussion markers in the 3D scene
 */
class DiscussionMarkers(private val scene: Scene) {
    private val markers = mutableMapOf<String, Mesh>()
    private val textLabels = mutableMapOf<String, Mesh>()

    // Glow layer for the markers
    private val glowLayer = GlowLayer("discussionGlow", scene).apply {
        intensity = 0.75f
        blurKernelSize = 16f
    }

    // Callback for when a marker is clicked
    var onMarkerClicked: ((String) -> Unit)? = null

    /**
     * Add a marker to the scene
     */
    fun addMarker(id: String, position: Vector3Data, comment: String? = null) {
        // Create a yellow sphere for the marker
        val markerMesh = MeshBuilder.CreateSphere(id, object : CreateSphereOptions {
            override val diameter = 0.5f
            override val segments = 16
        }, scene)

        // Position the marker
        markerMesh.position = Vector3(position.x, position.y, position.z)

        // Create a yellow material
        val material = StandardMaterial("marker-material-$id", scene)
        material.diffuseColor = Color3(1f, 1f, 0f) // Yellow color
        material.emissiveColor = Color3(.2f, .2f, 0f) // Add emissive color for glow
        material.specularColor = Color3(0.1f, 0.1f, 0.1f)
        markerMesh.material = material

        // Add click handling to the marker
        markerMesh.actionManager = ActionManager(scene)
        markerMesh.actionManager?.registerAction(
            ExecuteCodeAction(
                trigger = PointerEventTypes.POINTERDOWN
            ) {
                // Call the callback with the marker ID when clicked
                onMarkerClicked?.invoke(id)
            }
        )

        // Store the marker
        markers[id] = markerMesh

        // Add the marker to the glow layer
        glowLayer.addIncludedOnlyMesh(markerMesh)

        // Add text label if comment is provided
        if (!comment.isNullOrBlank()) {
            addTextLabel(id, position, comment)
        }
    }

    /**
     * Add a text label next to a marker
     */
    private fun addTextLabel(id: String, position: Vector3Data, text: String) {
        // Measurement setup
        val measureTexture = DynamicTexture(
            name = "measure-texture",
            options = object : DynamicTextureOptions {
                override val width = 1
                override val height = 1
            },
            scene = scene,
            format = Engine.TEXTUREFORMAT_ALPHA
        )
        val fontSize = 32f
        val measureContext = measureTexture.getContext()
        measureContext.font = "bold ${fontSize}px 'Ysabeau Infant'"

        val displayText = text.take(50)
        val metrics = measureContext.measureText(displayText)
        val textWidth = (metrics.actualBoundingBoxRight - metrics.actualBoundingBoxLeft).toFloat()
        val textHeight = (metrics.actualBoundingBoxAscent + metrics.actualBoundingBoxDescent).toFloat()

        // Texture dimensions - maintain aspect ratio
        val textureAspectRatio = textWidth / textHeight
        val baseTextureHeight = textHeight.toInt()
        val textureWidth = (baseTextureHeight * textureAspectRatio).toInt()
        val textureHeight = baseTextureHeight

        // Plane dimensions - match texture aspect ratio
        val planeHeight = .125f
        val planeWidth = planeHeight * textureAspectRatio

        // Create the plane
        val plane = MeshBuilder.CreatePlane("text-$id", object : CreatePlaneOptions {
            override val width = planeWidth
            override val height = planeHeight
        }, scene)

        var bv = plane.getVerticesData(VertexBuffer.PositionKind)!!
        bv = bv.mapIndexed { index, pos ->
            if (index % 3 == 1) pos + 0.5f else pos
        }.toTypedArray()
        plane.setVerticesData(VertexBuffer.PositionKind, bv, false)


        // Position and billboard setup
        plane.position = Vector3(position.x, position.y, position.z)
        plane.billboardMode = Mesh.BILLBOARDMODE_ALL

        // Create the render texture
        val textTexture = DynamicTexture(
            name = "text-texture-$id",
            options = object : DynamicTextureOptions {
                override val width = textureWidth
                override val height = textureHeight
            },
            scene = scene,
            format = Engine.TEXTUREFORMAT_ALPHA
        )
        textTexture.hasAlpha = true

        // Draw text
        textTexture.drawText(
            text = displayText,
            x = 0,
            y = (textureHeight - metrics.actualBoundingBoxDescent).toInt(),
            font = "bold ${fontSize}px 'Ysabeau Infant'",
            color = "white",
            clearColor = "transparent",
            update = true
        )

        // Material setup
        val material = StandardMaterial("text-material-$id", scene)
        with(material) {
            diffuseTexture = textTexture
            specularColor = Color3(0f, 0f, 0f)
            backFaceCulling = false
            emissiveColor = Color3(1f, 1f, 1f)
            disableLighting = true
            useAlphaFromDiffuseTexture = true
            transparencyMode = Material.MATERIAL_ALPHATEST
        }

        plane.material = material
        textLabels[id] = plane

        // Cleanup
        measureTexture.dispose()
    }

    /**
     * Focus the camera on a marker
     */
    fun focusOnMarker(id: String, camera: Camera) {
        markers[id]?.let { marker ->
            // Set the camera target to the marker position
            camera.camera.setTarget(marker.position.clone())
        }
    }

    /**
     * Remove a marker from the scene
     */
    fun removeMarker(id: String) {
        markers[id]?.let { 
            // Remove from glow layer first
            glowLayer.removeIncludedOnlyMesh(it)
            // Then remove from scene
            scene.removeMesh(it) 
        }
        markers.remove(id)

        textLabels[id]?.let { scene.removeMesh(it) }
        textLabels.remove(id)
    }

    /**
     * Clear all markers
     */
    fun clearMarkers() {
        markers.forEach { (_, mesh) -> 
            // Remove from glow layer first
            glowLayer.removeIncludedOnlyMesh(mesh)
            // Then remove from scene
            scene.removeMesh(mesh) 
        }
        markers.clear()

        textLabels.forEach { (_, mesh) -> scene.removeMesh(mesh) }
        textLabels.clear()
    }

    /**
     * Update markers based on discussions
     */
    fun updateMarkers(discussions: List<GameDiscussionExtended>) {
        // Clear existing markers
        clearMarkers()

        // Add markers for all discussions
        discussions.forEach { discussion ->
            val discussionObj = discussion.discussion
            if (discussionObj != null) {
                val id = discussionObj.id
                val position = discussionObj.position
                val comment = discussionObj.comment
                if (id != null && position != null) {
                    addMarker(id, position, comment)
                }
            }
        }
    }

    /**
     * Dispose of resources
     */
    fun dispose() {
        // Clear all markers
        clearMarkers()

        // Dispose of the glow layer
        glowLayer.dispose()
    }
}
