package tech.khana.reflekt.preferences

import android.hardware.camera2.CaptureRequest
import tech.khana.reflekt.core.CameraPreference
import tech.khana.reflekt.models.CameraMode
import tech.khana.reflekt.models.FlashMode

internal object FlashPreference : CameraPreference {

    var flashMode: FlashMode = FlashMode.AUTO

    override fun CaptureRequest.Builder.apply(cameraMode: CameraMode) = when {

        cameraMode == CameraMode.PREVIEW && flashMode == FlashMode.TORCH ->
            set(CaptureRequest.FLASH_MODE, CaptureRequest.FLASH_MODE_TORCH)

        cameraMode == CameraMode.PREVIEW ->
            set(CaptureRequest.FLASH_MODE, CaptureRequest.FLASH_MODE_OFF)

        else -> {
            // nothing
        }
    }
}