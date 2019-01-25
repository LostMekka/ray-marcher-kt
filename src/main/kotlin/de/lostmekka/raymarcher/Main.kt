package de.lostmekka.raymarcher

import java.awt.image.BufferedImage
import java.io.File
import javax.imageio.ImageIO
import kotlin.math.roundToInt
import kotlin.math.sin

fun main(args: Array<String>) {
    val startTime = System.currentTimeMillis()

    val count = 100
    for (i in (0 until count)) {
        println("drawing image ${i + 1} of $count...")
        val fraction = i.toDouble() / (count - 1)
        val image = drawImage(fraction)
        val numberInName = i.toString().padStart(3, '0')
        ImageIO.write(image, "png", File("out_$numberInName.png"))
    }
//    val image = drawImage(0.0)
//    ImageIO.write(image, "png", File("out.png"))

    val endTime = System.currentTimeMillis()
    val timeSpent = (endTime - startTime) / 1_000f
    println("done in $timeSpent seconds")
}

private fun drawImage(state: Double): BufferedImage {
    val floorMaterial = CheckerboardMaterial(Color(0.8), Color(0.4), 2.0)
    val scene = Scene(
        Cube(1.0, SingleColorMaterial(1.0, 0.2, 0.2)).apply {
            repeat(3) {
                translate(-1.0, -1.0, -1.0)
                mirrorOnPlane(Point(-0.5, 0.0, 0.0), zRotationMatrix(0.25 * sin(state * 2 * Math.PI)) * Point.left)
                mirrorOnPlane(Point.zero, Point(1, 0, -1))
                mirrorOnPlane(Point.zero, Point(0, -1, 1))
                mirrorOnPlane(Point.zero, Point.backward)
                mirrorOnPlane(Point.zero, Point.down)
                mirrorOnPlane(Point.zero, Point.left)
                scale(1.0 / 3.0)
            }
            rotateY(state * 2 * Math.PI)
        },
        Plane(Point.up, floorMaterial).apply {
            translate(Point.down * 0.6)
        },
        Plane(Point.forward, floorMaterial).apply {
            translate(Point.forward * 5.0)
        }
    )
    val hitDistance = 0.01

    val light = PointLight(
        position = Point(-1.5, 1.0, -3.2),
        minDistance = 3.0,
        maxDistance = 9.5,
        intensity = 0.9
    )
//    val ambientOcclusionMaxMarchCount = 30
//    val ambientOcclusionIntensity = 0.2
    val diffuseIntensity = 0.09

    val camera = Point(0, 0.08, -3.0)
    val frustumUpperLeftCorner = Point(-8.0, 4.5, 15.0) + camera
    val frustumLowerRightCorner = Point(8.0, -4.5, 15.0) + camera

    val imageWidth = 1920 / 3
    val imageHeight = 1080 / 3
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
                geometry = scene
            )
            val color = when (marchResult) {
                is RayMarchMiss -> Color.black
                is RayMarchHit -> {
                    val lightAmount = light.hardShadowedIntensityAt(
                        marchResult.hitPoint,
                        marchResult.hitNormal,
                        scene,
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
    return image
}

private fun Double.toColorValue() = (coerceIn(0.0, 1.0) * 255).roundToInt()
private fun Color.toInt(): Int = clamp().run {
    blue.toColorValue() + green.toColorValue().shl(8) + red.toColorValue().shl(16) + 0xff000000.toInt()
}
