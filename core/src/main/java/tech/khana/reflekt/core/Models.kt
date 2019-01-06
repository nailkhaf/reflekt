package tech.khana.reflekt.core

import android.annotation.SuppressLint
import android.graphics.ImageFormat
import android.graphics.SurfaceTexture
import android.hardware.camera2.CameraDevice
import android.hardware.camera2.CameraMetadata
import android.hardware.camera2.CaptureRequest
import android.view.Surface

data class UserSettings(
    val surfaces: List<ReflektSurface>,
    val rotation: Rotation,
    val direct: Lens = Lens.FRONT,
    val flashMode: FlashMode = FlashMode.OFF,
    val supportLevel: SupportLevel = SupportLevel.LEGACY
)

enum class SurfaceType(val value: Int) {
    PREVIEW(CameraDevice.TEMPLATE_PREVIEW),
    CAPTURE(CameraDevice.TEMPLATE_STILL_CAPTURE)
}

@Suppress("EnumEntryName")
enum class Rotation {
    _0,
    _90,
    _180,
    _270
}

internal fun rotationOf(value: Int) = when (value) {
    Surface.ROTATION_0 -> Rotation._0
    Surface.ROTATION_90 -> Rotation._90
    Surface.ROTATION_180 -> Rotation._180
    Surface.ROTATION_270 -> Rotation._270
    else -> throw IllegalArgumentException("unknown rotation")
}

data class SurfaceConfig(val resolutions: List<Resolution>, val rotation: Rotation)

data class TypedSurface(val type: SurfaceType, val surface: Surface)

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

data class Resolution(val width: Int, val height: Int)

val Resolution.area
    get() = width * height

val Resolution.ratio: Float
    get() = width.toFloat() / height.toFloat()

enum class Lens(val value: Int) {
    FRONT(CameraMetadata.LENS_FACING_FRONT),
    BACK(CameraMetadata.LENS_FACING_BACK)
}

@SuppressLint("InlinedApi")
enum class SupportLevel(val value: Int, val description: String) {
    LEVEL_3(CameraMetadata.INFO_SUPPORTED_HARDWARE_LEVEL_3, "Level 3 support"),
    LEGACY(CameraMetadata.INFO_SUPPORTED_HARDWARE_LEVEL_LEGACY, "Legacy support"),
    FULL(CameraMetadata.INFO_SUPPORTED_HARDWARE_LEVEL_FULL, "Full support"),
    LIMIT(CameraMetadata.INFO_SUPPORTED_HARDWARE_LEVEL_LIMITED, "Limited support"),
    EXTERNAL(
        CameraMetadata.INFO_SUPPORTED_HARDWARE_LEVEL_EXTERNAL,
        "External support, like limited"
    )
}

enum class FlashMode(val value: Int) {
    OFF(CaptureRequest.FLASH_MODE_OFF),
    PHOTO(CaptureRequest.FLASH_MODE_SINGLE),
    TORCH(CaptureRequest.FLASH_MODE_TORCH),
    SCREEN(777),
}