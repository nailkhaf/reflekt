package tech.khana.reflekt.ext

import android.hardware.camera2.CaptureRequest
import android.view.Surface

internal fun CaptureRequest.Builder.addAllSurfaces(list: List<Surface>) =
    list.forEach { addTarget(it) }
