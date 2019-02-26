package tech.khana.reflekt.api.models

import android.annotation.TargetApi
import android.hardware.camera2.CameraMetadata.*
import android.os.Build

enum class SupportLevel(val value: Int) {

    LEGACY(INFO_SUPPORTED_HARDWARE_LEVEL_LEGACY),

    LIMITED(INFO_SUPPORTED_HARDWARE_LEVEL_LIMITED),

    FULL(INFO_SUPPORTED_HARDWARE_LEVEL_FULL),

    @TargetApi(Build.VERSION_CODES.N)
    LEVEL_3(INFO_SUPPORTED_HARDWARE_LEVEL_3),

    @TargetApi(Build.VERSION_CODES.P)
    EXTERNAL(INFO_SUPPORTED_HARDWARE_LEVEL_EXTERNAL)
}