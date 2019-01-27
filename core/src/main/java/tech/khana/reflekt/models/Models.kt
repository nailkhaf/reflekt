package tech.khana.reflekt.models

import android.graphics.ImageFormat
import android.graphics.Rect
import android.graphics.SurfaceTexture
import android.view.Surface
import tech.khana.reflekt.core.ReflektSurface
import tech.khana.reflekt.models.AspectRatio.AR_4X3

data class Settings(
    val surfaces: List<ReflektSurface>,
    val displayRotation: Rotation,
    val previewAspectRatio: AspectRatio = AR_4X3,
    val lensDirect: LensDirect = LensDirect.FRONT,
    val flashMode: FlashMode = FlashMode.OFF
)

data class ReflektSettings(
    val surfaces: List<ReflektSurface>,
    val displayRotation: Rotation,
    val previewAspectRatio: AspectRatio,
    val lensDirect: LensDirect,
    val flashMode: FlashMode,
    val hardwareRotation: Rotation = Rotation._90,
    val supportLevel: SupportLevel = SupportLevel.LEGACY,
    val previewActive: Boolean = false,
    val sessionActive: Boolean = false,
    val zoom: Float = 1f,
    val sensorRect: Rect = Rect()
)

fun Settings.toReflektSettings() = ReflektSettings(
    surfaces = surfaces,
    displayRotation = displayRotation,
    previewAspectRatio = previewAspectRatio,
    lensDirect = lensDirect,
    flashMode = flashMode
)


data class SurfaceConfig(
    val resolutions: List<Resolution>,
    val aspectRatio: AspectRatio,
    val displayRotation: Rotation,
    val hardwareRotation: Rotation = Rotation._0
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
