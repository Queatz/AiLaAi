package game

import com.queatz.db.SketchLayerData
import com.queatz.db.SketchLineData
import lib.Scene
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
import lib.Vector3
import lib.Color4
import lib.MeshBuilder
import lib.Matrix
import lib.PointerEventTypes
import lib.Mesh
import lib.StandardMaterial
import lib.Color3
import kotlin.js.json
import kotlin.random.Random.Default.nextInt

/**
 * Manager for freehand sketching on the map.
 * This class handles layers, lines, and drawing state.
 */

/**
 * Manager for freehand sketching on the map.
 * This class handles layers, lines, and drawing state.
 */
class SketchManager(private val scene: Scene) {
    /** Sketch layer model */
    data class Layer(
        val id: String,
        var name: String,
        var initialVisible: Boolean = true,
        var initialGlow: Boolean = false,
        val lines: MutableList<Line> = mutableStateListOf()
    ) {
        var visible by mutableStateOf(initialVisible)
        var glow by mutableStateOf(initialGlow)
    }
    /** Single sketch line with list of points */
    data class Line(
        val points: MutableList<Vector3> = mutableListOf(),
        var color: Color4 = Color4(0f, 0f, 0f, 1f),
        var thickness: Int = 1,
        var mesh: Mesh? = null
    )

    /** All sketch layers (observable) */
    val layers = mutableStateListOf<Layer>()
    /** Default color for new sketch lines */
    var currentColor: Color4 = Color4(0f, 0f, 0f, 1f)
    /** Default thickness for new sketch lines */
    var currentThickness: Int = 1
    /** Currently selected layer for new sketches */
    private val _currentLayer = mutableStateOf<Layer?>(null)
    var currentLayer: Layer
        get() = _currentLayer.value!!
        set(value) { _currentLayer.value = value }
    /** Whether the manager is in sketching mode */
    var isActive: Boolean = false

    init {
        // Initialize with a default layer
        currentLayer = createLayer("Layer 1")
    }

    /** Create a new sketch layer */
    fun createLayer(name: String = "Layer ${layers.size + 1}"): Layer {
        val layer = Layer(id = "layer_${nextInt()}", name = name)
        layers.add(layer)
        currentLayer = layer
        return layer
    }

    /** Select an existing layer by id */
    fun selectLayer(id: String) {
        currentLayer = layers.find { it.id == id }
            ?: if (layers.isNotEmpty()) {
                // If the requested layer doesn't exist but there are other layers, select the first one
                layers.first()
            } else {
                // If no layers exist, create a new one
                createLayer("Layer 1")
            }
    }

    /** Toggle visibility of a layer by id */
    fun toggleVisibility(id: String) {
        layers.find { it.id == id }?.let { layer ->
            layer.visible = !layer.visible
            layer.lines.forEach { line ->
                line.mesh?.isVisible = layer.visible
            }
        }
    }

    /** Toggle glow effect of a layer by id */
    fun toggleGlow(id: String) {
        layers.find { it.id == id }?.let { layer ->
            layer.glow = !layer.glow
            // Update materials for all lines in the layer
            layer.lines.forEach { line ->
                updateLineMaterial(line, layer.glow)
            }
        }
    }

    /** Update a line's material based on glow state */
    private fun updateLineMaterial(line: Line, glow: Boolean) {
        line.mesh?.let { mesh ->
            val mat = mesh.material as? StandardMaterial ?: return
            // Set emissive color equal to diffuse color when glow is enabled
            if (glow) {
                mat.emissiveColor = Color3(line.color.r, line.color.g, line.color.b)
            } else {
                mat.emissiveColor = Color3(0f, 0f, 0f)
            }
        }
    }

    private var isDrawing: Boolean = false
    private var currentLine: Line? = null
    private var currentSketchMesh: Mesh? = null
    /** Handle pointer events for sketching on the given drawing plane and offset */
    fun handlePointer(event: lib.PointerInfo, drawPlane: Side, offset: Int, eraser: Boolean = false) {
        when (event.type) {
            PointerEventTypes.POINTERDOWN -> {
                isDrawing = true
                // Compute pick point on desired plane
                val camera = scene.activeCamera ?: return
                val ray = scene.createPickingRay(scene.pointerX, scene.pointerY, Matrix.Identity(), camera)
                val origin = ray.origin
                val dir = ray.direction
                // Solve for t where ray intersects plane coord = offset
                val p = when (drawPlane) {
                    Side.Y, Side.NEGATIVE_Y -> {
                        val t = if (dir.y != 0f) (offset - origin.y) / dir.y else 0f
                        Vector3(origin.x + dir.x * t, offset.toFloat(), origin.z + dir.z * t)
                    }
                    Side.X, Side.NEGATIVE_X -> {
                        val t = if (dir.x != 0f) (offset - origin.x) / dir.x else 0f
                        Vector3(offset.toFloat(), origin.y + dir.y * t, origin.z + dir.z * t)
                    }
                    Side.Z, Side.NEGATIVE_Z -> {
                        val t = if (dir.z != 0f) (offset - origin.z) / dir.z else 0f
                        Vector3(origin.x + dir.x * t, origin.y + dir.y * t, offset.toFloat())
                    }
                }

                // If eraser mode is active, find and remove lines that are close to the cursor
                if (eraser) {
                    eraseNearbyLines(p)
                    return
                }

                // Start new line data
                val line = Line(mutableListOf(p), color = currentColor, thickness = currentThickness)
                currentLayer.lines.add(line)
                currentLine = line
                // Initialize tube mesh with two identical points for visibility
                currentSketchMesh?.dispose()
                val pts = arrayOf(p, p)
                val radius = line.thickness * 0.01f
                val opts = json("path" to pts, "radius" to radius, "updatable" to true)
                val mesh = MeshBuilder.CreateTube("sketch_line", opts, scene)
                // Apply material for color and alpha
                val mat = StandardMaterial("sketch_mat", scene)
                mat.diffuseColor = Color3(line.color.r, line.color.g, line.color.b)
                mat.alpha = line.color.a
                mat.backFaceCulling = false
                // Set emissive color if layer has glow enabled
                if (currentLayer.glow) {
                    mat.emissiveColor = Color3(line.color.r, line.color.g, line.color.b)
                }
                mesh.material = mat
                line.mesh = mesh
            }
            PointerEventTypes.POINTERMOVE -> if (isDrawing) {
                val camera = scene.activeCamera ?: return
                val ray = scene.createPickingRay(scene.pointerX, scene.pointerY, Matrix.Identity(), camera)
                val origin = ray.origin
                val dir = ray.direction
                // Solve for t where ray intersects plane coord = offset
                val p = when (drawPlane) {
                    Side.Y, Side.NEGATIVE_Y -> {
                        val t = if (dir.y != 0f) (offset - origin.y) / dir.y else 0f
                        Vector3(origin.x + dir.x * t, offset.toFloat(), origin.z + dir.z * t)
                    }
                    Side.X, Side.NEGATIVE_X -> {
                        val t = if (dir.x != 0f) (offset - origin.x) / dir.x else 0f
                        Vector3(offset.toFloat(), origin.y + dir.y * t, origin.z + dir.z * t)
                    }
                    Side.Z, Side.NEGATIVE_Z -> {
                        val t = if (dir.z != 0f) (offset - origin.z) / dir.z else 0f
                        Vector3(origin.x + dir.x * t, origin.y + dir.y * t, offset.toFloat())
                    }
                }

                // If eraser mode is active, continue erasing lines that are close to the cursor
                if (eraser) {
                    eraseNearbyLines(p)
                    return
                }

                currentLine?.let { line ->
                    line.points.add(p)
                    // Recreate tube mesh for updated path
                        currentSketchMesh?.dispose()
                        val pathPts = line.points.toTypedArray()
                        val radius2 = line.thickness * 0.01f
                        val opts2 = json("path" to pathPts, "radius" to radius2, "updatable" to true)
                        val newMesh = MeshBuilder.CreateTube("sketch_line", opts2, scene)
                        val mat2 = StandardMaterial("sketch_mat", scene)
                        mat2.diffuseColor = Color3(line.color.r, line.color.g, line.color.b)
                        mat2.alpha = line.color.a
                        mat2.backFaceCulling = false
                        // Set emissive color if layer has glow enabled
                        if (currentLayer.glow) {
                            mat2.emissiveColor = Color3(line.color.r, line.color.g, line.color.b)
                        }
                        newMesh.material = mat2
                        currentSketchMesh = newMesh
                        line.mesh = newMesh
                }
            }
            PointerEventTypes.POINTERUP -> {
                isDrawing = false
                currentLine = null
                currentSketchMesh = null
            }
        }
    }
    /** Cancel the current line drawing before completion */
    fun cancelCurrentLine() {
        currentLine?.let { line ->
            // Remove line data
            currentLayer.lines.remove(line)
        }
        // Remove mesh
        currentSketchMesh?.let { mesh ->
            mesh.dispose()
            scene.removeMesh(mesh)
        }
        isDrawing = false
        currentLine = null
        currentSketchMesh = null
    }
    /** Remove an entire sketch layer and its meshes */
    fun removeLayer(id: String) {
        // Don't allow removing the last layer
        if (layers.size <= 1) {
            return
        }

        val layer = layers.find { it.id == id } ?: return
        // Dispose and remove all meshes in the layer
        layer.lines.forEach { line ->
            line.mesh?.apply {
                dispose()
                scene.removeMesh(this)
            }
        }
        // Remove layer data
        layers.remove(layer)
        // If current layer, select another
        if (currentLayer.id == id) {
            currentLayer = layers.firstOrNull() ?: createLayer("Layer 1")
        }
    }

    /**
     * Erases lines that are close to the specified point
     */
    private fun eraseNearbyLines(point: Vector3) {
        // Define the maximum distance for erasing (adjust as needed)
        val eraseDistance = 0.5f

        // Iterate through all layers
        layers.forEach { layer ->
            // Only process visible layers
            if (layer.visible) {
                // Create a list to hold lines that should be removed
                val linesToRemove = mutableListOf<Line>()

                // Check each line in the layer
                layer.lines.forEach { line ->
                    // Check if any point in the line is close to the erase point
                    for (i in 0 until line.points.size - 1) {
                        val p1 = line.points[i]
                        val p2 = line.points[i + 1]

                        // Calculate distance from point to line segment
                        val distance = distanceToLineSegment(point, p1, p2)

                        // If the distance is less than the threshold, mark the line for removal
                        if (distance < eraseDistance) {
                            linesToRemove.add(line)
                            break
                        }
                    }
                }

                // Remove the marked lines
                linesToRemove.forEach { line ->
                    // Dispose the mesh
                    line.mesh?.dispose()
                    line.mesh?.let { scene.removeMesh(it) }

                    // Remove the line from the layer
                    layer.lines.remove(line)
                }
            }
        }
    }

    /**
     * Calculates the distance from a point to a line segment
     */
    private fun distanceToLineSegment(point: Vector3, lineStart: Vector3, lineEnd: Vector3): Float {
        val lineVector = lineEnd.subtract(lineStart)
        val pointVector = point.subtract(lineStart)

        val lineLength = lineVector.length()
        if (lineLength < 0.0001f) {
            // Line segment is too short, just return distance to one endpoint
            return pointVector.length()
        }

        // Calculate dot product manually
        val dotProduct = pointVector.x * lineVector.x + pointVector.y * lineVector.y + pointVector.z * lineVector.z

        // Calculate projection of point onto line
        val t = dotProduct / (lineLength * lineLength)

        // Clamp t to [0,1] to ensure we're on the line segment
        val tClamped = t.coerceIn(0f, 1f)

        // Calculate the closest point on the line segment
        val closestPoint = lineStart.add(lineVector.scale(tClamped))

        // Return the distance to the closest point
        return point.subtract(closestPoint).length()
    }

    /** Load sketch data from serialized model */
    fun loadData(model: List<SketchLayerData>) {
        layers.clear()
        model.forEach { layerData ->
            val layer = Layer(id = layerData.id, name = layerData.name, initialVisible = layerData.visible, initialGlow = layerData.glow)
            layerData.lines.forEach { lineData ->
                val line = Line(
                    color = Color4(lineData.color.r, lineData.color.g, lineData.color.b, lineData.color.a),
                    thickness = lineData.thickness
                )
                // Populate points
                lineData.points.forEach { p -> line.points.add(Vector3(p.x, p.y, p.z)) }
                // Create mesh for this line
                if (line.points.size >= 2) {
                    val pts = line.points.toTypedArray()
                    val radius = line.thickness * 0.01f
                    val opts = json("path" to pts, "radius" to radius, "updatable" to false)
                    val tube = MeshBuilder.CreateTube("sketch_${layer.id}", opts, scene)
                    // Apply material
                    val mat = StandardMaterial("sketch_mat_${layer.id}", scene)
                    mat.diffuseColor = Color3(line.color.r, line.color.g, line.color.b)
                    mat.alpha = line.color.a
                    mat.backFaceCulling = false
                    // Set emissive color if layer has glow enabled
                    if (layer.glow) {
                        mat.emissiveColor = Color3(line.color.r, line.color.g, line.color.b)
                    }
                    tube.material = mat
                    tube.isVisible = layer.visible
                    line.mesh = tube
                }
                layer.lines.add(line)
            }
            layers.add(layer)
        }
        if (layers.isNotEmpty()) {
            currentLayer = layers.first()
        } else {
            // If no layers were loaded, create a default layer
            currentLayer = createLayer("Layer 1")
        }
    }

    /** Convert internal sketch data to serializable model */
    fun toModel(): List<SketchLayerData> {
        return layers.map { layer ->
            SketchLayerData(
                id = layer.id,
                name = layer.name,
                visible = layer.visible,
                glow = layer.glow,
                lines = layer.lines.map { line ->
                    SketchLineData(
                        points = line.points.map { p -> com.queatz.db.Vector3Data(p.x, p.y, p.z) },
                        color = com.queatz.db.Color4Data(line.color.r, line.color.g, line.color.b, line.color.a),
                        thickness = line.thickness,
                        layerId = layer.id
                    )
                }
            )
        }
    }
}
