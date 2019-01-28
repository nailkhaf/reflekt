package tech.khana.reflekt.preferences

import android.hardware.camera2.CaptureRequest
import tech.khana.reflekt.core.CameraPreference
import tech.khana.reflekt.models.CameraMode
import tech.khana.reflekt.models.LensDirect
import tech.khana.reflekt.models.Rotation


object JpegPreference : CameraPreference {

    var hardwareRotation: Rotation = Rotation._0

    var displayRotation: Rotation = Rotation._0

    var lensDirect: LensDirect = LensDirect.FRONT

    override fun CaptureRequest.Builder.apply(cameraMode: CameraMode) = when {
        cameraMode == CameraMode.CAPTURE -> {
            val jpegOrientation = getJpegOrientation(lensDirect, hardwareRotation, displayRotation)
            set(CaptureRequest.JPEG_ORIENTATION, jpegOrientation)
        }
        else -> {
            // nothing
        }
    }

    private fun getJpegOrientation(lensDirect: LensDirect, hardwareRotation: Rotation, displayRotation: Rotation): Int {
        var deviceOrientation = displayRotation.value
        val sensorOrientation = hardwareRotation.value

        // Round device orientation to a multiple of 90
        deviceOrientation = (deviceOrientation + 45) / 90 * 90

        // Reverse device orientation for front-facing cameras
        if (lensDirect == LensDirect.FRONT) deviceOrientation = -deviceOrientation

        // Calculate desired JPEG orientation relative to camera orientation to make
        // the image upright relative to the device orientation

        return (sensorOrientation + deviceOrientation + 360) % 360
    }
}