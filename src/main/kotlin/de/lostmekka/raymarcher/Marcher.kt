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
    distanceEstimator: DistanceEstimator
): RayMarchResult {
    var currentPosition = start
    var estimatedDistance: Double
    var marchCount = 0
    var minEstimatedDistance = Double.MAX_VALUE
    var maxEstimatedDistance = Double.MIN_VALUE
    while (true) {
        val estimate = distanceEstimator(currentPosition)
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
            val probePosition = currentPosition - direction.normalized * hitDistance
            val probeDistance = hitDistance / 2
            val left = distanceEstimator(probePosition + Point.left * probeDistance).distance
            val right = distanceEstimator(probePosition + Point.right * probeDistance).distance
            val up = distanceEstimator(probePosition + Point.up * probeDistance).distance
            val down = distanceEstimator(probePosition + Point.down * probeDistance).distance
            val forward = distanceEstimator(probePosition + Point.forward * probeDistance).distance
            val backward = distanceEstimator(probePosition + Point.backward * probeDistance).distance
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

