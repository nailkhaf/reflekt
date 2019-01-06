package tech.khana.reflekt.core

import android.hardware.camera2.CameraCaptureSession
import android.hardware.camera2.CameraDevice
import android.os.HandlerThread
import android.view.Surface
import kotlinx.coroutines.coroutineScope
import tech.khana.reflekt.core.SurfaceType.CAPTURE
import tech.khana.reflekt.core.SurfaceType.PREVIEW

internal class ReflektDeviceImpl(
    private val cameraDevice: CameraDevice,
    private val requestFactory: RequestFactory,
    private val surfaces: List<TypedSurface> = emptyList(),
    private val handlerThread: HandlerThread = HandlerThread("").apply { start() }
) : ReflektDevice {

    private var currentSession: CameraCaptureSession? = null

    override suspend fun open() = coroutineScope {
        currentSession = cameraDevice.createCaptureSession(surfaces.all, handlerThread)
    }

    override suspend fun startPreview() = coroutineScope {
        val session = currentSession
        check(session != null) { "session is not started" }
        with(requestFactory) {
            val previewRequest = cameraDevice.createPreviewRequest {
                addAllSurfaces(surfaces.byType(PREVIEW))
            }
            session.setRepeatingRequest(previewRequest)
        }
    }

    override suspend fun stopPreview() = coroutineScope {
        val session = currentSession
        check(session != null) { "session is not started" }
        session.abortCaptures()
        session.stopRepeating()
    }

    override suspend fun capture() = coroutineScope {
        val session = currentSession
        check(session != null) { "session is not started" }
        session.abortCaptures()
        with(requestFactory) {
            val previewRequest = cameraDevice.createStillRequest {
                addAllSurfaces(surfaces.byType(CAPTURE))
            }
            session.capture(previewRequest)
        }
    }

    override suspend fun release() = coroutineScope {
        cameraDevice.close()
        surfaces.forEach { it.surface.release() }
    }
}

private val List<TypedSurface>.all: List<Surface>
    get() = map { it.surface }