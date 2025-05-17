package game

import com.queatz.db.GameObject
import com.queatz.db.GameTile
import lib.Color3
import lib.CreateGroundOptions
import lib.CreatePlaneOptions
import lib.GridMaterial
import lib.Math
import lib.Matrix
import lib.Mesh
import lib.MeshBuilder
import lib.Plane
import lib.Scene
import lib.StandardMaterial
import lib.Texture
import lib.Vector3
import lib.VertexBuffer

enum class DrawMode {
    Tile,
    Object
}

class TilemapEditor(private val scene: Scene, val tilemap: Tilemap) {
    val cursor: Mesh
    var grid: Mesh

    // Flag to track if initialization is complete
    private var initialized = false

    // Flag to control whether editing is allowed
    var editable: Boolean = true

    var pickedPoint: Vector3 = Vector3.Zero()
    var tilePos = Vector3.Zero()
    var curPosAnimated: Vector3? = null
    var side: Side = Side.Y
    var drawPlane: Side = Side.Y
    var drawPlaneOffset = 0
    var drawMode: DrawMode = DrawMode.Tile
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

    // Current GameTile to paint with
    var currentGameTile: GameTile? = null

    // Current GameObject to place
    var currentGameObject: GameObject? = null

    init {
        // Create a square cursor that represents the brush area
        val cursor = MeshBuilder.CreatePlane("tile", object : CreatePlaneOptions {
            override val width = 1f
            override val height = 1f
            override val updatable = true
        }, scene)
        // Create a semi-transparent material for the cursor
        val material = StandardMaterial("cursorMaterial", scene)
        material.emissiveColor = Color3.White().scale(2.0f)  // Increase emissive intensity for better visibility
        material.specularColor = Color3.Black()
        material.alpha = 0.5f  // More visible but still semi-transparent
        material.backFaceCulling = false  // Show both sides
        material.zOffset = -0.01f  // Ensure it's drawn on top
        material.zOffsetUnits = -1
        cursor.rotation = Vector3(Math.PI / 2, 0f, 0f)
        cursor.material = material
        this.cursor = cursor

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
        grid.opacity = 0.25f
        grid.alpha = 0.25f
        grid.gridRatio = 1f
        grid.majorUnitFrequency = 1
        grid.zOffsetUnits = -0.01f
        grid.fogEnabled = true
        grid.gridOffset = Vector3(0.5f, 0f, 0.5f)
        grid.antialias = false

        brushGrid.material = grid
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
        val hasSelection = if (drawMode == DrawMode.Tile) currentGameTile != null else currentGameObject != null

        // Only show cursor and grid if editing is allowed and something is selected
        // This ensures the cursor mesh is hidden when no object and no tile is selected
        cursor.isVisible = editable && hasSelection
        grid.isVisible = editable && hasSelection

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
            curPosAnimated = Vector3.Lerp(curPosAnimated!!, curPos, Math.min(1f, scene.deltaTime / 30f))
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
            else -> throw IllegalArgumentException("Invalid side: $side")
        }

        if (this.side == this.drawPlane) {
            return pickedPoint.add(up).floor()
        } else {
            val forwardPos = pickedPoint.add(forward).floor()
            val rightPos = pickedPoint.add(right).floor()

            if (autoRotate && (Math.abs(pickForward(pickedPoint) - pickForward(forwardPos)) > 0.25f || Math.abs(pickRight(pickedPoint) - pickRight(rightPos)) > 0.25f)) {
                if (Math.abs(pickForward(pickedPoint) - pickForward(forwardPos)) < Math.abs(pickRight(pickedPoint) - pickRight(rightPos))) {
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
        // Only draw if we have a selection or if we're erasing
        val hasSelection = if (drawMode == DrawMode.Tile) currentGameTile != null else currentGameObject != null
        if (eraser || hasSelection) {
            drawBrush(tilePos, side, eraser)
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

            else -> throw IllegalArgumentException("Unexpected side: $side")
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
                            Side.Y -> Vector3(0.5f, 0.5f, 0f)
                            Side.Z -> Vector3(0.5f, 0f, 0.5f)
                            else -> Vector3(0.5f, 0f, 0f)
                        }
                    ).floor()
                }
                Side.Y, Side.NEGATIVE_Y -> {
                    pos = pickedPoint.add(
                        when (side) {
                            Side.Y -> Vector3(0f, 0.5f, 0f)
                            Side.Z -> Vector3(0f, 0.5f, 0.5f)
                            else -> Vector3(0.5f, 0.5f, 0f)
                        }
                    ).floor()
                }
                Side.Z, Side.NEGATIVE_Z -> {
                    pos = pickedPoint.add(
                        when (side) {
                            Side.Y -> Vector3(0f, 0.5f, 0.5f)
                            Side.Z -> Vector3(0f, 0f, 0.5f)
                            else -> Vector3(0.5f, 0f, 0.5f)
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

    fun toggleDrawMode() {
        drawMode = if (drawMode == DrawMode.Tile) DrawMode.Object else DrawMode.Tile
    }

    fun toggleAutoRotate() {
        autoRotate = !autoRotate
    }

    fun toggleSide(isReversed: Boolean) {
        if (autoRotate && side != drawPlane) {
            side = when (drawPlane) {
                Side.Y, Side.NEGATIVE_Y -> if (side == Side.Y) Side.Z else Side.Y
                Side.X, Side.NEGATIVE_X -> if (side == Side.X) Side.Y else Side.X
                Side.Z, Side.NEGATIVE_Z -> if (side == Side.Z) Side.X else Side.Z
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
}
