package de.lostmekka.raymarcher

import kotlin.math.abs
import kotlin.math.cos
import kotlin.math.sin
import kotlin.math.sqrt

data class Point(val x: Double, val y: Double, val z: Double) {
    constructor(x: Number, y: Number, z: Number) : this(x.toDouble(), y.toDouble(), z.toDouble())

    val length by lazy { sqrt(squaredLength) }
    val squaredLength by lazy { x * x + y * y + z * z }
    val isNormalized by lazy { squaredLength == 1.0 }
    val absoluteValue: Point by lazy { Point(abs(x), abs(y), abs(z)) }
    val normalized: Point by lazy { length.takeIf { it != 0.0 }?.let { Point(x / it, y / it, z / it) } ?: Point.zero }

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

infix fun Point.floorMod(other: Point) = Point(
    (x % other.x + other.x) % other.x,
    (y % other.y + other.y) % other.y,
    (z % other.z + other.z) % other.z
)

data class Matrix(
    val a00: Double, val a10: Double, val a20: Double,
    val a01: Double, val a11: Double, val a21: Double,
    val a02: Double, val a12: Double, val a22: Double
)

operator fun Matrix.times(p: Point) = Point(
    a00 * p.x + a10 * p.y + a20 * p.z,
    a01 * p.x + a11 * p.y + a21 * p.z,
    a02 * p.x + a12 * p.y + a22 * p.z
)

fun xRotationMatrix(angle: Double): Matrix {
    val sin = sin(angle)
    val cos = cos(angle)
    return Matrix(
        1.0, 0.0, 0.0,
        0.0, cos, -sin,
        0.0, sin, cos
    )
}

fun yRotationMatrix(angle: Double): Matrix {
    val sin = sin(angle)
    val cos = cos(angle)
    return Matrix(
        cos, 0.0, sin,
        0.0, 1.0, 0.0,
        -sin, 0.0, cos
    )
}

fun zRotationMatrix(angle: Double): Matrix {
    val sin = sin(angle)
    val cos = cos(angle)
    return Matrix(
        cos, -sin, 0.0,
        sin, cos, 0.0,
        0.0, 0.0, 1.0
    )
}
