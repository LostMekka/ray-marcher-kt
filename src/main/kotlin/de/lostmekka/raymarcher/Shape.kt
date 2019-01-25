package de.lostmekka.raymarcher

import kotlin.math.abs
import kotlin.math.max

typealias SpatialTransformation = (Point) -> Point

data class EstimatedDistance(val distance: Double, val shape: Shape)

abstract class Geometry {
    protected abstract fun estimateDistanceLocally(localPoint: Point, distanceScaling: Double): EstimatedDistance
    private val transformations = mutableListOf<SpatialTransformation>()
    private var distanceScaling = 1.0

    fun addTransform(transformation: SpatialTransformation) {
        transformations += transformation
    }

    fun grid(x: Double, y: Double, z: Double) = grid(Point(x, y, z))
    fun grid(gridSize: Point) {
        transformations += {
            val halfGridSize = gridSize / 2.0
            ((it + halfGridSize) floorMod gridSize) - halfGridSize
        }
    }

    fun mirrorOnPlane(origin: Point, normal: Point) {
        transformations += {
            val d = (it - origin) dot normal.normalized
            it + (normal.normalized * (abs(d) - d))
        }
    }

    fun translate(x: Double, y: Double, z: Double) = translate(Point(x, y, z))
    fun translate(amount: Point) {
        transformations += { it - amount }
    }

    fun scale(amount: Double) {
        transformations += { it / amount }
        distanceScaling *= amount
    }

    fun rotateX(angle: Double) {
        val matrix = xRotationMatrix(angle)
        transformations += { matrix * it }
    }

    fun rotateY(angle: Double) {
        val matrix = yRotationMatrix(angle)
        transformations += { matrix * it }
    }

    fun rotateZ(angle: Double) {
        val matrix = zRotationMatrix(angle)
        transformations += { matrix * it }
    }

    fun estimateDistanceTo(point: Point, distanceScaling: Double = 1.0): EstimatedDistance {
        var p = point
        transformations.reversed().forEach { p = it(p) }
        return estimateDistanceLocally(p, this.distanceScaling * distanceScaling)
    }
}

class Scene(val children: List<Geometry>) : Geometry() {
    constructor(vararg children: Geometry) : this(children.toList())

    override fun estimateDistanceLocally(localPoint: Point, distanceScaling: Double): EstimatedDistance = children
        .map { it.estimateDistanceTo(localPoint, distanceScaling) }
        .minBy { it.distance }
        ?: throw Exception("cannot estimate distance of empty scene")
}

abstract class Shape(val material: Material) : Geometry() {
    protected abstract fun estimateLocalDistanceOnly(localPoint: Point): Double
    override fun estimateDistanceLocally(localPoint: Point, distanceScaling: Double) =
        EstimatedDistance(estimateLocalDistanceOnly(localPoint) * distanceScaling, this)
}

class Sphere(val radius: Double, material: Material) : Shape(material) {
    override fun estimateLocalDistanceOnly(localPoint: Point) = max(.0, localPoint.length - radius)
}

class Plane(val normal: Point, material: Material) : Shape(material) {
    override fun estimateLocalDistanceOnly(localPoint: Point) = abs(localPoint dot normal.normalized)
}

class Cube(val sideLength: Double, material: Material) : Shape(material) {
    override fun estimateLocalDistanceOnly(localPoint: Point) = maxOf(
        max(0.0, abs(localPoint.x) - sideLength / 2),
        max(0.0, abs(localPoint.y) - sideLength / 2),
        max(0.0, abs(localPoint.z) - sideLength / 2)
    )
}
