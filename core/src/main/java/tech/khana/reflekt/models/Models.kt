package tech.khana.reflekt.models

import android.graphics.ImageFormat
import android.graphics.SurfaceTexture
import android.view.Surface
import tech.khana.reflekt.core.ReflektSurface
import tech.khana.reflekt.models.AspectRatio.AR_4X3

data class Settings(
    val surfaces: List<ReflektSurface> = emptyList(),
    val displayRotation: Rotation = Rotation._0,
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

data class CameraSurface(val type: CameraMode, val surface: Surface)

sealed class ReflektFormat {

    sealed class Image(val format: Int) : ReflektFormat() {
        object Jpeg : Image(ImageFormat.JPEG)
        object Yuv : Image(ImageFormat.YUV_420_888)
    }

    sealed class Clazz(val clazz: Class<out Any>) : ReflektFormat() {
        object Texture : Clazz(SurfaceTexture::class.java)
        object ImageReader : Clazz(ImageReader::class.java)
    }
}
