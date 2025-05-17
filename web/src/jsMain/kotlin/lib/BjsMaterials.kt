@file:JsModule("@babylonjs/materials")
@file:JsNonModule

package lib

external class GridMaterial(name: String, scene: Scene) : Material {
    var opacityTexture: Texture
    var mainColor: Color3
    var lineColor: Color3
    var opacity: Float
    var gridRatio: Float
    var majorUnitFrequency: Int
    var zOffsetUnits: Float
    var fogEnabled: Boolean
    var gridOffset: Vector3
    var antialias: Boolean
}
