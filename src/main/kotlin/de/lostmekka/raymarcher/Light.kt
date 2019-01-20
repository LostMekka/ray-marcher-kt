package de.lostmekka.raymarcher

abstract class Light(
    val position: Point,
    val minDistance: Double,
    val maxDistance: Double,
    val intensity: Double
) {
    private val distanceDifference = maxDistance - minDistance
    protected abstract fun directionToLightFrom(point: Point): Point
    protected abstract fun distanceToLightFrom(point: Point): Double
    fun hardShadowedIntensityAt(point: Point, normal: Point, sceneDistanceEstimator: DistanceEstimator, sceneHitDistance: Double): Double {
        val direction = directionToLightFrom(point)
        val marchResult = march(
            start = point,
            direction = direction,
            maxDistance = distanceToLightFrom(point),
            distanceEstimator = sceneDistanceEstimator,
            hitDistance = sceneHitDistance,
            minFirstStepLength = sceneHitDistance * 10
        )
        return when (marchResult) {
            is RayMarchHit -> 0.0
            is RayMarchMiss -> {
                val distanceMultiplier = 1.0 - ((point distanceTo position) - minDistance) / distanceDifference
                val normalMultiplier = normal dot direction.normalized
                intensity * distanceMultiplier * normalMultiplier
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