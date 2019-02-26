package tech.khana.reflekt.api.models

import android.util.Size

data class SurfaceConfig(
    val outputSize: List<Size>,
    val screenOrientation: Int,
    val sensorOrientation: Int,
    val lens: Lens
)

