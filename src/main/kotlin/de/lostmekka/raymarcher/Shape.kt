package de.lostmekka.raymarcher

import kotlin.math.abs
import kotlin.math.max

typealias DistanceEstimator = (Point) -> EstimatedDistance

data class EstimatedDistance(val distance: Double, val shape: Shape)

private fun Collection<DistanceEstimator>.max(): DistanceEstimator = { point ->
    this
        .map { it(point) }
        .minBy { it.distance }
        ?: throw Exception("cannot estimate distance of empty scene")
}

abstract class Shape(val material: Material) {
    abstract val distanceEstimator: DistanceEstimator
}

class Scene(children: Collection<Shape>) {
    constructor(vararg children: Shape) : this(children.toList())

    val distanceEstimator: DistanceEstimator = children.map { it.distanceEstimator }.max()
}

class Sphere(val center: Point, val radius: Double, material: Material) : Shape(material) {
    override val distanceEstimator: DistanceEstimator = {
        EstimatedDistance(max(.0, (it distanceTo center) - radius), this)
    }
}

class Plane(val origin: Point, val normal: Point, material: Material) : Shape(material) {
    override val distanceEstimator: DistanceEstimator = {
        EstimatedDistance(abs((it - origin) dot normal.normalized), this)
    }
}
