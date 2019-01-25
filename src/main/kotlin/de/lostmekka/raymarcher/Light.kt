package de.lostmekka.raymarcher

import kotlin.math.max

abstract class Light(
    val position: Point,
    val minDistance: Double,
    val maxDistance: Double,
    val intensity: Double
) {
    private val distanceDifference = maxDistance - minDistance
    protected abstract fun directionToLightFrom(point: Point): Point
    protected abstract fun distanceToLightFrom(point: Point): Double
    fun hardShadowedIntensityAt(point: Point, normal: Point, geometry: Geometry, sceneHitDistance: Double): Double {
        val direction = directionToLightFrom(point)
        val distanceToLight = distanceToLightFrom(point)
        val marchResult = march(
            start = point,
            direction = direction,
            maxDistance = distanceToLight,
            geometry = geometry,
            hitDistance = sceneHitDistance,
            minFirstStepLength = sceneHitDistance * 10
        )
        return when (marchResult) {
            is RayMarchHit -> 0.0
            is RayMarchMiss -> {
                val distanceMultiplier = 1.0 - ((distanceToLight - minDistance) / distanceDifference).coerceIn(0.0, 1.0)
//                val normalMultiplier = 1.0
                val normalMultiplier = normal dot direction.normalized
                max(0.0, intensity * distanceMultiplier * normalMultiplier)
            }
        }
    }
}

class PointLight(
    position: Point,
    minDistance: Double,
    maxDistance: Double,
    intensity: Double
) : Light(position, minDistance, maxDistance, intensity) {
    override fun directionToLightFrom(point: Point): Point = position - point
    override fun distanceToLightFrom(point: Point): Double = position distanceTo point
}