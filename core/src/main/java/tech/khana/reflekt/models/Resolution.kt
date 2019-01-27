package tech.khana.reflekt.models

data class Resolution(val width: Int, val height: Int)

val Resolution.area
    get() = width * height

val Resolution.ratio: Float
    get() = width.toFloat() / height.toFloat()
