package tech.khana.reflekt.models

import android.graphics.ImageFormat
import android.graphics.SurfaceTexture
import tech.khana.reflekt.core.ReflektSurface
import tech.khana.reflekt.models.AspectRatio.AR_4X3

data class Settings(
    val surfaces: List<ReflektSurface> = emptyList(),
    val displayRotation: Rotation = Rotation._0,
    val displayResolution: Resolution = Resolution(1280, 720),
    val aspectRatio: AspectRatio = AR_4X3,
    val lensDirect: LensDirect = LensDirect.FRONT
)

data class SurfaceConfig(
    val resolutions: List<Resolution>,
    val aspectRatio: AspectRatio,
    val displayRotation: Rotation,
    val hardwareRotation: Rotation,
    val lensDirect: LensDirect
)

sealed class ReflektFormat {

    sealed class Image(val format: Int) : ReflektFormat() {
        object Jpeg : Image(ImageFormat.JPEG)
        object Yuv : Image(ImageFormat.YUV_420_888)
    }

    sealed class Priv(val klass: Class<out Any>) : ReflektFormat() {
        object Texture : Priv(SurfaceTexture::class.java)
        object ImageReader : Priv(android.media.ImageReader::class.java)
        object MediaRecorder : Priv(android.media.MediaRecorder::class.java)
    }

    object None : ReflektFormat()
}

const val MAX_PREVIEW_WIDTH = 1920
const val MAX_PREVIEW_HEIGHT = 1080

enum class OutputType {
    PREVIEW,
    MAXIMUM,
    RECORD
}