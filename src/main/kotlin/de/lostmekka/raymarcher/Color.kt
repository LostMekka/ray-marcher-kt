package de.lostmekka.raymarcher

data class Color(val red: Double, val green: Double, val blue: Double) {
    constructor(whiteness: Double) : this(whiteness, whiteness, whiteness)

    companion object {
        val white = Color(1.0)
        val black = Color(0.0)
        val red = Color(1.0, 0.0, 0.0)
        val green = Color(0.0, 1.0, 0.0)
        val blue = Color(0.0, 0.0, 1.0)
    }
}

private fun Double.clamp() = coerceIn(0.0, 1.0)

fun Color.clamp() = Color(red.clamp(), green.clamp(), blue.clamp())
operator fun Color.plus(other: Color) = Color(red + other.red, green + other.green, blue + other.blue)
operator fun Color.times(scalar: Double) = Color(red * scalar, green * scalar, blue * scalar)
operator fun Double.times(color: Color) = Color(color.red * this, color.green * this, color.blue * this)