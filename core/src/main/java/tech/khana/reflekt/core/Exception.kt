package tech.khana.reflekt.core

import android.hardware.camera2.CameraDevice.StateCallback.*

sealed class CameraException(message: String = "") : Exception(message) {

    class CameraSessionConfigurationFailed :
        CameraException("Session configuration failed")

    class CameraNotOpenedException :
        CameraException("Camera is not yet opened. Please wait.")

    class CameraInUseException :
        CameraException("Camera is already in use by someone high-priority.")

    class CameraMaxCamerasInUseException :
        CameraException("Reached the maximum number of cameras. Close the previous ones.")

    class CameraIsDisabledException :
        CameraException("Camera is disabled by administrator.")

    class CameraUnknownException :
        CameraException("Something went wrong with camera. Try reopen camera")

    class CameraServiceException :
        CameraException("Something went wrong with camera service. Try close all cameras, restart device and reopen camera.")
}

internal fun cameraExceptionByErrorCode(errorCode: Int): CameraException = when (errorCode) {

    ERROR_CAMERA_IN_USE -> CameraException.CameraInUseException()

    ERROR_CAMERA_DEVICE -> CameraException.CameraUnknownException()

    ERROR_CAMERA_DISABLED -> CameraException.CameraIsDisabledException()

    ERROR_CAMERA_SERVICE -> CameraException.CameraServiceException()

    ERROR_MAX_CAMERAS_IN_USE -> CameraException.CameraMaxCamerasInUseException()

    else -> CameraException.CameraUnknownException()
}
