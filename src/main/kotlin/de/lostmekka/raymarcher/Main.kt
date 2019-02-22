package de.lostmekka.raymarcher

import kotlinx.coroutines.*
import kotlinx.coroutines.channels.Channel
import java.awt.image.BufferedImage
import java.io.File
import java.util.concurrent.Executors
import javax.imageio.ImageIO
import kotlin.math.*

private suspend fun BufferedImage.write(fileName: String = "out", path: String = ".", format: String = "png") {
    val pathWithoutTrailingSlash = path.trimEnd('/', '\\')
    val fullFilePath = "$pathWithoutTrailingSlash/$fileName.$format"
    withContext(Dispatchers.IO) {
        ImageIO.write(this@write, format, File(fullFilePath))
    }
}

private suspend fun measureSeconds(body: suspend () -> Unit): Double {
    val start = System.currentTimeMillis()
    body()
    val end = System.currentTimeMillis()
    return 0.001 * (end - start)
}

private suspend fun measureMilliSeconds(body: suspend () -> Unit): Long {
    val start = System.currentTimeMillis()
    body()
    val end = System.currentTimeMillis()
    return end - start
}

private suspend fun <T : Any> measureMilliSecondsWithAnswer(body: suspend () -> T): Pair<Long, T> {
    var answer: T? = null
    val time = measureMilliSeconds { answer = body() }
    return Pair(time, answer!!)
}

fun main() = runBlocking {
    val imageScaling = 0.2
    val pixelSize = 4

    val timeSpent = measureSeconds {
        val imageCount = 400
        val rotationCount = 8

        val maxImageIndexDigitCount = floor(log10(imageCount.toDouble())).toInt() + 1
        fun Int.padZero(length: Int = maxImageIndexDigitCount) = toString().padStart(length, '0')
        fun Int.padSpace(length: Int = maxImageIndexDigitCount) = toString().padStart(length, ' ')

        // leaving one core alone so i can still do things while this renders
        val threadCount = max(1, Runtime.getRuntime().availableProcessors() - 1)
        println("auto configured $threadCount threads.")
        val dispatcher = Executors
            .newFixedThreadPool(threadCount)
            .asCoroutineDispatcher()
        var imagesCompleted = 0
        var totalRenderCpuTime = 0L
        val answerChannel = Channel<Long>(imageCount + 1)

        println("deleting all previous output images...")
        File(".")
            .listFiles { _, name -> name.matches(Regex("""out_\d+\.png""")) }
            .forEach { it.delete() }

        println("scheduling $imageCount async render jobs...")
        (0 until imageCount).map { imageIndex ->
            launch(dispatcher) {
                val (time, image) = measureMilliSecondsWithAnswer {
                    drawImage(
                        state = imageIndex.toDouble() / (imageCount - 1),
                        imageScaling = imageScaling,
                        pixelSize = pixelSize,
                        rotationCount = rotationCount
                    )
                }
                answerChannel.send(time)
                val numberInName = imageIndex.toString().padStart(maxImageIndexDigitCount, '0')
                image.write("out_$numberInName")
            }
        }

        println("listening for render results...")
        repeat(imageCount) {
            val time = answerChannel.receive()
            imagesCompleted++
            totalRenderCpuTime += time
            val estimate =
                ((imageCount - imagesCompleted) * totalRenderCpuTime.toDouble() / imagesCompleted / threadCount).toInt()
            val millis = (estimate % 1000).padZero(3)
            val seconds = (estimate / 1000 % 60).padZero(2)
            val minutes = (estimate / 1000 / 60 % 60).padZero(2)
            val hours = (estimate / 1000 / 60 / 60).padZero(2)
            println("completed ${imagesCompleted.padSpace()} images. estimated time to go: $hours:$minutes:$seconds.$millis")
        }
        dispatcher.close()

//        val image = drawImage(0.0)
//        ImageIO.write(image, "png", File("out.png"))
    }
    println("done in $timeSpent seconds")
}

fun Double.mapWave(min: Double, max: Double, phase: Double = 0.0, frequency: Double = 1.0) =
    (-cos(2 * Math.PI * (this * frequency + phase)) + 1) / 2 * (max - min) + min

fun Double.mapLinear(min: Double, max: Double) = this / (max - min) + min

private fun drawImage(state: Double, imageScaling: Double, rotationCount: Int = 1, pixelSize: Int = 1): BufferedImage {
    val floorMaterial = CheckerboardMaterial(Color(0.8), Color(0.4), 2.0)
    val scene = Scene(
        Cube(1.0, SingleColorMaterial(1.0, 0.2, 0.2)).apply {
            repeat(3) {
                translate(-1.0, -1.0, -1.0)
                mirrorOnPlane(Point(-0.5, 0.0, 0.0), Point.left)
//                mirrorOnPlane(Point(-0.5, 0.0, 0.0), zRotationMatrix(state.mapWave(-0.3, 0.0)) * Point.left)

//                mirrorOnPlane(Point.zero, Point(1, 0, -1))
                mirrorOnPlane(Point.zero, zRotationMatrix(state.mapLinear(0.0, 2.0)) * Point(1, 0, -1))

                mirrorOnPlane(Point.zero, Point(0, -1, 1))
//                mirrorOnPlane(Point.zero, zRotationMatrix(state.mapLinear(0.0, -0.5)) * Point(0, -1, 1))

                mirrorOnPlane(Point.zero, Point.backward)
                mirrorOnPlane(Point.zero, Point.down)
                mirrorOnPlane(Point.zero, Point.left)
                scale(1.0 / 3.0)
            }
            rotateY(state * 2 * Math.PI * rotationCount)
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

    val imageWidth = (1920 * imageScaling).roundToInt()
    val imageHeight = (1080 * imageScaling).roundToInt()
    val image = BufferedImage(imageWidth * pixelSize, imageHeight * pixelSize, BufferedImage.TYPE_INT_ARGB)

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
            for (ix in (0 until pixelSize))
                for (iy in (0 until pixelSize))
                    image.setRGB(pixelSize * x + ix, pixelSize * y + iy, color.toInt())
        }
    }
    return image
}

private fun Double.toColorValue() = (coerceIn(0.0, 1.0) * 255).roundToInt()
private fun Color.toInt(): Int = clamp().run {
    blue.toColorValue() + green.toColorValue().shl(8) + red.toColorValue().shl(16) + 0xff000000.toInt()
}
