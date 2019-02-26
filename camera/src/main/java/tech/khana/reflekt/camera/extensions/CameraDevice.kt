package tech.khana.reflekt.camera.extensions

import android.hardware.camera2.CameraDevice
import android.media.CamcorderProfile
import android.util.Size

internal fun CameraDevice.maxRecordSize() = CamcorderProfile.get(id.toInt(), CamcorderProfile.QUALITY_HIGH).let {
    Size(it.videoFrameWidth, it.videoFrameHeight)
}