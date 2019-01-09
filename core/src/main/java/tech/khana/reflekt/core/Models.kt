package tech.khana.reflekt.core

import android.annotation.SuppressLint
import android.graphics.ImageFormat
import android.graphics.SurfaceTexture
import android.hardware.camera2.CameraDevice
import android.hardware.camera2.CameraMetadata
import android.hardware.camera2.CaptureRequest
import android.view.Surface
import tech.khana.reflekt.core.AspectRatio.AR_4X3

data class Settings(
    val surfaces: List<ReflektSurface>,
    val displayRotation: Rotation,
    val previewAspectRatio: AspectRatio = AR_4X3,
    val lens: Lens = Lens.FRONT,
    val flashMode: FlashMode = FlashMode.OFF
)

data class ReflektSettings(
    val surfaces: List<ReflektSurface>,
    val displayRotation: Rotation,
    val previewAspectRatio: AspectRatio,
    val lens: Lens,
    val flashMode: FlashMode,
    val hardwareRotation: Rotation = Rotation._90,
    val supportLevel: SupportLevel = SupportLevel.LEGACY,
    val previewActive: Boolean = false,
    val sessionActive: Boolean = false
)

fun Settings.toReflektSettings() = ReflektSettings(
    surfaces = surfaces,
    displayRotation = displayRotation,
    previewAspectRatio = previewAspectRatio,
    lens = lens,
    flashMode = flashMode
)

enum class AspectRatio(val value: Float) {
    AR_16X9(16f / 9f),
    AR_4X3(4f / 3f),
    AR_2X1(2f),
    AR_1X1(1f)
}

fun aspectRatioBy(value: Float): AspectRatio = AspectRatio.values().first { it.value == value }

fun aspectRatioBy(resolution: Resolution): AspectRatio =
    aspectRatioBy(resolution.width.toFloat() / resolution.height.toFloat())

enum class SurfaceType(val value: Int) {
    PREVIEW(CameraDevice.TEMPLATE_PREVIEW),
    CAPTURE(CameraDevice.TEMPLATE_STILL_CAPTURE)
}

/**
 * clockwise rotation, start from 180
 */
@Suppress("EnumEntryName")
enum class Rotation(val value: Int) {
    _0(0),
    _90(90),
    _180(180),
    _270(270)
}

fun displayRotationOf(value: Int) = when (value) {
    Surface.ROTATION_0 -> Rotation._0
    Surface.ROTATION_90 -> Rotation._90
    Surface.ROTATION_180 -> Rotation._180
    Surface.ROTATION_270 -> Rotation._270
    else -> throw IllegalArgumentException("unknown display rotation")
}

fun hardwareRotationOf(value: Int) = when (value) {
    0 -> Rotation._180
    90 -> Rotation._270
    180 -> Rotation._0
    270 -> Rotation._90
    else -> throw IllegalArgumentException("unknown hardware rotation")
}

data class SurfaceConfig(
    val resolutions: List<Resolution>,
    val previewAspectRatio: AspectRatio,
    val displayRotation: Rotation,
    val hardwareRotation: Rotation = Rotation._0
)

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
    SCREEN(4),
}