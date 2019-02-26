package tech.khana.reflekt.camera

import android.hardware.camera2.CameraManager
import android.os.Handler
import kotlinx.coroutines.CoroutineScope
import tech.khana.reflekt.api.Camera
import tech.khana.reflekt.api.SessionFactory

class CameraFactory(
    private val cameraManager: CameraManager,
    private val coroutineScope: CoroutineScope,
    private val sessionFactory: SessionFactory,
    private val handler: Handler
) {

    operator fun invoke(): Camera = CameraImpl(
        scope = coroutineScope,
        cameraManager = cameraManager,
        sessionFactory = sessionFactory,
        handler = handler
    )
}
