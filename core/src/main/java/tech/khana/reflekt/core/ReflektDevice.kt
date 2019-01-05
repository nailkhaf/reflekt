package tech.khana.reflekt.core

import android.hardware.camera2.CameraCaptureSession
import android.hardware.camera2.CameraDevice
import android.os.Handler
import android.os.HandlerThread
import android.view.Surface
import kotlinx.coroutines.android.asCoroutineDispatcher
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.withContext

internal class ReflektDeviceImpl(
    private val cameraDevice: CameraDevice,
    private val requestFactory: RequestFactory,
    private val surfaces: List<TypedSurface> = emptyList(),
    private val handlerThread: HandlerThread = HandlerThread("").apply { start() }
) : ReflektDevice {

    private val cameraDispatcher = Handler(handlerThread.looper).asCoroutineDispatcher("")

    private var currentSession: CameraCaptureSession? = null

    override suspend fun open() = coroutineScope {
        withContext(cameraDispatcher) {
            currentSession = cameraDevice.createCaptureSession(surfaces.all, handlerThread)
        }
    }

    override suspend fun startPreview() = coroutineScope {
        val session = currentSession
        check(session != null) { "session is not started" }
        session.setRepeatingRequest(requestFactory.createRequest(SurfaceType.PREVIEW, cameraDevice))
        Unit
    }

    override suspend fun stopPreview() = coroutineScope {
        val session = currentSession
        check(session != null) { "session is not started" }
        session.stopRepeating()
    }

    override suspend fun capture() = coroutineScope {
        val session = currentSession
        check(session != null) { "session is not started" }
        stopPreview()
        session.abortCaptures()
        session.capture(requestFactory.createRequest(SurfaceType.CAPTURE, cameraDevice))
        startPreview()
        Unit
    }

    override suspend fun flash(mode: FlashMode) = coroutineScope {
        throw NotImplementedError()
    }

    override suspend fun zoom(float: Float) = coroutineScope {
        throw NotImplementedError()
    }

    override suspend fun release() = coroutineScope {
        withContext(cameraDispatcher) {
            cameraDevice.close()
            surfaces.forEach { it.surface.release() }
        }
    }
}

private val List<TypedSurface>.all: List<Surface>
    get() = map { it.surface }