package de.lostmekka.raymarcher

import kotlin.math.roundToInt

abstract class Material {
    abstract fun colorAt(point: Point): Color
}

class SingleColorMaterial(val color: Color) : Material() {
    constructor(red: Double, green: Double, blue: Double) : this(Color(red, green, blue))
    constructor(gray: Double) : this(Color(gray))
    override fun colorAt(point: Point) = color
}

class CheckerboardMaterial(val color1: Color, val color2: Color, val scale: Double = 1.0) : Material() {
    override fun colorAt(point: Point): Color {
        val x = (point.x * scale).roundToInt()
        val y = (point.y * scale).roundToInt()
        val z = (point.z * scale).roundToInt()
        return if (Math.floorMod(x + y + z, 2) >= 1) color1 else color2
    }
}
