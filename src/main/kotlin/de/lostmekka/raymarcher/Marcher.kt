package de.lostmekka.raymarcher

import kotlin.math.max
import kotlin.math.min

sealed class RayMarchResult

data class RayMarchHit(
    val hitPoint: Point,
    val hitNormal: Point,
    val hitShape: Shape,
    val marchCount: Int,
    val minEstimatedDistance: Double,
    val maxEstimatedDistance: Double
) : RayMarchResult() {
    val color by lazy { hitShape.material.colorAt(hitPoint) }
}

data class RayMarchMiss(
    val marchCount: Int,
    val minEstimatedDistance: Double,
    val maxEstimatedDistance: Double
) : RayMarchResult()

/**
 * Marches one ray from the [start] position into the given [direction].
 * A hit registers, when the [distanceEstimator] returns a value less than or equal than the given [hitDistance].
 * If the ray marches beyond the [maxDistance] from the [start] point, no hit will be registered.
 */
fun march(
    start: Point,
    direction: Point,
    maxDistance: Double,
    hitDistance: Double,
    minFirstStepLength: Double = .0,
    geometry: Geometry
): RayMarchResult {
    var currentPosition = start
    var estimatedDistance: Double
    var marchCount = 0
    var minEstimatedDistance = Double.MAX_VALUE
    var maxEstimatedDistance = Double.MIN_VALUE
    while (true) {
        val estimate = geometry.estimateDistanceTo(currentPosition)
        estimatedDistance = if (marchCount == 0 && minFirstStepLength > 0) {
            minFirstStepLength
        } else {
            estimate.distance
        }
        maxEstimatedDistance = max(maxEstimatedDistance, estimatedDistance)
        minEstimatedDistance = min(minEstimatedDistance, estimatedDistance)
        currentPosition += direction.normalized * estimatedDistance
        marchCount++
        if (currentPosition distanceTo start >= maxDistance) return RayMarchMiss(
            marchCount = marchCount,
            minEstimatedDistance = minEstimatedDistance,
            maxEstimatedDistance = maxEstimatedDistance
        )
        if (estimatedDistance <= hitDistance) {
            val probeDistance = hitDistance / 2
            val probePosition = currentPosition - direction.normalized * probeDistance
            fun probe(probeDirection: Point) = estimate.shape
                .estimateDistanceTo(probePosition + probeDirection * probeDistance)
                .distance

            val left = probe(Point.left)
            val right = probe(Point.right)
            val up = probe(Point.up)
            val down = probe(Point.down)
            val forward = probe(Point.forward)
            val backward = probe(Point.backward)
            return RayMarchHit(
                hitPoint = currentPosition,
                hitNormal = Point(right - left, up - down, forward - backward).normalized,
                hitShape = estimate.shape,
                marchCount = marchCount,
                minEstimatedDistance = minEstimatedDistance,
                maxEstimatedDistance = maxEstimatedDistance
            )
        }
    }
}

