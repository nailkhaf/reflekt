package tech.khana.reflekt.api

import android.hardware.camera2.CaptureResult

interface ResultPreference {

    fun apply(result: CaptureResult)
}