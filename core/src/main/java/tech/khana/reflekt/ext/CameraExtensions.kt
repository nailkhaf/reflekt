package tech.khana.reflekt.ext

import android.hardware.camera2.CaptureRequest
import android.view.Surface
import tech.khana.reflekt.models.CameraMode
import tech.khana.reflekt.models.CameraSurface


internal fun List<CameraSurface>.byType(type: CameraMode): List<Surface> =
    filter { it.type == type }
        .map { it.surface }

internal fun CaptureRequest.Builder.addAllSurfaces(list: List<Surface>) =
    list.forEach { addTarget(it) }
