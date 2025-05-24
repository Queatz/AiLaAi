package game

import com.queatz.db.SavedPositionData
import ellipsize
import lib.ActionManager
import lib.Color3
import lib.CreatePlaneOptions
import lib.CreateSphereOptions
import lib.DynamicTexture
import lib.DynamicTextureOptions
import lib.Engine
import lib.ExecuteCodeAction
import lib.GlowLayer
import lib.Mesh
import lib.MeshBuilder
import lib.PointerEventTypes
import lib.Scene
import lib.StandardMaterial
import lib.Vector3
import lib.VertexBuffer

/**
 * Class to manage saved positions in the 3D scene
 */
class SavedPositions(private val scene: Scene) {
    private val markers = mutableMapOf<String, Mesh>()
    private val textLabels = mutableMapOf<String, Mesh>()
    private var visible = true

    // Glow layer for the markers
    private val glowLayer = GlowLayer("savedPositionsGlow", scene).apply {
        intensity = 0.75f
        blurKernelSize = 16f
    }

    // Callback for when a marker is clicked
    var onMarkerClicked: ((String) -> Unit)? = null

    /**
     * Add a marker to the scene
     */
    fun addMarker(id: String, position: Vector3, name: String? = null) {
        // Create a white sphere for the marker
        val markerMesh = MeshBuilder.CreateSphere(id, object : CreateSphereOptions {
            override val diameter = 0.5f
            override val segments = 16
        }, scene)

        // Position the marker
        markerMesh.position = position.clone()

        // Set visibility based on current setting
        markerMesh.isVisible = visible

        // Create a white material
        val material = StandardMaterial("marker-material-$id", scene)
        material.diffuseColor = Color3(1f, 1f, 1f) // White color
        material.emissiveColor = Color3(.2f, .2f, .2f) // Add emissive color for glow
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

        // Add text label if name is provided
        if (!name.isNullOrBlank()) {
            addTextLabel(id, position, name)
        }
    }

    /**
     * Add a text label next to a marker
     */
    private fun addTextLabel(id: String, position: Vector3, text: String) {
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
        val fontSize = 48f  // Increased font size from 32f to 48f
        val measureContext = measureTexture.getContext()
        measureContext.font = "bold ${fontSize}px 'Ysabeau Infant'"

        val displayText = text.ellipsize(50)
        val metrics = measureContext.measureText(displayText)
        val textWidth = (metrics.actualBoundingBoxRight - metrics.actualBoundingBoxLeft).toFloat()
        val textHeight = (metrics.actualBoundingBoxAscent + metrics.actualBoundingBoxDescent).toFloat()

        // Texture dimensions - maintain aspect ratio
        val textureAspectRatio = textWidth / textHeight
        val baseTextureHeight = textHeight.toInt()
        val textureWidth = (baseTextureHeight * textureAspectRatio).toInt()
        val textureHeight = baseTextureHeight

        // Plane dimensions - match texture aspect ratio
        val planeHeight = .2f  // Increased from .125f to match the larger font size
        val planeWidth = (planeHeight * textureAspectRatio).toFloat()

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
        plane.position = position.clone()
        plane.billboardMode = Mesh.BILLBOARDMODE_ALL

        // Set visibility based on current setting
        plane.isVisible = visible

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
            emissiveTexture = textTexture
            opacityTexture = textTexture
            backFaceCulling = false
            disableLighting = true
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

            // Ensure camera is within 20 units of the target
            if (camera.camera.radius > 20f) {
                camera.camera.radius = 20f
            }
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
     * Update markers based on saved positions
     */
    fun updateMarkers(savedPositions: List<SavedPositionData>) {
        // Remember current visibility state
        val currentVisibility = visible

        // Clear existing markers
        clearMarkers()

        // Add markers for all saved positions
        savedPositions.forEach { savedPosition ->
            val id = savedPosition.id
            val position = savedPosition.position
            val name = savedPosition.name
            addMarker(
                id = id,
                position = Vector3(position.x, position.y, position.z),
                name = name
            )
        }

        // Ensure visibility state is maintained after update
        if (!currentVisibility) {
            setVisible(false)
        }
    }

    /**
     * Set visibility of all markers
     */
    fun setVisible(isVisible: Boolean) {
        if (visible == isVisible) return

        visible = isVisible

        // Update visibility of all existing markers
        markers.values.forEach { it.isVisible = visible }
        textLabels.values.forEach { it.isVisible = visible }
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
