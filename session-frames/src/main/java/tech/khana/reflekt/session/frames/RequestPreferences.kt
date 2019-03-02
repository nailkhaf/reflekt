package tech.khana.reflekt.session.frames

import android.hardware.camera2.CaptureRequest
import android.hardware.camera2.CaptureRequest.*
import android.util.Range
import tech.khana.reflekt.api.RequestPreference

internal class FrameProcessorPreviewPreference : RequestPreference {

    override fun apply(builder: CaptureRequest.Builder) = builder.run {

        set(CONTROL_AF_MODE, CONTROL_AF_MODE_CONTINUOUS_PICTURE)
        set(CONTROL_AE_MODE, CONTROL_AE_MODE_ON)
        set(CONTROL_AWB_MODE, CONTROL_AWB_MODE_AUTO)

        set(CONTROL_AE_ANTIBANDING_MODE, CONTROL_AE_ANTIBANDING_MODE_AUTO)

        set(CONTROL_AE_TARGET_FPS_RANGE, Range.create(30, 30))
    }
}

internal interface FlashPreference : RequestPreference

internal class FrameProcessorFlashTorchPreference : FlashPreference {

    override fun apply(builder: CaptureRequest.Builder) = builder.run {
        set(FLASH_MODE, FLASH_MODE_TORCH)
    }
}

internal class FrameProcessorFlashOffPreference : FlashPreference {

    override fun apply(builder: CaptureRequest.Builder) = builder.run {
        set(FLASH_MODE, FLASH_MODE_OFF)
    }
}
