package com.queatz

import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.sin
import kotlin.random.Random

// https://stackoverflow.com/a/50506609/2938945
fun List<Double>.offsetGeo(latitudeMeters: Double, longitudeMeters: Double): List<Double> {
    val earth = 6378.137 //radius of the earth in kilometer
    val m = 1.0 / (2.0 * PI / 360.0 * earth) / 1_000.0 // 1 meter in degrees
    val latitude = get(0) + latitudeMeters * m
    val longitude = get(1) + (longitudeMeters * m) / cos(get(0) * (PI / 180.0))

    return listOf(latitude, longitude)
}

fun List<Double>.scatterGeo(maxDistance: Double = 1000.0): List<Double> {
    val angle = Random.nextDouble() * PI * 2
    val distance = maxDistance * Random.nextDouble()
    return offsetGeo(sin(angle) * distance, cos(angle) * distance)
}
