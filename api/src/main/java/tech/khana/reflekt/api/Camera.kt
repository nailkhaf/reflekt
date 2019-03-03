package tech.khana.reflekt.api

import android.hardware.camera2.CameraDevice
import android.view.Surface
import tech.khana.reflekt.api.models.CameraConfig
import tech.khana.reflekt.api.models.CameraMode

abstract class Camera : AutoCloseable, CameraDevice.StateCallback() {

    abstract val id: String

    abstract suspend fun start(cameraConfig: CameraConfig)

    protected fun List<Pair<Surface, Set<CameraMode>>>.groupByMode() =
        flatMap { (surface, modes) -> modes.map { it to surface } }
            .groupBy(keySelector = { it.first }, valueTransform = { it.second })
}

fun <T> List<Pair<T, Set<CameraMode>>>.groupByCameraMode() =
    flatMap { (t, modes) -> modes.map { it to t } }
        .groupBy(keySelector = { it.first }, valueTransform = { it.second })