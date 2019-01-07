package tech.khana.reflekt.core

import android.graphics.Color
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
    override val cameraId: String,
    private val handlerThread: HandlerThread = HandlerThread("").apply { start() }
) : ReflektDevice {

    private var currentSession: CameraCaptureSession? = null
    private var currentSurfaces: List<TypedSurface>? = null

    override suspend fun startSession(surfaces: List<TypedSurface>) = coroutineScope {
        cameraLogger.debug { "device #open" }
        this@ReflektDeviceImpl.currentSurfaces = surfaces
        currentSession = cameraDevice.createCaptureSession(surfaces.all, handlerThread)
    }

    override suspend fun startPreview() = coroutineScope {
        cameraLogger.debug { "device #startPreview" }
        val session = currentSession
        val surfaces = currentSurfaces
        check(session != null) { "session is not started" }
        check(surfaces != null) { "surfaces is null" }

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
        val surfaces = currentSurfaces
        check(session != null) { "session is not started" }
        check(surfaces != null) { "surfaces is null" }

        session.abortCaptures()
        with(requestFactory) {
            val previewRequest = cameraDevice.createStillRequest {
                addAllSurfaces(surfaces.byType(CAPTURE))
            }
            session.capture(previewRequest)
        }
    }

    override suspend fun stopSession() = coroutineScope {
        currentSession?.stopRepeating()
        currentSession?.abortCaptures()
        currentSession?.close()
        currentSurfaces?.map { it.surface.release() }
        currentSession = null
        currentSurfaces = null
    }

    override suspend fun release() = coroutineScope {
        cameraLogger.debug { "device #release" }
        check(currentSession == null) { "session is not stopped" }
        check(currentSurfaces == null) { "surfaces is not released" }

        cameraDevice.close()
    }
}

private val List<TypedSurface>.all: List<Surface>
    get() = map { it.surface }