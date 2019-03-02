package tech.khana.reflekt.api

import android.hardware.camera2.CaptureRequest

interface RequestPreference {

    fun apply(builder: CaptureRequest.Builder)
}

