package tech.khana.reflekt.camera.extensions

import android.Manifest.permission.CAMERA
import android.content.Context
import android.graphics.Rect
import android.hardware.camera2.CameraCharacteristics
import android.hardware.camera2.CameraCharacteristics.*
import android.hardware.camera2.CameraManager
import android.hardware.camera2.CameraMetadata
import android.hardware.camera2.params.StreamConfigurationMap
import android.support.v4.content.ContextCompat.checkSelfPermission
import android.support.v4.content.PermissionChecker.PERMISSION_GRANTED
import android.util.Range
import android.util.Size
import android.view.Surface
import tech.khana.reflekt.api.CameraException
import tech.khana.reflekt.api.models.Lens
import tech.khana.reflekt.api.models.SupportLevel

internal fun Context.requireCameraPermission() {
    if (checkSelfPermission(this, CAMERA) != PERMISSION_GRANTED) {
        throw CameraException.CameraPermissionRequired()
    }
}

internal val Context.cameraManager
    get() = getSystemService(Context.CAMERA_SERVICE) as CameraManager

internal fun CameraManager.findCameraByLens(direct: Lens): String =
    cameraIdList.first { directCamera(it) == direct }

internal fun CameraManager.availableLenses(): List<Lens> =
    cameraIdList.map { directCamera(it) }

internal fun CameraManager.directCamera(cameraId: String): Lens {
    val lensFacing = getCameraCharacteristics(cameraId).get(LENS_FACING)
    return when (lensFacing) {
        CameraMetadata.LENS_FACING_FRONT -> Lens.FRONT
        CameraMetadata.LENS_FACING_BACK -> Lens.BACK
        CameraMetadata.LENS_FACING_EXTERNAL -> Lens.EXTERNAL
        else -> throw IllegalArgumentException("unknown direction")
    }
}

internal fun CameraCharacteristics.outputResolutions(
    klass: Class<out Any>
): List<Size> {
    val map = get(SCALER_STREAM_CONFIGURATION_MAP)!!
    if (StreamConfigurationMap.isOutputSupportedFor(klass).not())
        throw IllegalArgumentException("Class is nut supported: ${klass.simpleName}")
    return map.getOutputSizes(klass).toList()
}

internal fun CameraCharacteristics.outputResolutions(format: Int): List<Size> {
    val map = get(SCALER_STREAM_CONFIGURATION_MAP)!!
    if (map.isOutputSupportedFor(format).not())
        throw IllegalArgumentException("Format is not supported: $format")
    return map.getOutputSizes(format).toList()
}

internal fun CameraCharacteristics.surfaceSupported(surface: Surface): Boolean {
    val map = get(SCALER_STREAM_CONFIGURATION_MAP)!!
    return map.isOutputSupportedFor(surface)
}

internal fun CameraCharacteristics.hardwareOrientation(): Int {
    return get(SENSOR_ORIENTATION)!!
}

internal fun CameraCharacteristics.supportedLevel(): SupportLevel {
    val level = get(INFO_SUPPORTED_HARDWARE_LEVEL)
    return SupportLevel.values().first { it.value == level }
}

fun CameraCharacteristics.isHardwareLevelSupported(
    requiredLevel: SupportLevel
): Boolean {
    val deviceLevel = get(INFO_SUPPORTED_HARDWARE_LEVEL)
        ?: error("can't define hardware level")
    return if (deviceLevel == INFO_SUPPORTED_HARDWARE_LEVEL_LEGACY) {
        requiredLevel.value == deviceLevel
    } else {
        requiredLevel.value <= deviceLevel
    }
}

internal fun CameraCharacteristics.maxNonStallingStreams(): Int =
    get(REQUEST_MAX_NUM_OUTPUT_PROC) ?: 0

internal fun CameraCharacteristics.outputFpsRanges(): Array<Range<Int>> =
    get(CONTROL_AE_AVAILABLE_TARGET_FPS_RANGES) ?: emptyArray()

internal fun CameraCharacteristics.maxStallingStreams(): Int =
    get(REQUEST_MAX_NUM_OUTPUT_PROC_STALLING) ?: 0

internal fun CameraCharacteristics.outputStallDuration(format: Int, size: Size): Long =
    get(SCALER_STREAM_CONFIGURATION_MAP)!!.getOutputStallDuration(format, size)

internal fun CameraCharacteristics.outputStallDuration(
    klass: Class<out Any>, size: Size
): Long = get(SCALER_STREAM_CONFIGURATION_MAP)!!.getOutputStallDuration(klass, size)

internal fun CameraCharacteristics.outputFrameDuration(format: Int, size: Size): Long =
    get(SCALER_STREAM_CONFIGURATION_MAP)!!.getOutputMinFrameDuration(format, size)

internal fun CameraCharacteristics.outputFrameDuration(
    klass: Class<out Any>, size: Size
): Long = get(SCALER_STREAM_CONFIGURATION_MAP)!!.getOutputMinFrameDuration(klass, size)

internal fun CameraCharacteristics.outputFormats(): IntArray =
    get(SCALER_STREAM_CONFIGURATION_MAP)!!.outputFormats

internal fun CameraCharacteristics.availableFlash(): Boolean =
    get(FLASH_INFO_AVAILABLE) ?: error("can't define suppor flash")

internal fun CameraCharacteristics.availableMaxZoom(): Float =
    get(SCALER_AVAILABLE_MAX_DIGITAL_ZOOM) ?: error("can't get max zoom")

internal fun CameraCharacteristics.sensorRect(): Rect =
    get(SENSOR_INFO_ACTIVE_ARRAY_SIZE) ?: error("can't get sensor rect")

internal fun CameraCharacteristics.supportFocus(): Boolean =
    get(CONTROL_AF_AVAILABLE_MODES)?.size ?: 0 > 1

internal fun CameraCharacteristics.supportExposure(): Boolean =
    get(CONTROL_AE_AVAILABLE_MODES)?.size ?: 0 > 1

internal fun CameraCharacteristics.supportAwb(): Boolean =
    get(CONTROL_AWB_AVAILABLE_MODES)?.size ?: 0 > 1

