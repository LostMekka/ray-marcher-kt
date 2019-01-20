package de.lostmekka.raymarcher

import kotlin.math.sqrt

data class Point(val x: Double, val y: Double, val z: Double) {
    val length by lazy { sqrt(squaredLength) }
    val squaredLength by lazy { x * x + y * y + z * z }
    val isNormalized by lazy { squaredLength == 1.0 }
    val normalized: Point by lazy { Point(x / length, y / length, z / length) }

    companion object {
        val zero = Point(.0, .0, .0)
        val right = Point(1.0, .0, .0)
        val left = Point(-1.0, .0, .0)
        val up = Point(.0, 1.0, .0)
        val down = Point(.0, -1.0, .0)
        val forward = Point(.0, .0, 1.0)
        val backward = Point(.0, .0, -1.0)
    }
}

operator fun Point.plus(other: Point) = Point(x + other.x, y + other.y, z + other.z)
operator fun Point.minus(other: Point) = Point(x - other.x, y - other.y, z - other.z)
operator fun Point.unaryMinus() = Point(-x, -y, -z)
operator fun Point.times(scalar: Double) = Point(x * scalar, y * scalar, z * scalar)
operator fun Point.div(scalar: Double) = Point(x / scalar, y / scalar, z / scalar)

infix fun Point.dot(other: Point) = x * other.x + y * other.y + z * other.z
infix fun Point.cross(other: Point) = Point(
    x * other.z - z * other.y,
    z * other.x - x * other.z,
    x * other.y - y * other.x
)

private fun square(scalar: Double) = scalar * scalar
infix fun Point.distanceTo(other: Point) = sqrt(squaredDistanceTo(other))
infix fun Point.squaredDistanceTo(other: Point) = square(x - other.x) + square(y - other.y) + square(z - other.z)
