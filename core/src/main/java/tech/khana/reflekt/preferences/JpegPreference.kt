package tech.khana.reflekt.preferences

import android.hardware.camera2.CaptureRequest
import android.location.Location
import tech.khana.reflekt.core.CameraPreference
import tech.khana.reflekt.models.CameraMode
import tech.khana.reflekt.models.LensDirect
import tech.khana.reflekt.models.Rotation
import tech.khana.reflekt.models.rotationOf


object JpegPreference : CameraPreference {

    var hardwareRotation: Rotation = Rotation._0

    var displayRotation: Rotation = Rotation._0

    var lensDirect: LensDirect = LensDirect.FRONT

    var location: Location? = null

    override fun CaptureRequest.Builder.apply(cameraMode: CameraMode) = when (cameraMode) {
        CameraMode.CAPTURE -> {
            val jpegOrientation = getJpegOrientation(lensDirect, hardwareRotation, displayRotation)
            location?.let { set(CaptureRequest.JPEG_GPS_LOCATION, it) }
            set(CaptureRequest.JPEG_ORIENTATION, jpegOrientation.value)
            set(CaptureRequest.JPEG_QUALITY, 95)
        }
        else -> { // nothing
        }
    }

    fun getJpegOrientation(lensDirect: LensDirect, hardwareRotation: Rotation, displayRotation: Rotation): Rotation {
        var deviceOrientation = displayRotation.value
        val sensorOrientation = hardwareRotation.value

        deviceOrientation = (deviceOrientation + 45) / 90 * 90

        if (lensDirect == LensDirect.FRONT) deviceOrientation = -deviceOrientation

        return rotationOf((sensorOrientation + deviceOrientation + 360) % 360)
    }
}