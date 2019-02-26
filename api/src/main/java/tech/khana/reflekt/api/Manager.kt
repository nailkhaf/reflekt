package tech.khana.reflekt.api

import android.hardware.camera2.CameraManager
import tech.khana.reflekt.api.models.Lens
import tech.khana.reflekt.api.models.Lens.FRONT

abstract class Manager : CameraManager.AvailabilityCallback() {

    abstract suspend fun open(lens: Lens = FRONT)

    abstract fun release()
}