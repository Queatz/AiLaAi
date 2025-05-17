@file:JsModule("@babylonjs/core")
@file:JsNonModule

package lib

/**
 * The GlowLayer Helps adding a glow effect around meshes.
 * Once instantiated in a scene, simply use the pushMesh or removeMesh methods to add or remove
 * glowy meshes to your scene.
 */
external class GlowLayer(name: String, scene: Scene) {
    /**
     * The intensity of the glow
     */
    var intensity: Float

    /**
     * The color of the glow
     */
    var blurKernelSize: Float

    /**
     * Add a mesh to the glow layer
     */
    fun addIncludedOnlyMesh(mesh: AbstractMesh): AbstractMesh

    /**
     * Remove a mesh from the included only meshes
     */
    fun removeIncludedOnlyMesh(mesh: AbstractMesh): AbstractMesh

    /**
     * Dispose the glow layer
     */
    fun dispose()
}
