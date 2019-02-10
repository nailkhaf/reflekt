package tech.khana.reflekt.models

data class Resolution(val width: Int = 0, val height: Int = 0)

val Resolution.area
    get() = width * height

val Resolution.ratio: Float
    get() = width.toFloat() / height.toFloat()
