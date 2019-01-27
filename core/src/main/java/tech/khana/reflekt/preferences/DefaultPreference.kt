package tech.khana.reflekt.preferences

import android.hardware.camera2.CaptureRequest
import tech.khana.reflekt.core.CameraPreference
import tech.khana.reflekt.models.CameraMode

internal object DefaultPreference : CameraPreference {

    override fun CaptureRequest.Builder.apply(cameraMode: CameraMode) {
    }
}