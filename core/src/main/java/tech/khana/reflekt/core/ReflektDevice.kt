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
        cameraLogger.debug { "device #open" }
        currentSession = cameraDevice.createCaptureSession(surfaces.all, handlerThread)
    }

    override suspend fun startPreview() = coroutineScope {
        cameraLogger.debug { "device #startPreview" }
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
        cameraLogger.debug { "device #stopPreview" }
        val session = currentSession
        check(session != null) { "session is not started" }
        session.abortCaptures()
        session.stopRepeating()
    }

    override suspend fun capture() = coroutineScope {
        cameraLogger.debug { "device #capture" }
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
        cameraLogger.debug { "device #release" }
        currentSession?.stopRepeating()
        currentSession?.abortCaptures()
        currentSession?.close()
        cameraDevice.close()
        surfaces.forEach { it.surface.release() }
    }
}

private val List<TypedSurface>.all: List<Surface>
    get() = map { it.surface }