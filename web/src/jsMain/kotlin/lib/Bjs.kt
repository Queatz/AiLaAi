@file:JsModule("@babylonjs/core")
@file:JsNonModule

package lib

import org.w3c.dom.HTMLCanvasElement
import org.w3c.dom.CanvasRenderingContext2D

abstract external class AbstractMesh {
    var isVisible: Boolean
    var position: Vector3
    var material: Material
    var receiveShadows: Boolean
    var rotation: Vector3
    var rotationQuaternion: Quaternion?
    var origin: Vector3
    var scaling: Vector3
    var billboardMode: Int
    val onAfterWorldMatrixUpdateObservable: Observable<AbstractMesh>
    var instancedBuffers: dynamic
    var actionManager: ActionManager?

    fun getVerticesData(kind: String): Array<Float>?
    fun setVerticesData(kind: String, data: Array<Float>, updatable: Boolean)
    fun clone(name: String): AbstractMesh
    fun registerInstancedBuffer(name: String, size: Int)
    fun addRotation(x: Float, y: Float, z: Float)

}

external class CascadedShadowGenerator(
    mapSize: Int,
    light: DirectionalLight,
    useFullFloatEngine: Boolean = definedExternally
) {
    var lambda: Float
    var bias: Float
    var normalBias: Float
    var transparencyShadow: Boolean
    var stabilizeCascades: Boolean
    var shadowMaxZ: Float

    fun splitFrustum()
    fun addShadowCaster(mesh: AbstractMesh)
    fun removeShadowCaster(mesh: AbstractMesh)
}

external class Color3(r: Float = definedExternally, g: Float = definedExternally, b: Float = definedExternally) {
    fun toColor4(): Color4
    fun toGammaSpace(): Color3
    fun toLinearSpace(): Color3
    fun toLinearSpaceToRef(ref: Color3): Color3
    fun toGammaSpaceToRef(ref: Color3): Color3
    fun scale(scale: Float): Color3

    companion object {
        fun FromHexString(hex: String): Color3
        fun White(): Color3
        fun Black(): Color3
    }
}

external class Color4(
    var r: Float = definedExternally,
    var g: Float = definedExternally,
    var b: Float = definedExternally,
    var a: Float = definedExternally
) {
    fun toLinearSpace(): Color4
    fun scale(scale: Float): Color4
    fun subtract(color: Color4): Color4
    fun asArray(): Array<Float>
}

external class DirectionalLight(name: String, direction: Vector3, scene: Scene) {
    var intensity: Float
    var diffuse: Color3
    var shadowMinZ: Float
    var shadowMaxZ: Float
    var position: Vector3
    var direction: Vector3
}

external class HemisphericLight(name: String, direction: Vector3, scene: Scene) {
    var diffuse: Color3
    var groundColor: Color3
    var intensity: Float
}

external class Scene(engine: Engine? = definedExternally) {
    var fogMode: Int
    var fogDensity: Float
    var fogColor: Color3
    var clearColor: Color4
    var ambientColor: Color3
    var activeCamera: Camera?
    var pointerX: Int
    var pointerY: Int
    var deltaTime: Float
    val onKeyboardObservable: Observable<KeyboardInfo>
    val onPointerObservable: Observable<PointerInfo>

    fun render()
    fun dispose()
    fun createPickingRay(x: Int, y: Int, world: Matrix, camera: Camera? = definedExternally): Ray
    fun registerBeforeRender(callback: () -> Unit)
    fun getFrameId(): Int
    fun removeMesh(mesh: AbstractMesh)

    companion object {
        val FOGMODE_EXP2: Int
    }
}
external class Mesh(name: String = definedExternally, scene: Scene = definedExternally) : AbstractMesh {
    fun createInstance(name: String): Mesh

    companion object {
        val BILLBOARDMODE_Y: Int
        val BILLBOARDMODE_ALL: Int
        val ALPHA_DISABLE: Int
        val ALPHA_ADD: Int
        val ALPHA_COMBINE: Int
        val ALPHA_SUBTRACT: Int
        val ALPHA_MULTIPLY: Int
        val ALPHA_MAXIMIZED: Int
        val ALPHA_ONEONE: Int
        val ALPHA_PREMULTIPLIED: Int
        val ALPHA_PREMULTIPLIED_PORTERDUFF: Int
        val ALPHA_COMPLEX: Int
    }
}
external class MeshBuilder {
    companion object {
        fun CreateGround(name: String, options: dynamic = definedExternally, scene: Scene = definedExternally): Mesh
        fun CreatePlane(name: String, options: dynamic = definedExternally, scene: Scene = definedExternally): Mesh
        fun CreateSphere(name: String, options: dynamic = definedExternally, scene: Scene = definedExternally): Mesh
    }
}


abstract external class Material {
    var backFaceCulling: Boolean
    var alphaMode: Int
    var needDepthPrePass: Boolean
    var wireframe: Boolean
    var zOffset: Float
    var cullBackFaces: Boolean
    var transparencyMode: Int
    var alpha: Float
    var id: String
    var name: String

    fun clone(name: String): Material
    fun dispose(forceDisposeEffect: Boolean = definedExternally)
    fun isReady(): Boolean

    companion object {
        val MATERIAL_ALPHATEST: Int
    }
}

external class StandardMaterial(name: String, scene: Scene) : Material {
    var diffuseTexture: Texture
    var diffuseColor: Color3
    var specularColor: Color3
    var emissiveColor: Color3
    var disableColorWrite: Boolean
    var disableDepthWrite: Boolean
    var zOffsetUnits: Int
    var roughness: Float
    var disableLighting: Boolean
    var useAlphaFromDiffuseTexture: Boolean
}

open external class Texture(url: String, scene: Scene, options: dynamic = definedExternally) {
    var hasAlpha: Boolean
    var wrapU: Int
    var wrapV: Int
    var url: String

    fun dispose()

    companion object {
        val NEAREST_SAMPLINGMODE: Int
        val BILINEAR_SAMPLINGMODE: Int
        val TRILINEAR_SAMPLINGMODE: Int
        val NEAREST_NEAREST_MIPLINEAR: Int
        val NEAREST_LINEAR_MIPNEAREST: Int
        val NEAREST_LINEAR_MIPLINEAR: Int
        val NEAREST_LINEAR: Int
        val NEAREST_NEAREST: Int
        val LINEAR_NEAREST_MIPNEAREST: Int
        val LINEAR_NEAREST_MIPLINEAR: Int
        val LINEAR_LINEAR_MIPNEAREST: Int
        val LINEAR_LINEAR_MIPLINEAR: Int
        val CLAMP_ADDRESSMODE: Int
    }
}

external class Engine(
    canvas: HTMLCanvasElement = definedExternally,
    antialias: Boolean = definedExternally,
    options: dynamic = definedExternally,
    adaptToDeviceRatio: Boolean = definedExternally
) {
    fun runRenderLoop(callback: () -> Unit)
    fun setHardwareScalingLevel(scalingLevel: Int)
    fun resize()
    fun dispose()

    companion object {
        val TEXTUREFORMAT_ALPHA: Int
    }
}

external class Observable<T> {
    fun add(callback: (T) -> Unit)
}

external class ShadowGenerator(mapSize: Int, light: DirectionalLight) {
    fun addShadowCaster(mesh: AbstractMesh)
    fun removeShadowCaster(mesh: AbstractMesh)
}

external class Vector3(x: Float = definedExternally, y: Float = definedExternally, z: Float = definedExternally) {
    var x: Float
    var y: Float
    var z: Float

    fun normalize(): Vector3
    fun copyFrom(source: Vector3): Vector3
    fun length(): Float
    fun scaleInPlace(scale: Float): Vector3
    fun add(otherVector: Vector3): Vector3
    fun addInPlace(otherVector: Vector3): Vector3
    fun multiply(otherVector: Vector3): Vector3
    fun scale(scale: Float): Vector3
    fun subtract(otherVector: Vector3): Vector3
    fun floor(): Vector3
    fun equals(otherVector: Vector3): Boolean
    fun projectOnPlaneToRef(plane: Plane, result: Vector3, origin: Vector3)
    fun clone(): Vector3

    companion object {
        fun Zero(): Vector3
        fun Forward(): Vector3
        fun Right(): Vector3
        fun Up(): Vector3
        fun Down(): Vector3
        fun Left(): Vector3
        fun Backward(): Vector3
        fun TransformCoordinates(vector: Vector3, transformation: Matrix): Vector3
        fun Distance(v1: Vector3, v2: Vector3): Float
        fun Lerp(start: Vector3, end: Vector3, amount: Float): Vector3
    }
}

external class Matrix {
    fun invert(): Matrix

    companion object {
        fun Identity(): Matrix
    }
}

external class Quaternion {
    fun toEulerAngles(): Vector3

    companion object {
        fun Identity(): Quaternion
        fun FromLookDirectionRHToRef(direction: Vector3, up: Vector3, result: Quaternion): Quaternion
        fun FromLookDirectionLH(direction: Vector3, up: Vector3): Quaternion
    }
}

abstract external class Camera {
    var minZ: Float
    var maxZ: Float
    var position: Vector3
    var fov: Float
    var globalPosition: Vector3

    fun getDirection(direction: Vector3): Vector3
}

external class ArcRotateCamera(
    name: String,
    alpha: Float,
    beta: Float,
    radius: Float,
    target: Vector3,
    scene: Scene
) : Camera {
    var alpha: Float
    var beta: Float
    var radius: Float
    var target: Vector3
    var zoomToMouseLocation: Boolean
    var wheelDeltaPercentage: Float
    var wheelPrecision: Float
    var lowerRadiusLimit: Float
    var inertialRadiusOffset: Float
    var inertialAlphaOffset: Float
    var inertialBetaOffset: Float
    var targetScreenOffset: Vector3
    var inputs: ArcRotateCameraInputs

    fun attachControl(noPreventDefault: Boolean = definedExternally)
    fun detachControl()
    fun getViewMatrix(): Matrix
    fun setTarget(target: Vector3)
}

external class ArcRotateCameraInputs {
    val attached: dynamic
}

external class ArcRotateCameraKeyboardMoveInput {
    var angularSpeed: Float
}

external class KeyboardEventTypes {
    companion object {
        val KEYDOWN: Number
        val KEYUP: Number
    }
}

external class KeyboardInfo {
    val type: Number
    val event: KeyboardEvent
}

external class KeyboardEvent {
    val key: String
    val shiftKey: Boolean
    val ctrlKey: Boolean
    val altKey: Boolean
    fun preventDefault()
}

external class PointerEventTypes {
    companion object {
        val POINTERDOWN: Number
        val POINTERUP: Number
        val POINTERMOVE: Number
        val POINTERWHEEL: Number
        val POINTERPICK: Number
        val POINTERTAP: Number
        val POINTERDOUBLETAP: Number
    }
}

external class PointerInfo {
    val type: Number
    val event: PointerEvent
}

external class PointerEvent {
    val shiftKey: Boolean
    val ctrlKey: Boolean
    val altKey: Boolean
}

external class ActionManager(scene: Scene) {
    fun registerAction(action: ExecuteCodeAction)
}

external class ExecuteCodeAction(
    trigger: Number,
    action: () -> Unit
)

external class Ray {
    var origin: Vector3
    var direction: Vector3

    fun intersectsMesh(mesh: AbstractMesh): PickingInfo
}

external class PickingInfo {
    val pickedPoint: Vector3?
}

external class Plane {
    companion object {
        fun FromPositionAndNormal(position: Vector3, normal: Vector3): Plane
    }
}

external class VertexBuffer {
    companion object {
        val PositionKind: String
    }
}

external class VertexData {
    var positions: Array<Float>?
    var indices: Array<Int>?
    var normals: Array<Float>?
    var uvs: Array<Float>?

    fun applyToMesh(mesh: Mesh, updatable: Boolean)

    companion object {
        fun ComputeNormals(positions: Array<Float>, indices: Array<Int>, normals: Array<Float>)
    }
}

abstract external class PostProcess {
    fun dispose()
}

external class ColorCorrectionPostProcess(name: String, colorTableUrl: String, ratio: Float, camera: Camera) : PostProcess

external class DefaultRenderingPipeline(
    name: String,
    hdr: Boolean,
    scene: Scene,
    cameras: Array<Camera?>
) {
    var depthOfFieldEnabled: Boolean
    var depthOfField: DepthOfFieldEffect
    var depthOfFieldBlurLevel: Int
    var bloomEnabled: Boolean
    var bloomThreshold: Float
    var bloomWeight: Float
    var bloomKernel: Float
    var bloomScale: Float
    var sharpenEnabled: Boolean
    var sharpen: ImageProcessingPostProcess
    var hdrEnabled: Boolean

}

external class DepthOfFieldEffect {
    var fStop: Float
    var focalLength: Float
    var focusDistance: Float
}

external class ImageProcessingPostProcess {
    var edgeAmount: Float
    var colorAmount: Float
}

external class DepthOfFieldEffectBlurLevel {
    companion object {
        val High: Int
    }
}

external class LensRenderingPipeline(
    name: String,
    parameters: dynamic,
    scene: Scene,
    ratio: Float,
    cameras: Array<Camera>
)

external class SSAO2RenderingPipeline(
    name: String,
    scene: Scene,
    options: dynamic,
    cameras: Array<ArcRotateCamera>
) {
    var radius: Float
    var totalStrength: Float
    var samples: Int
    var bypassBlur: Boolean
    var bilateralSamples: Int
    var maxZ: Float
}

external interface CreatePlaneOptions {
    val size: Float? get() = definedExternally
    val updatable: Boolean? get() = definedExternally
    val width: Float? get() = definedExternally
    val height: Float? get() = definedExternally
}

external interface CreateSphereOptions {
    val diameter: Float? get() = definedExternally
    val segments: Int? get() = definedExternally
    val updatable: Boolean? get() = definedExternally
}

external interface CreateGroundOptions {
    val width: Float? get() = definedExternally
    val height: Float? get() = definedExternally
    val subdivisions: Int? get() = definedExternally
}

external interface TextureOptions {
    val samplingMode: Int? get() = definedExternally
}

external interface DynamicTextureOptions {
    val width: Int? get() = definedExternally
    val height: Int? get() = definedExternally
}

external class DynamicTexture(
    name: String,
    options: DynamicTextureOptions,
    scene: Scene,
    generateMipMaps: Boolean = definedExternally,
    samplingMode: Int = definedExternally,
    format: Int = definedExternally
) : Texture {

    fun update(invertY: Boolean = definedExternally)
    fun drawText(
        text: String,
        x: Int,
        y: Int,
        font: String,
        color: String,
        clearColor: String = definedExternally,
        invertY: Boolean = definedExternally,
        update: Boolean = definedExternally
    )
    fun getContext(): CanvasRenderingContext2D
}
