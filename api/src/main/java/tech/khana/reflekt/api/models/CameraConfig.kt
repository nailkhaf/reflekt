package tech.khana.reflekt.api.models

import android.util.Size
import tech.khana.reflekt.api.Surface

data class CameraConfig(
    val surfaces: List<Surface>,
    val screenSize: Size,
    val screenOrientation: Int
)