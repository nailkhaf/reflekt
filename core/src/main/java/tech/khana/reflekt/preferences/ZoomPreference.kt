package tech.khana.reflekt.preferences

import android.graphics.Rect
import android.hardware.camera2.CaptureRequest
import tech.khana.reflekt.core.CameraPreference
import tech.khana.reflekt.models.CameraMode
import tech.khana.reflekt.models.ZoomMode

object ZoomPreference : CameraPreference {

    var zoomLevel: Float = 1f
    var sensorRect: Rect = Rect()
    var zoomMode: ZoomMode = ZoomMode.OFF

    override fun CaptureRequest.Builder.apply(cameraMode: CameraMode) = when {

        cameraMode == CameraMode.PREVIEW && zoomLevel != 1f && zoomMode == ZoomMode.ON -> {
            val croppedRect = calculateCroppedRect(sensorRect, zoomLevel)
            set(CaptureRequest.SCALER_CROP_REGION, croppedRect)
        }

        else -> {
            // nothing
        }
    }
}

internal fun calculateCroppedRect(sensorRect: Rect, zoomLevel: Float): Rect {
    val width = Math.floor(sensorRect.width().toDouble() / zoomLevel).toInt()
    val left = (sensorRect.width() - width) / 2
    val height = Math.floor(sensorRect.height().toDouble() / zoomLevel).toInt()
    val top = (sensorRect.height() - height) / 2
    return Rect(left, top, left + width, top + height)
}
