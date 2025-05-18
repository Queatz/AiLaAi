package com.queatz.db

import kotlinx.serialization.Serializable

@Serializable
data class GameScene(
    var person: String? = null,
    var published: Boolean? = null,
    var url: String? = null,
    var name: String? = null,
    var categories: List<String>? = null,
    var tiles: String? = null,
    var objects: String? = null,
    var config: String? = null,
    var photo: String? = null,
) : Model()

@Serializable
data class GameDiscussion(
    var person: String? = null,        // Creator of the discussion
    var scene: String? = null,         // Reference to the game scene
    var title: String? = null,         // Optional title for the discussion
    var position: Vector3Data? = null, // 3D position in the scene
    var comment: String? = null,       // The initial comment text
    var resolved: Boolean? = null,     // Whether the discussion is resolved
) : Model()

@Serializable
data class GameTile(
    var person: String? = null,
    var name: String? = null,
    var published: Boolean? = null,
    var categories: List<String>? = null,
    var photo: String? = null,
    var offset: Double? = null,
) : Model()

@Serializable
data class GameObject(
    var person: String? = null,
    var name: String? = null,
    var published: Boolean? = null,
    var categories: List<String>? = null,
    var photo: String? = null,
    var width: String? = null,
    var height: String? = null,
) : Model()

@Serializable
data class GameMusic(
    var person: String? = null,
    var name: String? = null,
    var published: Boolean? = null,
    var categories: List<String>? = null,
    var audio: String? = null,
    var duration: Double? = null,
) : Model()

/**
 * Serializable data class for storing animation configuration
 */
@Serializable
data class GameSceneConfig(
    val markers: List<AnimationMarkerData> = emptyList(),
    val cameraKeyframes: List<CameraKeyframeData> = emptyList(),
    val backgroundColor: Color4Data? = null,
    val cameraFov: Float? = null,
    val ssaoEnabled: Boolean? = null,
    val bloomEnabled: Boolean? = null,
    val sharpenEnabled: Boolean? = null,
    val colorCorrectionEnabled: Boolean? = null,
    val linearSamplingEnabled: Boolean? = null,
    val depthOfFieldEnabled: Boolean? = null,
    val ambienceIntensity: Float? = null,
    val sunIntensity: Float? = null,
    val fogDensity: Float? = null,
    val timeOfDay: Float? = null,
    val pixelSize: Int? = null,
    val brushSize: Int? = null,
    val brushDensity: Int? = null,
    val gridSize: Int? = null,
    val snowEffectEnabled: Boolean? = null,
    val snowEffectIntensity: Float? = null,
    val rainEffectEnabled: Boolean? = null,
    val rainEffectIntensity: Float? = null,
    val dustEffectEnabled: Boolean? = null,
    val dustEffectIntensity: Float? = null
)

/**
 * Serializable version of Color4 (RGBA)
 */
@Serializable
data class Color4Data(
    val r: Float,
    val g: Float,
    val b: Float,
    val a: Float = 1.0f
)

/**
 * Serializable version of AnimationMarker
 */
@Serializable
data class AnimationMarkerData(
    val id: String,
    val name: String,
    val time: Double,
    val duration: Double = 0.0
)

/**
 * Serializable version of Vector3
 */
@Serializable
data class Vector3Data(
    val x: Float,
    val y: Float,
    val z: Float
)

/**
 * Serializable version of CameraKeyframe
 */
@Serializable
data class CameraKeyframeData(
    val id: String,
    val time: Double,
    val position: Vector3Data,
    val target: Vector3Data,
    val alpha: Float,
    val beta: Float,
    val radius: Float,
    val fov: Float
)
