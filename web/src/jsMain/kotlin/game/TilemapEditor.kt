package game

import androidx.compose.runtime.Composable
import androidx.compose.runtime.mutableStateOf
import com.queatz.db.GameObject
import com.queatz.db.GameMusic
import com.queatz.db.GameTile
import lib.Color3
import lib.CreateGroundOptions
import lib.CreatePlaneOptions
import lib.GlowLayer
import lib.GridMaterial
import lib.Math
import lib.Math.abs
import lib.Math.min
import lib.Matrix
import lib.Mesh
import lib.MeshBuilder
import lib.Plane
import lib.Scene
import lib.StandardMaterial
import lib.Texture
import lib.Vector3
import lib.VertexBuffer
import kotlin.math.max

enum class DrawMode {
    Tile,
    Object,
    Clone
}

class TilemapEditor(
    private val scene: Scene,
    val tilemap: Tilemap,
    val toolState: ToolState
) {
    val cursor: Mesh
    var grid: Mesh

    // Glow layer for clone tool visualization
    private val cloneGlowLayer = GlowLayer("cloneGlow", scene).apply {
        intensity = .8f
        blurKernelSize = 16f
    }
    // Glow layer for cursor visualization
    private val cursorGlowLayer = GlowLayer("cursorGlow", scene).apply {
        intensity = .8f
        blurKernelSize = 16f
    }

    // Clone tool selection visualization
    private var cloneSelectionPlane: Mesh? = null
    private var cloneSelectionAdditionalPlanes: MutableList<Mesh> = mutableListOf()

    // Clone insertion preview visualization
    private var cloneInsertionPreview: Mesh? = null
    private var cloneInsertionAdditionalPlanes: MutableList<Mesh> = mutableListOf()

    // Clone selection state
    enum class CloneSelectionState {
        NotStarted,
        FirstPointSelected,
        SecondPointSelected,
        Complete
    }

    // Clone selection variables
    private var cloneSelectionState: CloneSelectionState = CloneSelectionState.NotStarted
    private var cloneFirstPoint: Vector3 = Vector3.Zero()
    private var cloneSecondPoint: Vector3 = Vector3.Zero()
    private var cloneHeight: Int = 1

    // Clone selection box dimensions
    private var cloneSelectionWidth: Int = 0
    private var cloneSelectionDepth: Int = 0
    private var cloneSelectionHeight: Int = 0

    // Stored tiles for cloning
    private var clonedTiles: MutableMap<String, String> = mutableMapOf()

    // Flag to track if initialization is complete
    private var initialized = false

    // Flag to control whether editing is allowed
    var editable: Boolean = true

    var pickedPoint: Vector3 = Vector3.Zero()
    var tilePos = Vector3.Zero()
    var curPosAnimated: Vector3? = null
    // These properties now delegate to toolState
    var side: Side = Side.Y
    var drawPlane: Side = Side.Y
    var drawPlaneOffset: Int = 0

    var drawMode: DrawMode = DrawMode.Tile
        set(value) {
            field = value
            // Update toolState based on the draw mode
            when (value) {
                DrawMode.Tile -> toolState.selectTool(ToolType.DRAW)
                DrawMode.Object -> toolState.selectTool(ToolType.DRAW)
                DrawMode.Clone -> toolState.selectTool(ToolType.CLONE)
            }
        }
    var autoRotate = true
    var brushShape: String = "square"
    var brushSize = 1
        set(value) {
            field = value
            updateCursorSize()
        }
    var brushDensity = 100

    // Grid size property (11-101)
    var gridSize = 51
        set(value) {
            // Store the old value
            val oldValue = field
            // Set the new value (11-101)
            field = value
            // Only recreate the grid if the value has changed and initialization is complete
            if (oldValue != value && initialized) {
                recreateGrid()
            }
        }

    // Grid line width property (1-10)
    var gridLineAlpha = 5
        set(value) {
            // Store the old value
            val oldValue = field
            // Set the new value (1-10)
            field = value
            // Only update the grid material if the value has changed and initialization is complete
            if (oldValue != value && initialized) {
                updateGridLineAlpha()
            }
        }

    // Updates the grid line width by adjusting the opacity property of the GridMaterial
    // This keeps the grid density constant while changing the perceived width of the lines
    private fun updateGridLineAlpha() {
        // Get the grid material
        val gridMaterial = grid.material as? GridMaterial ?: return

        // Keep gridRatio constant to maintain grid density
        // Use a fixed value that provides a good grid density
        gridMaterial.gridRatio = 1f

        // Adjust opacity based on gridLineAlpha to control perceived line width
        // Higher gridLineAlpha means higher opacity (thicker lines)
        // Scale to a reasonable range (0.1 to 0.5)
        val baseOpacity = 0.1f
        val normalized = (gridLineAlpha - 1) / 9f  // Normalize to 0-1 range (gridLineAlpha is 1-10)
        gridMaterial.opacity = (baseOpacity + normalized).coerceIn(.1f, .99f)
    }

    // Current GameTile to paint with
    private var _currentGameTile = mutableStateOf<GameTile?>(null)
    var currentGameTile: GameTile?
        get() = _currentGameTile.value
        set(value) {
            _currentGameTile.value = value
            // Update toolState as well
            toolState.setCurrentGameTile(value)
        }
    // Public getter for the state object
    @Composable
    fun getCurrentGameTileState() = _currentGameTile

    // Current GameObject to place
    private var _currentGameObject = mutableStateOf<GameObject?>(null)
    var currentGameObject: GameObject?
        get() = _currentGameObject.value
        set(value) {
            _currentGameObject.value = value
            // Update toolState as well
            toolState.setCurrentGameObject(value)
        }
    // Public getter for the state object
    @Composable
    fun getCurrentGameObjectState() = _currentGameObject

    // Current GameMusic to play
    private var _currentGameMusic = mutableStateOf<GameMusic?>(null)
    var currentGameMusic: GameMusic?
        get() = _currentGameMusic.value
        set(value) {
            _currentGameMusic.value = value
            // Update toolState as well
            toolState.setCurrentGameMusic(value)
        }
    // Public getter for the state object
    @Composable
    fun getCurrentGameMusicState() = _currentGameMusic

    init {
        // Create a square cursor that represents the brush area
        val cursor = MeshBuilder.CreatePlane("tile", object : CreatePlaneOptions {
            override val width = 1f
            override val height = 1f
            override val updatable = true
        }, scene)
        // Create a semi-transparent material for the cursor
        val material = StandardMaterial("cursorMaterial", scene)
        material.emissiveColor = Color3.White()
        material.specularColor = Color3.Black()
        material.alpha = 0.3f
        material.backFaceCulling = false  // Show both sides
        material.zOffset = -0.01f  // Draw on top
        material.zOffsetUnits = -1
        cursor.rotation = Vector3(Math.PI / 2, 0f, 0f)
        cursor.material = material
        this.cursor = cursor
        // Add cursor to glow layer for enhanced visibility
        cursorGlowLayer.addIncludedOnlyMesh(cursor)

        // Create the grid with initial size
        val brushGrid = MeshBuilder.CreateGround("Grid", object : CreateGroundOptions {
            override val width = gridSize.toFloat()
            override val height = gridSize.toFloat()
            override val subdivisions = gridSize
        }, scene)

        this.grid = brushGrid

        val grid = GridMaterial("grid", scene)
        grid.backFaceCulling = false
        grid.opacityTexture = Texture("/assets/glow.png", scene)
        grid.mainColor = Color3.Black()
        grid.lineColor = Color3(0.25f, 0.5f, 1f)
        // Set a constant gridRatio to maintain consistent grid density
        grid.gridRatio = 0.5f
        // Opacity will be set by updategridLineAlpha() based on gridLineAlpha
        grid.majorUnitFrequency = 1
        grid.zOffsetUnits = -0.01f
        grid.fogEnabled = true
        grid.gridOffset = Vector3(0.5f, 0f, 0.5f)
        grid.antialias = false

        brushGrid.material = grid

        // Initialize grid opacity based on gridLineAlpha
        updateGridLineAlpha()

        // Get the vertex data for the cursor
        var v = cursor.getVerticesData(VertexBuffer.PositionKind)!!

        // Adjust the vertices to center the cursor on the tile position
        // This ensures the cursor shows exactly where the brush will draw
        v = v.mapIndexed { index, pos -> if (index % 3 == 2) pos else pos + 0.5f }.toTypedArray()

        cursor.setVerticesData(VertexBuffer.PositionKind, v, false)

        // Initialize cursor size based on brush size
        updateCursorSize()

        // Mark initialization as complete
        initialized = true
    }


    /**
     * Recreates the grid with the current gridSize
     */
    private fun recreateGrid() {
        // Store the current material, visibility, and position
        val gridMaterial = grid.material
        val isVisible = grid.isVisible
        val gridPosition = grid.position.clone()

        // Remove the old grid from the scene
        scene.removeMesh(grid)

        // Create a new grid with the updated size
        val newGrid = MeshBuilder.CreateGround("Grid", object : CreateGroundOptions {
            override val width = gridSize.toFloat()
            override val height = gridSize.toFloat()
            override val subdivisions = gridSize
        }, scene)

        // Apply the same material, visibility, and position
        newGrid.material = gridMaterial
        newGrid.isVisible = isVisible
        newGrid.position.copyFrom(gridPosition)

        // Update the grid reference
        grid = newGrid

        // Ensure grid opacity is set correctly based on gridLineAlpha
        updateGridLineAlpha()

        // Update the grid rotation based on the current drawPlane
        when (drawPlane) {
            Side.X, Side.NEGATIVE_X -> {
                newGrid.rotation = Vector3(0f, 0f, Math.PI / 2)
            }

            Side.Y, Side.NEGATIVE_Y -> {
                newGrid.rotation = Vector3(0f, -Math.PI / 2, 0f)
            }

            Side.Z, Side.NEGATIVE_Z -> {
                newGrid.rotation = Vector3(Math.PI / 2, 0f, 0f)
            }
        }
    }

    fun update() {
        // Check if a tile or object is selected based on draw mode
        val hasSelection = when (drawMode) {
            DrawMode.Tile -> currentGameTile != null
            DrawMode.Object -> currentGameObject != null
            DrawMode.Clone -> true // Always show cursor in clone mode
        }

        // Check if sketching is active
        val isSketchingActive = toolState.isSketching

        // Show cursor and grid only when a tool is actually selected AND not in sketching mode
        cursor.isVisible = editable && !isSketchingActive && (drawMode == DrawMode.Clone || hasSelection)
        grid.isVisible = editable && (isSketchingActive || drawMode == DrawMode.Clone || hasSelection)

        // Even if nothing is selected, still update the cursor position
        // but don't allow drawing without selection

        val plane = Plane.FromPositionAndNormal(
            Vector3(
                if (drawPlane == Side.X) drawPlaneOffset.toFloat() else 0f,
                if (drawPlane == Side.Y) drawPlaneOffset.toFloat() else 0f,
                if (drawPlane == Side.Z) drawPlaneOffset.toFloat() else 0f
            ),
            if (drawPlane == Side.X) Vector3.Right() else if (drawPlane == Side.Y) Vector3.Up() else Vector3.Forward()
        )
        val ray = scene.createPickingRay(scene.pointerX, scene.pointerY, Matrix.Identity(), scene.activeCamera!!)

        ray.origin.projectOnPlaneToRef(plane, ray.origin.add(ray.direction), pickedPoint)

        updateDraw(pickedPoint)

        // Update clone selection visualization if in clone mode
        if (drawMode == DrawMode.Clone && cloneSelectionState != CloneSelectionState.NotStarted) {
            updateCloneSelection()

            // Show insertion preview if clone selection is complete
            if (cloneSelectionState == CloneSelectionState.Complete) {
                updateCloneInsertionPreview()
            } else {
                // Remove insertion preview if not in complete state
                removeCloneInsertionPreview()
            }
        } else {
            // Remove insertion preview if not in clone mode
            removeCloneInsertionPreview()
            // Remove clone selection visualization if not in clone mode
            if (cloneSelectionPlane != null) {
                cloneGlowLayer.removeIncludedOnlyMesh(cloneSelectionPlane!!)
                scene.removeMesh(cloneSelectionPlane!!)
                cloneSelectionPlane = null
            }
            cloneSelectionAdditionalPlanes.forEach { plane ->
                cloneGlowLayer.removeIncludedOnlyMesh(plane)
                scene.removeMesh(plane)
            }
            cloneSelectionAdditionalPlanes.clear()
        }
    }

    /**
     * Updates the clone insertion preview at the cursor position
     */
    private fun updateCloneInsertionPreview() {
        // Get the dimensions of the clone selection
        val width = cloneSelectionWidth
        val depth = cloneSelectionDepth
        val height = cloneSelectionHeight

        // Determine preview anchor using smoothed cursor position and brush offset
        val posAnimated = curPosAnimated ?: tilePos.clone()
        // Calculate same offset used when rendering cursor
        val offset = run {
            val r = Math.floor(brushSize / 2f)
            when (drawPlane.abs) {
                Side.X -> Vector3(0f, r, r)
                Side.Y -> Vector3(r, 0f, r)
                Side.Z -> Vector3(r, r, 0f)
                else -> Vector3.Zero()
            }
        }
        val previewPos = posAnimated.subtract(offset)
        // Create or update the insertion preview at the preview position
        createOrUpdateCloneInsertionPreview(
            position = previewPos,
            size = Vector3(width.toFloat(), height.toFloat(), depth.toFloat())
        )
    }

    /**
     * Removes the clone insertion preview
     */
    private fun removeCloneInsertionPreview() {
        if (cloneInsertionPreview != null) {
            // Remove from glow layer first
            cloneGlowLayer.removeIncludedOnlyMesh(cloneInsertionPreview!!)
            scene.removeMesh(cloneInsertionPreview!!)
            cloneInsertionPreview = null
        }

        cloneInsertionAdditionalPlanes.forEach { plane ->
            // Remove from glow layer first
            cloneGlowLayer.removeIncludedOnlyMesh(plane)
            scene.removeMesh(plane)
        }
        cloneInsertionAdditionalPlanes.clear()
    }

    /**
     * Creates or updates the clone insertion preview
     */
    private fun createOrUpdateCloneInsertionPreview(position: Vector3, size: Vector3) {
        // Remove existing preview if it exists
        removeCloneInsertionPreview()

        // Create a new plane for the base (bottom)
        cloneInsertionPreview = MeshBuilder.CreatePlane("cloneInsertionPreview", object : CreatePlaneOptions {
            override val width = size.x
            override val height = size.z
            override val updatable = true
        }, scene)

        // Create a semi-transparent material
        val material = StandardMaterial("cloneInsertionMaterial", scene)
        material.emissiveColor = Color3(1f, 1f, 0f) // Yellow color
        material.specularColor = Color3.Black()
        material.alpha = 0.3f
        material.backFaceCulling = false
        material.wireframe = true
        cloneInsertionPreview!!.material = material

        // Rotate to be horizontal (bottom plane)
        cloneInsertionPreview!!.rotation = Vector3(Math.PI / 2, 0f, 0f)

        // Position the bottom plane
        cloneInsertionPreview!!.position = position.add(Vector3(size.x / 2, 0f, size.z / 2))

        // Add to glow layer
        cloneGlowLayer.addIncludedOnlyMesh(cloneInsertionPreview!!)

        // Create top plane
        val topPlane = MeshBuilder.CreatePlane("cloneInsertionTop", object : CreatePlaneOptions {
            override val width = size.x
            override val height = size.z
            override val updatable = true
        }, scene)
        topPlane.material = material
        topPlane.rotation = Vector3(Math.PI / 2, 0f, 0f)
        topPlane.position = position.add(Vector3(size.x / 2, size.y, size.z / 2))
        cloneInsertionAdditionalPlanes.add(topPlane)
        cloneGlowLayer.addIncludedOnlyMesh(topPlane)

        // Create front plane
        val frontPlane = MeshBuilder.CreatePlane("cloneInsertionFront", object : CreatePlaneOptions {
            override val width = size.x
            override val height = size.y
            override val updatable = true
        }, scene)
        frontPlane.material = material
        frontPlane.position = position.add(Vector3(size.x / 2, size.y / 2, 0f))
        cloneInsertionAdditionalPlanes.add(frontPlane)
        cloneGlowLayer.addIncludedOnlyMesh(frontPlane)

        // Create back plane
        val backPlane = MeshBuilder.CreatePlane("cloneInsertionBack", object : CreatePlaneOptions {
            override val width = size.x
            override val height = size.y
            override val updatable = true
        }, scene)
        backPlane.material = material
        backPlane.rotation = Vector3(0f, Math.PI, 0f)
        backPlane.position = position.add(Vector3(size.x / 2, size.y / 2, size.z))
        cloneInsertionAdditionalPlanes.add(backPlane)
        cloneGlowLayer.addIncludedOnlyMesh(backPlane)

        // Create left plane
        val leftPlane = MeshBuilder.CreatePlane("cloneInsertionLeft", object : CreatePlaneOptions {
            override val width = size.z
            override val height = size.y
            override val updatable = true
        }, scene)
        leftPlane.material = material
        leftPlane.rotation = Vector3(0f, Math.PI / 2, 0f)
        leftPlane.position = position.add(Vector3(0f, size.y / 2, size.z / 2))
        cloneInsertionAdditionalPlanes.add(leftPlane)
        cloneGlowLayer.addIncludedOnlyMesh(leftPlane)

        // Create right plane
        val rightPlane = MeshBuilder.CreatePlane("cloneInsertionRight", object : CreatePlaneOptions {
            override val width = size.z
            override val height = size.y
            override val updatable = true
        }, scene)
        rightPlane.material = material
        rightPlane.rotation = Vector3(0f, -Math.PI / 2, 0f)
        rightPlane.position = position.add(Vector3(size.x, size.y / 2, size.z / 2))
        cloneInsertionAdditionalPlanes.add(rightPlane)
        cloneGlowLayer.addIncludedOnlyMesh(rightPlane)
    }

    /**
     * Updates the clone selection visualization based on the current selection state
     */
    private fun updateCloneSelection() {
        // Calculate dimensions based on selection state
        when (cloneSelectionState) {
            CloneSelectionState.FirstPointSelected -> {
                // Just show a marker at the first point
                createOrUpdateCloneSelectionPlane(
                    cloneFirstPoint,
                    Vector3(1f, 1f, 1f) // z=1f for visualization purposes
                )
            }

            CloneSelectionState.SecondPointSelected -> {
                // Show a flat plane with width and depth
                val width = abs(cloneSecondPoint.x - cloneFirstPoint.x) + 1
                val depth = abs(cloneSecondPoint.z - cloneFirstPoint.z) + 1

                // Store dimensions
                cloneSelectionWidth = width.toInt()
                cloneSelectionDepth = depth.toInt()

                // Calculate the min point (corner of the selection)
                val minX = min(cloneFirstPoint.x, cloneSecondPoint.x)
                val minZ = min(cloneFirstPoint.z, cloneSecondPoint.z)

                createOrUpdateCloneSelectionPlane(
                    Vector3(minX, cloneFirstPoint.y, minZ),
                    Vector3(width, 0.1f, depth)
                )
            }

            CloneSelectionState.Complete -> {
                // Show a plane with width and depth (height is visualized differently)
                val width = abs(cloneSecondPoint.x - cloneFirstPoint.x) + 1
                val depth = abs(cloneSecondPoint.z - cloneFirstPoint.z) + 1

                // Store dimensions
                cloneSelectionWidth = width.toInt()
                cloneSelectionDepth = depth.toInt()
                cloneSelectionHeight = cloneHeight

                // Calculate the min point (corner of the selection)
                val minX = min(cloneFirstPoint.x, cloneSecondPoint.x)
                val minZ = min(cloneFirstPoint.z, cloneSecondPoint.z)

                createOrUpdateCloneSelectionPlane(
                    Vector3(minX, cloneFirstPoint.y, minZ),
                    Vector3(width, 0.1f, depth)
                )
            }

            else -> {
                // Remove the plane if not in a selection state
                if (cloneSelectionPlane != null) {
                    scene.removeMesh(cloneSelectionPlane!!)
                    cloneSelectionPlane = null
                }
            }
        }
    }

    /**
     * Creates or updates the clone selection visualization
     * Uses multiple planes to form a box-like structure
     */
    private fun createOrUpdateCloneSelectionPlane(position: Vector3, size: Vector3) {
        // Determine the height to use for the box
        val boxHeight = if (cloneSelectionState == CloneSelectionState.Complete) cloneHeight.toFloat() else 1f

        // Remove existing visualization if it exists
        if (cloneSelectionPlane != null) {
            // Remove from glow layer first
            cloneGlowLayer.removeIncludedOnlyMesh(cloneSelectionPlane!!)
            scene.removeMesh(cloneSelectionPlane!!)
            cloneSelectionPlane = null
        }

        // Remove any additional planes
        cloneSelectionAdditionalPlanes.forEach { plane ->
            // Remove from glow layer first
            cloneGlowLayer.removeIncludedOnlyMesh(plane)
            scene.removeMesh(plane)
        }
        cloneSelectionAdditionalPlanes.clear()

        // Create a new plane for the base (bottom)
        cloneSelectionPlane = MeshBuilder.CreatePlane("cloneSelectionPlane", object : CreatePlaneOptions {
            override val width = size.x
            override val height = size.z
            override val updatable = true
        }, scene)

        // Create a semi-transparent material
        val material = StandardMaterial("cloneSelectionMaterial", scene)
        material.emissiveColor = Color3(0f, 1f, 1f) // Cyan color
        material.specularColor = Color3.Black()
        material.alpha = 0.3f
        material.backFaceCulling = false
        material.wireframe = true
        cloneSelectionPlane!!.material = material

        // Rotate to be horizontal (bottom plane)
        cloneSelectionPlane!!.rotation = Vector3(Math.PI / 2, 0f, 0f)

        // Position the bottom plane
        cloneSelectionPlane!!.position = position.add(Vector3(size.x / 2, 0f, size.z / 2))

        // Add to glow layer
        cloneGlowLayer.addIncludedOnlyMesh(cloneSelectionPlane!!)

        // Only create additional planes for the box if we're in a state that needs it
        if (cloneSelectionState == CloneSelectionState.SecondPointSelected || 
            cloneSelectionState == CloneSelectionState.Complete) {

            // Create top plane
            val topPlane = MeshBuilder.CreatePlane("cloneSelectionTop", object : CreatePlaneOptions {
                override val width = size.x
                override val height = size.z
                override val updatable = true
            }, scene)
            topPlane.material = material
            topPlane.rotation = Vector3(Math.PI / 2, 0f, 0f)
            topPlane.position = position.add(Vector3(size.x / 2, boxHeight, size.z / 2))
            cloneSelectionAdditionalPlanes.add(topPlane)
            cloneGlowLayer.addIncludedOnlyMesh(topPlane)

            // Create front plane
            val frontPlane = MeshBuilder.CreatePlane("cloneSelectionFront", object : CreatePlaneOptions {
                override val width = size.x
                override val height = boxHeight
                override val updatable = true
            }, scene)
            frontPlane.material = material
            frontPlane.position = position.add(Vector3(size.x / 2, boxHeight / 2, 0f))
            cloneSelectionAdditionalPlanes.add(frontPlane)
            cloneGlowLayer.addIncludedOnlyMesh(frontPlane)

            // Create back plane
            val backPlane = MeshBuilder.CreatePlane("cloneSelectionBack", object : CreatePlaneOptions {
                override val width = size.x
                override val height = boxHeight
                override val updatable = true
            }, scene)
            backPlane.material = material
            backPlane.rotation = Vector3(0f, Math.PI, 0f)
            backPlane.position = position.add(Vector3(size.x / 2, boxHeight / 2, size.z))
            cloneSelectionAdditionalPlanes.add(backPlane)
            cloneGlowLayer.addIncludedOnlyMesh(backPlane)

            // Create left plane
            val leftPlane = MeshBuilder.CreatePlane("cloneSelectionLeft", object : CreatePlaneOptions {
                override val width = size.z
                override val height = boxHeight
                override val updatable = true
            }, scene)
            leftPlane.material = material
            leftPlane.rotation = Vector3(0f, Math.PI / 2, 0f)
            leftPlane.position = position.add(Vector3(0f, boxHeight / 2, size.z / 2))
            cloneSelectionAdditionalPlanes.add(leftPlane)
            cloneGlowLayer.addIncludedOnlyMesh(leftPlane)

            // Create right plane
            val rightPlane = MeshBuilder.CreatePlane("cloneSelectionRight", object : CreatePlaneOptions {
                override val width = size.z
                override val height = boxHeight
                override val updatable = true
            }, scene)
            rightPlane.material = material
            rightPlane.rotation = Vector3(0f, -Math.PI / 2, 0f)
            rightPlane.position = position.add(Vector3(size.x, boxHeight / 2, size.z / 2))
            cloneSelectionAdditionalPlanes.add(rightPlane)
            cloneGlowLayer.addIncludedOnlyMesh(rightPlane)
        }
    }

    private fun updateDraw(pickedPoint: Vector3) {
        val cPos = scene.activeCamera!!.globalPosition
        var pos = updateSide(drawPlane, pickedPoint)
        val curPos = pos.clone()

        if (side != drawPlane) {
            if (drawPlane == Side.Y) {
                if (cPos.y < curPos.y) {
                    curPos.y -= 1f
                }
            } else if (drawPlane == Side.X) {
                if (cPos.x < curPos.x) {
                    curPos.x -= 1f
                }
            } else if (drawPlane == Side.Z) {
                if (cPos.z < curPos.z) {
                    curPos.z -= 1f
                }
            }
        }

        if (curPosAnimated == null) {
            curPosAnimated = curPos
        } else if (!curPosAnimated!!.equals(curPos)) {
            curPosAnimated = Vector3.Lerp(curPosAnimated!!, curPos, min(1f, scene.deltaTime / 30f))
        }

        tilePos.copyFrom(curPos)

        val offset = let {
            val r = Math.floor(brushSize / 2f)
            when (drawPlane.abs) {
                Side.X -> Vector3(0f, r, r)
                Side.Y -> Vector3(r, 0f, r)
                Side.Z -> Vector3(r, r, 0f)
                else -> Vector3.Zero()
            }
        }

        cursor.position.copyFrom(curPosAnimated!!.subtract(offset))

        grid.position.copyFrom(
            pos.add(
                when (drawPlane) {
                    Side.Y -> Vector3(0.5f, 0f, 0.5f)
                    Side.Z -> Vector3(0.5f, 0.5f, 0f)
                    else -> Vector3(0f, 0.5f, 0.5f)
                }
            )
        )

        if (autoRotate) {
            when (side) {
                Side.Z -> {
                    cursor.rotation = Vector3(0f, 0f, 0f)
                }

                Side.X -> {
                    cursor.rotation = Vector3(0f, -Math.PI / 2, 0f)
                }

                else -> {
                    cursor.rotation = Vector3(Math.PI / 2, 0f, 0f)
                }
            }
        }
    }

    private fun updateSide(side: Side, pickedPoint: Vector3): Vector3 {
        val up: Vector3
        val right: Vector3
        val forward: Vector3
        val sideForward: Side
        val sideRight: Side
        val pickRight: (Vector3) -> Float
        val pickForward: (Vector3) -> Float

        when (side.abs) {
            Side.Y -> {
                up = Vector3.Up().scale(0.5f)
                forward = Vector3(0f, 0.5f, 0.5f)
                right = Vector3(0.5f, 0.5f, 0f)
                sideForward = Side.Z
                sideRight = Side.X
                pickForward = { v -> v.z }
                pickRight = { v -> v.x }
            }

            Side.X -> {
                up = Vector3.Right().scale(0.5f)
                forward = Vector3(0.5f, 0.5f, 0f)
                right = Vector3(0.5f, 0f, 0.5f)
                sideForward = Side.Y
                sideRight = Side.Z
                pickForward = { v -> v.y }
                pickRight = { v -> v.z }
            }

            Side.Z -> {
                up = Vector3.Forward().scale(0.5f)
                forward = Vector3(0f, 0.5f, 0.5f)
                right = Vector3(0.5f, 0f, 0.5f)
                sideForward = Side.Y
                sideRight = Side.X
                pickForward = { v -> v.y }
                pickRight = { v -> v.x }
            }

            Side.NEGATIVE_Y -> {
                up = Vector3.Up().scale(-0.5f)
                forward = Vector3(0f, -0.5f, 0.5f)
                right = Vector3(0.5f, -0.5f, 0f)
                sideForward = Side.NEGATIVE_Z
                sideRight = Side.NEGATIVE_X
                pickForward = { v -> v.z }
                pickRight = { v -> v.x }
            }

            Side.NEGATIVE_X -> {
                up = Vector3.Right().scale(-0.5f)
                forward = Vector3(-0.5f, 0.5f, 0f)
                right = Vector3(-0.5f, 0f, 0.5f)
                sideForward = Side.NEGATIVE_Y
                sideRight = Side.NEGATIVE_Z
                pickForward = { v -> v.y }
                pickRight = { v -> v.z }
            }

            Side.NEGATIVE_Z -> {
                up = Vector3.Forward().scale(-0.5f)
                forward = Vector3(0f, 0.5f, -0.5f)
                right = Vector3(0.5f, 0f, -0.5f)
                sideForward = Side.NEGATIVE_Y
                sideRight = Side.NEGATIVE_X
                pickForward = { v -> v.y }
                pickRight = { v -> v.x }
            }
        }

        if (this.side == this.drawPlane) {
            return pickedPoint.add(up).floor()
        } else {
            val forwardPos = pickedPoint.add(forward).floor()
            val rightPos = pickedPoint.add(right).floor()

            if (autoRotate && (abs(pickForward(pickedPoint) - pickForward(forwardPos)) > 0.25f || abs(
                    pickRight(pickedPoint) - pickRight(rightPos)
                ) > 0.25f)
            ) {
                if (abs(pickForward(pickedPoint) - pickForward(forwardPos)) < abs(
                        pickRight(pickedPoint) - pickRight(
                            rightPos
                        )
                    )
                ) {
                    this.side = sideForward
                    return forwardPos
                } else {
                    this.side = sideRight
                    return rightPos
                }
            } else {
                return pickedPoint.add(if (this.side == sideForward) forward else right).floor()
            }
        }
    }

    fun draw(eraser: Boolean) {
        // Handle Clone mode separately
        if (drawMode == DrawMode.Clone) {
            if (eraser) {
                // Alt+click in clone mode - erase tiles in the cloned area size at cursor
                if (cloneSelectionState == CloneSelectionState.Complete) {
                    eraseCloneAreaAtCursor()
                }
            } else {
                handleCloneClick()
            }
            return
        }

        // Only draw if we have a selection or if we're erasing
        val hasSelection = if (drawMode == DrawMode.Tile) currentGameTile != null else currentGameObject != null
        if (eraser || hasSelection) {
            drawBrush(tilePos, side, eraser)
        }
    }

    /**
     * Erases all tiles in an area the size of the clone selection at the current cursor position
     * Also erases perimeter tiles similar to how storeClonedTiles() works
     */
    private fun eraseCloneAreaAtCursor() {
        // Only proceed if we have a complete clone selection
        if (cloneSelectionState != CloneSelectionState.Complete) return

        // Get the dimensions of the clone selection
        val width = cloneSelectionWidth
        val depth = cloneSelectionDepth
        val height = cloneSelectionHeight

        // Get the current cursor position as the starting point
        val startPos = tilePos.clone()

        // Calculate the min and max points of the selection box
        val minX = startPos.x.toInt()
        val maxX = minX + width - 1
        val minY = startPos.y.toInt()
        val maxY = minY + height - 1
        val minZ = startPos.z.toInt()
        val maxZ = minZ + depth - 1

        // Expand the area to include the perimeter
        val expandedMinX = minX - 1
        val expandedMaxX = maxX + 1
        val expandedMinZ = minZ - 1
        val expandedMaxZ = maxZ + 1

        // Iterate through all positions in the expanded selection box
        for (x in expandedMinX..expandedMaxX) {
            for (y in minY..maxY) {
                for (z in expandedMinZ..expandedMaxZ) {
                    // Check if this position is on the perimeter or inside the original selection
                    val isPerimeterX = x == expandedMinX || x == expandedMaxX
                    val isPerimeterZ = z == expandedMinZ || z == expandedMaxZ
                    val isInside = x in minX..maxX && z in minZ..maxZ

                    // Only process if it's inside the original selection or on the X/Z perimeter
                    if (isInside || isPerimeterX || isPerimeterZ) {
                        // For perimeter tiles, only include X, -X, Z, -Z sides (not Y or -Y)
                        // For inside tiles, include all sides
                        val isPerimeter = isPerimeterX || isPerimeterZ
                        val sidesToCheck = if (isPerimeter && !isInside) {
                            // For perimeter-only tiles, exclude Y and -Y sides
                            listOf(Side.X, Side.Z, Side.NEGATIVE_X, Side.NEGATIVE_Z)
                        } else {
                            // For inside tiles, include all sides
                            Side.entries.toList()
                        }

                        // Create a position vector
                        val position = Vector3(x.toFloat(), y.toFloat(), z.toFloat())

                        // Remove tiles on the appropriate sides at this position
                        sidesToCheck.forEach { side ->
                            tilemap.removeTile(position, side)
                        }
                    }
                }
            }
        }
    }

    private fun drawBrush(position: Vector3, side: Side, eraser: Boolean = false) {
        val camera = scene.activeCamera!!
        var localSide = side.abs
        if (localSide == Side.Y) {
            if (camera.globalPosition.y < position.y) {
                localSide = Side.NEGATIVE_Y
            }
        } else if (localSide == Side.X) {
            if (camera.globalPosition.x < position.x) {
                localSide = Side.NEGATIVE_X
            }
        } else if (localSide == Side.Z) {
            if (camera.globalPosition.z < position.z) {
                localSide = Side.NEGATIVE_Z
            }
        }

        val o = Vector3.Zero()
        val r = Math.floor(brushSize / 2f)
        for (x in -r.toInt() until brushSize - r.toInt()) {
            for (y in -r.toInt() until brushSize - r.toInt()) {
                when (drawPlane.abs) {
                    Side.X -> {
                        o.z = x.toFloat()
                        o.y = y.toFloat()
                    }

                    Side.Y -> {
                        o.x = x.toFloat()
                        o.z = y.toFloat()
                    }

                    Side.Z -> {
                        o.x = x.toFloat()
                        o.y = y.toFloat()
                    }

                    else -> throw IllegalArgumentException("Unexpected side: $side")
                }
                if (brushDensity == 100 || Math.random() < brushDensity / 100f) {
                    if (drawMode == DrawMode.Object) {
                        if (eraser) {
                            tilemap.removeObject(position.add(o), localSide)
                        } else if (currentGameObject != null) {
                            tilemap.addObject(position.add(o), localSide, currentGameObject!!)
                        }
                    } else if (drawMode == DrawMode.Tile) {
                        if (eraser) {
                            tilemap.removeTile(position.add(o), localSide)
                        } else if (currentGameTile != null) {
                            tilemap.setTile(position.add(o), localSide, currentGameTile)
                        }
                    }
                }
            }
        }
    }

    fun togglePlane(isReverse: Boolean) {
        val ray = scene.createPickingRay(scene.pointerX, scene.pointerY, Matrix.Identity(), scene.activeCamera!!)
        val pickedPoint = ray.intersectsMesh(tilemap.mesh).pickedPoint
        var pos = tilePos

        if (pickedPoint != null) {
            // note these are rotated 1 step forward
            if (drawPlane == Side.X) {
                pos = pickedPoint.add(Vector3(0f, 0.5f, 0f)).floor()
            } else if (drawPlane == Side.Y) {
                pos = pickedPoint.add(Vector3(0f, 0f, 0.5f)).floor()
            } else if (drawPlane == Side.Z) {
                pos = pickedPoint.add(Vector3(0.5f, 0f, 0f)).floor()
            }
        }

        when (drawPlane.abs) {
            Side.X -> {
                if (isReverse) {
                    drawPlane = Side.Z
                    drawPlaneOffset = pos.z.toInt()
                    side = Side.Z
                } else {
                    drawPlane = Side.Y
                    drawPlaneOffset = pos.y.toInt()
                    side = Side.Y
                }
            }

            Side.Y -> {
                if (isReverse) {
                    drawPlane = Side.X
                    drawPlaneOffset = pos.x.toInt()
                    side = Side.X
                } else {
                    drawPlane = Side.Z
                    drawPlaneOffset = pos.z.toInt()
                    side = Side.Z
                }
            }

            Side.Z -> {
                if (isReverse) {
                    drawPlane = Side.Y
                    drawPlaneOffset = pos.y.toInt()
                    side = Side.Y
                } else {
                    drawPlane = Side.X
                    drawPlaneOffset = pos.x.toInt()
                    side = Side.X
                }
            }

            Side.NEGATIVE_X -> {
                if (isReverse) {
                    drawPlane = Side.NEGATIVE_Z
                    drawPlaneOffset = pos.z.toInt()
                    side = Side.NEGATIVE_Z
                } else {
                    drawPlane = Side.NEGATIVE_Y
                    drawPlaneOffset = pos.y.toInt()
                    side = Side.NEGATIVE_Y
                }
            }

            Side.NEGATIVE_Y -> {
                if (isReverse) {
                    drawPlane = Side.NEGATIVE_X
                    drawPlaneOffset = pos.x.toInt()
                    side = Side.NEGATIVE_X
                } else {
                    drawPlane = Side.NEGATIVE_Z
                    drawPlaneOffset = pos.z.toInt()
                    side = Side.NEGATIVE_Z
                }
            }

            Side.NEGATIVE_Z -> {
                if (isReverse) {
                    drawPlane = Side.NEGATIVE_Y
                    drawPlaneOffset = pos.y.toInt()
                    side = Side.NEGATIVE_Y
                } else {
                    drawPlane = Side.NEGATIVE_X
                    drawPlaneOffset = pos.x.toInt()
                    side = Side.NEGATIVE_X
                }
            }
        }

        when (drawPlane) {
            Side.X, Side.NEGATIVE_X -> {
                grid.rotation = Vector3(0f, 0f, Math.PI / 2)
            }

            Side.Y, Side.NEGATIVE_Y -> {
                grid.rotation = Vector3(0f, -Math.PI / 2, 0f)
            }

            Side.Z, Side.NEGATIVE_Z -> {
                grid.rotation = Vector3(Math.PI / 2, 0f, 0f)
            }
        }

        refreshBrush()
    }

    fun pickAdjust() {
        val ray = scene.createPickingRay(scene.pointerX, scene.pointerY, Matrix.Identity(), scene.activeCamera!!)
        val pickedPoint = ray.intersectsMesh(tilemap.mesh).pickedPoint

        if (pickedPoint != null) {
            var pos = pickedPoint

            when (drawPlane) {
                Side.X, Side.NEGATIVE_X -> {
                    pos = pickedPoint.add(
                        when (side) {
                            Side.Y, Side.NEGATIVE_Y -> Vector3(0.5f, 0.5f, 0f)
                            Side.Z, Side.NEGATIVE_Z -> Vector3(0.5f, 0f, 0.5f)
                            Side.X, Side.NEGATIVE_X -> Vector3(0.5f, 0f, 0f)
                        }
                    ).floor()
                }

                Side.Y, Side.NEGATIVE_Y -> {
                    pos = pickedPoint.add(
                        when (side) {
                            Side.Y, Side.NEGATIVE_Y -> Vector3(0f, 0.5f, 0f)
                            Side.Z, Side.NEGATIVE_Z -> Vector3(0f, 0.5f, 0.5f)
                            Side.X, Side.NEGATIVE_X -> Vector3(0.5f, 0.5f, 0f)
                        }
                    ).floor()
                }

                Side.Z, Side.NEGATIVE_Z -> {
                    pos = pickedPoint.add(
                        when (side) {
                            Side.Y, Side.NEGATIVE_Y -> Vector3(0f, 0.5f, 0.5f)
                            Side.Z, Side.NEGATIVE_Z -> Vector3(0f, 0f, 0.5f)
                            Side.X, Side.NEGATIVE_X -> Vector3(0.5f, 0f, 0.5f)
                        }
                    ).floor()
                }
            }

            drawPlaneOffset = when (drawPlane) {
                Side.X, Side.NEGATIVE_X -> {
                    pos.x.toInt()
                }

                Side.Y, Side.NEGATIVE_Y -> {
                    pos.y.toInt()
                }

                Side.Z, Side.NEGATIVE_Z -> {
                    pos.z.toInt()
                }
            }
        }
    }

    fun adjustPlane(distance: Int) {
        drawPlaneOffset += distance
    }

    fun toggleAutoRotate() {
        autoRotate = !autoRotate
    }

    fun toggleSide(isReversed: Boolean) {
        if (autoRotate && side != drawPlane) {
            side = when (drawPlane.abs) {
                Side.Y -> if (side == Side.Y) Side.Z else Side.Y
                Side.X -> if (side == Side.X) Side.Y else Side.X
                Side.Z -> if (side == Side.Z) Side.X else Side.Z
                Side.NEGATIVE_Y -> if (side == Side.NEGATIVE_Y) Side.NEGATIVE_Z else Side.NEGATIVE_Y
                Side.NEGATIVE_X -> if (side == Side.NEGATIVE_X) Side.NEGATIVE_Y else Side.NEGATIVE_X
                Side.NEGATIVE_Z -> if (side == Side.NEGATIVE_Z) Side.NEGATIVE_X else Side.NEGATIVE_Z
            }
        } else if (isReversed) {
            side = when (side) {
                Side.Y -> Side.X
                Side.Z -> Side.Y
                Side.X -> Side.Z
                Side.NEGATIVE_Y -> Side.NEGATIVE_X
                Side.NEGATIVE_Z -> Side.NEGATIVE_Y
                Side.NEGATIVE_X -> Side.NEGATIVE_Z
            }
        } else {
            side = when (side) {
                Side.Y -> Side.Z
                Side.Z -> Side.X
                Side.X -> Side.Y
                Side.NEGATIVE_Y -> Side.NEGATIVE_Z
                Side.NEGATIVE_Z -> Side.NEGATIVE_X
                Side.NEGATIVE_X -> Side.NEGATIVE_Y
            }
        }
        refreshBrush()
    }

    private fun refreshBrush() {
        when (side) {
            Side.Z, Side.NEGATIVE_Z -> {
                cursor.rotation = Vector3(0f, 0f, 0f)
            }

            Side.X, Side.NEGATIVE_X -> {
                cursor.rotation = Vector3(0f, -Math.PI / 2, 0f)
            }

            Side.Y, Side.NEGATIVE_Y -> {
                cursor.rotation = Vector3(Math.PI / 2, 0f, 0f)
            }
        }
        updateCursorSize()
    }

    private fun updateCursorSize() {
        cursor.scaling.x = brushSize.toFloat()
        cursor.scaling.y = brushSize.toFloat()
    }

    /**
     * Handles a click in clone mode based on the current selection state
     */
    fun handleCloneClick() {
        when (cloneSelectionState) {
            CloneSelectionState.NotStarted -> {
                // First click - store the first point
                cloneFirstPoint = tilePos.clone()
                cloneSelectionState = CloneSelectionState.FirstPointSelected
            }

            CloneSelectionState.FirstPointSelected -> {
                // Second click - store the second point (on XZ plane)
                cloneSecondPoint = Vector3(tilePos.x, cloneFirstPoint.y, tilePos.z)
                cloneSelectionState = CloneSelectionState.SecondPointSelected
            }

            CloneSelectionState.SecondPointSelected -> {
                // Third click - set the height
                cloneHeight = max(1, abs(tilePos.y - cloneFirstPoint.y).toInt() + 1)
                cloneSelectionState = CloneSelectionState.Complete

                // Store the tiles in the selection box
                storeClonedTiles()
            }

            CloneSelectionState.Complete -> {
                // Clone the tiles to the current position
                cloneTiles()
            }
        }
    }

    /**
     * Stores all tiles within the clone selection box and on its perimeter
     */
    private fun storeClonedTiles() {
        clonedTiles.clear()

        // Calculate the min and max points of the selection box
        val minX = min(cloneFirstPoint.x, cloneSecondPoint.x).toInt()
        val maxX = max(cloneFirstPoint.x, cloneSecondPoint.x).toInt()
        val minY = cloneFirstPoint.y.toInt()
        val maxY = (cloneFirstPoint.y + cloneHeight - 1).toInt()
        val minZ = min(cloneFirstPoint.z, cloneSecondPoint.z).toInt()
        val maxZ = max(cloneFirstPoint.z, cloneSecondPoint.z).toInt()

        // Get all tiles in the selection box
        val tileTypes = tilemap.getTileTypes()

        // Expand the search area to include the perimeter
        val expandedMinX = minX - 1
        val expandedMaxX = maxX + 1
        val expandedMinZ = minZ - 1
        val expandedMaxZ = maxZ + 1

        // Iterate through all positions in the expanded selection box
        for (x in expandedMinX..expandedMaxX) {
            for (y in minY..maxY) {
                for (z in expandedMinZ..expandedMaxZ) {
                    // Check if this position is on the perimeter or inside the original selection
                    val isPerimeterX = x == expandedMinX || x == expandedMaxX
                    val isPerimeterZ = z == expandedMinZ || z == expandedMaxZ
                    val isInside = x in minX..maxX && z >= minZ && z <= maxZ

                    // Only process if it's inside the original selection or on the X/Z perimeter
                    if (isInside || isPerimeterX || isPerimeterZ) {
                        // For perimeter tiles, only include X, -X, Z, -Z sides (not Y or -Y)
                        // For inside tiles, include all sides
                        val isPerimeter = isPerimeterX || isPerimeterZ
                        val sidesToCheck = if (isPerimeter && !isInside) {
                            // For perimeter-only tiles, exclude Y and -Y sides
                            listOf(Side.X, Side.Z, Side.NEGATIVE_X, Side.NEGATIVE_Z)
                        } else {
                            // For inside tiles, include all sides
                            listOf(Side.X, Side.Y, Side.Z, Side.NEGATIVE_X, Side.NEGATIVE_Y, Side.NEGATIVE_Z)
                        }

                        for (side in sidesToCheck) {
                            val key = "$x,$y,$z,$side"
                            if (tileTypes.containsKey(key)) {
                                // Store the tile type with its relative position in the expanded selection box
                                val relX = x - expandedMinX
                                val relY = y - minY
                                val relZ = z - expandedMinZ
                                val relKey = "$relX,$relY,$relZ,$side"
                                clonedTiles[relKey] = tileTypes[key]!!
                            }
                        }
                    }
                }
            }
        }
    }

    /**
     * Clones the stored tiles to the current cursor position
     */
    private fun cloneTiles() {
        if (clonedTiles.isEmpty()) return

        // Get the target position (top-left corner of the clone)
        val targetPos = tilePos.clone()

        // Clone all stored tiles to the target position
        val newTiles = buildList {
            for ((relPosKey, tileId) in clonedTiles) {
                val parts = relPosKey.split(",")
                if (parts.size == 4) {
                    val relX = parts[0].toIntOrNull() ?: 0
                    val relY = parts[1].toIntOrNull() ?: 0
                    val relZ = parts[2].toIntOrNull() ?: 0
                    val side = parts[3]

                    // Calculate the absolute position
                    // Subtract 1 from X and Z to correct the offset
                    val absX = targetPos.x.toInt() + relX - 1
                    val absY = targetPos.y.toInt() + relY
                    val absZ = targetPos.z.toInt() + relZ - 1

                    // Create a GameTile with the stored ID
                    val gameTile = GameTile().apply {
                        this.id = tileId
                    }

                    this@buildList.add(
                        Triple(
                            Vector3(absX.toFloat(), absY.toFloat(), absZ.toFloat()),
                            Side.fromString(side),
                            gameTile
                        )
                    )
                }
            }
        }

        if (newTiles.isNotEmpty()) {
            // Set the tile at the calculated position
            tilemap.setTiles(newTiles)
        }
    }

    /**
     * Resets the clone selection process
     */
    fun resetCloneSelection() {
        cloneSelectionState = CloneSelectionState.NotStarted

        // Remove the main selection plane
        if (cloneSelectionPlane != null) {
            // Remove from glow layer first
            cloneGlowLayer.removeIncludedOnlyMesh(cloneSelectionPlane!!)
            scene.removeMesh(cloneSelectionPlane!!)
            cloneSelectionPlane = null
        }

        // Remove all additional planes
        cloneSelectionAdditionalPlanes.forEach { plane ->
            // Remove from glow layer first
            cloneGlowLayer.removeIncludedOnlyMesh(plane)
            scene.removeMesh(plane)
        }
        cloneSelectionAdditionalPlanes.clear()

        // Remove the insertion preview
        removeCloneInsertionPreview()

        // Clear stored tiles
        clonedTiles.clear()
    }

    /**
     * Returns the current clone selection state
     */
    fun getCloneSelectionState(): CloneSelectionState {
        return cloneSelectionState
    }
}
