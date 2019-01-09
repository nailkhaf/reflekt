package tech.khana.reflekt.core

import android.content.Context
import android.hardware.camera2.CameraCharacteristics
import android.hardware.camera2.CameraManager
import android.hardware.camera2.CameraMetadata
import android.hardware.camera2.CaptureRequest
import android.hardware.camera2.params.StreamConfigurationMap
import android.util.Size
import android.view.Surface

internal val Context.cameraManager
    get() = getSystemService(Context.CAMERA_SERVICE) as CameraManager

internal fun CameraManager.findCameraByLens(direct: Lens): String =
    cameraIdList.first { directCamera(it) == direct }

internal fun CameraManager.directCamera(cameraId: String): Lens {
    val lensFacing = getCameraCharacteristics(cameraId).get(CameraCharacteristics.LENS_FACING)
    return when (lensFacing) {
        CameraMetadata.LENS_FACING_FRONT -> Lens.FRONT
        CameraMetadata.LENS_FACING_BACK -> Lens.BACK
        else -> throw IllegalArgumentException("unknown direction")
    }
}

internal fun CameraManager.outputResolutions(
    cameraId: String,
    clazz: Class<out Any>
): List<Resolution> {
    val characteristics = getCameraCharacteristics(cameraId)
    val map = characteristics.get(CameraCharacteristics.SCALER_STREAM_CONFIGURATION_MAP)!!
    if (StreamConfigurationMap.isOutputSupportedFor(clazz).not())
        throw IllegalArgumentException("Class is nut supported: ${clazz.simpleName}")
    return map.getOutputSizes(clazz).map { it.toResolution() }
}

internal fun CameraManager.outputResolutions(cameraId: String, format: Int): List<Resolution> {
    val characteristics = getCameraCharacteristics(cameraId)
    val map = characteristics.get(CameraCharacteristics.SCALER_STREAM_CONFIGURATION_MAP)!!
    if (map.isOutputSupportedFor(format).not())
        throw IllegalArgumentException("Format is not supported: $format")
    return map.getOutputSizes(format).map { it.toResolution() }
}

internal fun CameraManager.surfaceSupported(cameraId: String, surface: Surface): Boolean {
    val characteristics = getCameraCharacteristics(cameraId)
    CameraCharacteristics.CONTROL_AE_STATE_LOCKED
    val map = characteristics.get(CameraCharacteristics.SCALER_STREAM_CONFIGURATION_MAP)!!
    return map.isOutputSupportedFor(surface)
}

internal fun CameraManager.hardwareRotation(cameraId: String): Int {
    val characteristics = getCameraCharacteristics(cameraId)
    CameraCharacteristics.CONTROL_AE_STATE_LOCKED
    return characteristics.get(CameraCharacteristics.SENSOR_ORIENTATION)!!
}

internal fun CameraManager.supportedLevel(cameraId: String): SupportLevel {
    val characteristics = getCameraCharacteristics(cameraId)
    val level = characteristics.get(CameraCharacteristics.INFO_SUPPORTED_HARDWARE_LEVEL)
    return SupportLevel.values().first {
        it.value == level
    }
}

internal fun CameraManager.availableFlash(cameraId: String): Boolean {
    val characteristics = getCameraCharacteristics(cameraId)
    return characteristics.get(CameraCharacteristics.FLASH_INFO_AVAILABLE)
        ?: throw IllegalStateException()
}


internal fun Size.toResolution() = Resolution(width, height)


internal fun List<TypedSurface>.byType(type: SurfaceType): List<Surface> =
    filter { it.type == type }
        .map { it.surface }

internal fun Lens.invert() = when (this) {
    Lens.FRONT -> Lens.BACK
    Lens.BACK -> Lens.FRONT
}

internal fun CaptureRequest.Builder.addAllSurfaces(list: List<Surface>) =
    list.forEach { addTarget(it) }
