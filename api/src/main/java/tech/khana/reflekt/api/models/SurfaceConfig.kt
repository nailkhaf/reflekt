package tech.khana.reflekt.api.models

import android.util.Size
import android.view.Surface

data class SurfaceConfig(
    val outputSizes: List<Size>,
    val screenOrientation: Int,
    val sensorOrientation: Int,
    val lens: Lens
)

val Size.ratio: Float
    get() = width.toFloat() / height.toFloat()

val Size.area: Int
    get() = width * height

fun getCorrectOrientation(
    lensDirect: Lens,
    sensorOrientation: Int,
    displayRotation: Int
): Int {
    var screenOrientation = (displayRotation + 45) / 90 * 90
    if (lensDirect == Lens.FRONT) screenOrientation = -screenOrientation
    return (sensorOrientation + screenOrientation + 360) % 360
}

fun getCorrectOrientation(surfaceConfig: SurfaceConfig): Int =
    getCorrectOrientation(
        surfaceConfig.lens,
        surfaceConfig.sensorOrientation,
        surfaceConfig.screenOrientation
    )

fun surfaceOrientationToInt(value: Int) = when (value) {
    Surface.ROTATION_0 -> 0
    Surface.ROTATION_90 -> 90
    Surface.ROTATION_180 -> 180
    Surface.ROTATION_270 -> 270
    else -> error("unknown surface orientation")
}