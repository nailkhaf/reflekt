package tech.khana.reflekt.models

import android.hardware.camera2.CameraMetadata


enum class LensDirect {
    FRONT,
    BACK
}

fun LensDirect.switch() = when (this) {
    LensDirect.BACK -> LensDirect.FRONT
    LensDirect.FRONT -> LensDirect.BACK
}

enum class FlashMode {
    AUTO,
    ON,
    OFF,
    TORCH
}

enum class ZoomMode {
    ON,
    OFF
}

enum class CameraMode {
    PREVIEW,
    CAPTURE,
    RECORD
}


enum class SupportLevel(val value: Int, val description: String) {
    LEGACY(CameraMetadata.INFO_SUPPORTED_HARDWARE_LEVEL_LEGACY, "Legacy support"),
    LIMIT(CameraMetadata.INFO_SUPPORTED_HARDWARE_LEVEL_LIMITED, "Limited support"),
    EXTERNAL(
        CameraMetadata.INFO_SUPPORTED_HARDWARE_LEVEL_EXTERNAL,
        "External support, like limited"
    ),
    FULL(CameraMetadata.INFO_SUPPORTED_HARDWARE_LEVEL_FULL, "Full support"),
    LEVEL_3(CameraMetadata.INFO_SUPPORTED_HARDWARE_LEVEL_3, "Level 3 support")
}
