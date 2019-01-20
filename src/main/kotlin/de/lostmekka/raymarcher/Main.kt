package de.lostmekka.raymarcher

import java.awt.image.BufferedImage
import java.io.File
import javax.imageio.ImageIO
import kotlin.math.roundToInt

fun main(args: Array<String>) {
    val startTime = System.currentTimeMillis()

    val scene = Scene(
        Sphere(Point(1.1, 0.0, 0.0), 0.5, SingleColorMaterial(Color(1.0, 0.2, 0.2))),
        Sphere(Point(0.0, 0.0, -0.5), 0.5, SingleColorMaterial(Color(0.2, 1.0, 0.2))),
        Sphere(Point(-1.1, 0.0, 0.0), 0.5, SingleColorMaterial(Color(0.2, 0.2, 1.0))),
        Plane(Point.down * 0.3, Point.up, CheckerboardMaterial(Color(0.8), Color(0.4)))
    )
    val hitDistance = 0.01

    val light = PointLight(
        position = Point(-2.0, 2.7, -1.8),
        minDistance = 7.0,
        maxDistance = 8.0,
        intensity = 0.35
    )
    val ambientOcclusionMaxMarchCount = 30
    val ambientOcclusionIntensity = 0.2
    val diffuseIntensity = 0.15

    val camera = Point(0.0, 0.0, -5.0)
    val frustumUpperLeftCorner = Point(-4.0, 2.25, 5.0)
    val frustumLowerRightCorner = Point(4.0, -2.25, 5.0)

    val imageWidth = 1920
    val imageHeight = 1080
    val image = BufferedImage(imageWidth, imageHeight, BufferedImage.TYPE_INT_ARGB)

    val frustumPixelWidth = (frustumLowerRightCorner.x - frustumUpperLeftCorner.x) / imageWidth
    val frustumPixelHeight = (frustumLowerRightCorner.y - frustumUpperLeftCorner.y) / imageHeight
    for (x in 0 until imageWidth) {
        for (y in 0 until imageHeight) {
            val destination = Point(
                frustumUpperLeftCorner.x + x * frustumPixelWidth,
                frustumUpperLeftCorner.y + y * frustumPixelHeight,
                frustumUpperLeftCorner.z
            )
            val maxDistance = camera distanceTo destination
            val marchResult = march(
                start = camera,
                direction = destination - camera,
                hitDistance = hitDistance,
                maxDistance = maxDistance,
                distanceEstimator = scene.distanceEstimator
            )
            val color = when (marchResult) {
                is RayMarchMiss -> Color.black
                is RayMarchHit -> {
                    val lightAmount = light.hardShadowedIntensityAt(
                        marchResult.hitPoint,
                        marchResult.hitNormal,
                        scene.distanceEstimator,
                        hitDistance
                    )
                    val ambientOcclusion = 0.0
                    // ambient occlusion can be crudely estimated with the march step count.
                    // but that looks silly for flat surfaces ^^
                    // val ambientOcclusion = (marchResult.marchCount.toDouble() / ambientOcclusionMaxMarchCount) * ambientOcclusionIntensity
                    (lightAmount + diffuseIntensity - ambientOcclusion) * marchResult.color
                }
            }
            image.setRGB(x, y, color.toInt())
        }
    }

    ImageIO.write(image, "png", File("out.png"))

    val endTime = System.currentTimeMillis()
    val timeSpent = (endTime - startTime) / 1_000f
    println("done in $timeSpent seconds")
}

private fun Double.toColorValue() = (coerceIn(0.0, 1.0) * 255).roundToInt()
private fun Color.toInt(): Int = clamp().run {
    red.toColorValue() + green.toColorValue().shl(8) + blue.toColorValue().shl(16) + 0xff000000.toInt()
}
