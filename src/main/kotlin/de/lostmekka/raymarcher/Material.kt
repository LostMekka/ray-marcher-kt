package de.lostmekka.raymarcher

import kotlin.math.roundToInt

abstract class Material {
    abstract fun colorAt(point: Point): Color
}

class SingleColorMaterial(val color: Color) : Material() {
    override fun colorAt(point: Point) = color
}

class CheckerboardMaterial(val color1: Color, val color2: Color) : Material() {
    override fun colorAt(point: Point): Color {
        val scale = 2.0
        val x = (point.x * scale).roundToInt()
        val y = (point.y * scale).roundToInt()
        val z = (point.z * scale).roundToInt()
        return if (Math.floorMod(x + y + z, 2) >= 1) color1 else color2
    }
}
