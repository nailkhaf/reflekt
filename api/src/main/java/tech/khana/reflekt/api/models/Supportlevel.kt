package tech.khana.reflekt.api.models

import android.hardware.camera2.CameraMetadata.*
import android.os.Build

enum class SupportLevel {
    LEGACY,
    LIMITED,
    FULL,
    LEVEL3,
    EXTERNAL
}

fun SupportLevel.isLevelSupported(requiredLevel: SupportLevel): Boolean =
    ordinal >= requiredLevel.ordinal

fun supportLevelOf(value: Int): SupportLevel = when {
    value == INFO_SUPPORTED_HARDWARE_LEVEL_LEGACY -> SupportLevel.LEGACY
    value == INFO_SUPPORTED_HARDWARE_LEVEL_LIMITED -> SupportLevel.LIMITED
    value == INFO_SUPPORTED_HARDWARE_LEVEL_FULL -> SupportLevel.FULL
    Build.VERSION.SDK_INT >= Build.VERSION_CODES.N && value == INFO_SUPPORTED_HARDWARE_LEVEL_3 ->
        SupportLevel.LEVEL3
    Build.VERSION.SDK_INT >= Build.VERSION_CODES.O && value == INFO_SUPPORTED_HARDWARE_LEVEL_EXTERNAL ->
        SupportLevel.EXTERNAL
    else -> error("unknown support level")
}