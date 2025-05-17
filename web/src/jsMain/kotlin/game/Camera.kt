package game

import lib.*
import kotlin.math.PI

enum class CameraView {
    Free,
    Player,
    Eye
}

class Camera(private val scene: Scene, private val pick: () -> AbstractMesh) {
    val camera: ArcRotateCamera
    var isMoving = false
    var view = CameraView.Free

    init {
        val camera = ArcRotateCamera("camera", 0f, PI.toFloat() / 4f, 30f, Vector3.Zero(), scene)
        camera.zoomToMouseLocation = true
        camera.wheelDeltaPercentage = 0.01f
        camera.wheelPrecision *= 0.1f
        (camera.inputs.attached["keyboard"] as ArcRotateCameraKeyboardMoveInput).angularSpeed *= 0.25f
        camera.attachControl()
        camera.fov = 0.5f  // Default FOV, may be overridden by scene config
        camera.maxZ = 100f
        camera.minZ = 0.1f
        camera.lowerRadiusLimit = 1f
        scene.activeCamera = camera
        this.camera = camera
    }

    fun update() {
        if (view == CameraView.Free) {
            val settled = !isMoving && camera.inertialRadiusOffset == 0f && camera.inertialAlphaOffset == 0f && 
                camera.inertialBetaOffset == 0f && camera.targetScreenOffset.length() != 0f

            if (settled) {
                camera.target.copyFrom(
                    Vector3.TransformCoordinates(
                        Vector3(0f, 0f, camera.radius),
                        camera.getViewMatrix().invert()
                    )
                )
                camera.targetScreenOffset.scaleInPlace(0f)
            }
        }
    }

    fun toggleView(isReverse: Boolean) {
        camera.targetScreenOffset.scaleInPlace(0f)
        view = if (isReverse) {
            when (view) {
                CameraView.Free -> CameraView.Eye
                CameraView.Eye -> CameraView.Player
                CameraView.Player -> CameraView.Free
            }
        } else {
            when (view) {
                CameraView.Free -> CameraView.Player
                CameraView.Player -> CameraView.Eye
                CameraView.Eye -> CameraView.Free
            }
        }
        if (view == CameraView.Free && camera.radius < 10f) {
            camera.radius = 10f
        }
        if (view == CameraView.Eye) {
            camera.beta = PI.toFloat() / 2f
        }
    }

    fun walk(walk: Vector3) {
        val walkForward = camera.getDirection(Vector3.Forward()).multiply(Vector3(1f, 0f, 1f)).normalize().scale(walk.y)
        val walkRight = camera.getDirection(Vector3.Right()).multiply(Vector3(1f, 0f, 1f)).normalize().scale(walk.x)
        camera.target.addInPlace(walkForward.add(walkRight).normalize().scale(2f).scale(scene.deltaTime / 1000f))
    }

    fun getPick(): AbstractMesh {
        return pick()
    }

    fun recenter() {
        if (view != CameraView.Free) {
            return
        }
        val ray = scene.createPickingRay(scene.pointerX, scene.pointerY, Matrix.Identity(), camera)
        val pickedPoint = ray.intersectsMesh(pick()).pickedPoint
        if (pickedPoint != null) {
            val relPos = Vector3.TransformCoordinates(pickedPoint, camera.getViewMatrix())
            val alpha = camera.alpha
            val beta = camera.beta
            camera.target.copyFrom(pickedPoint)
            camera.targetScreenOffset.scaleInPlace(0f)
            camera.targetScreenOffset.x = relPos.x
            camera.targetScreenOffset.y = relPos.y
            camera.radius = relPos.z
            camera.alpha = alpha
            camera.beta = beta
        }
    }
}
