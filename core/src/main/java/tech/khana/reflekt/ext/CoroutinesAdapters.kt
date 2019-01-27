package tech.khana.reflekt.ext

import android.hardware.camera2.CameraCaptureSession
import android.hardware.camera2.CameraDevice
import android.hardware.camera2.CaptureRequest
import android.os.Handler
import android.os.HandlerThread
import android.view.Surface
import tech.khana.reflekt.core.CameraException
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException
import kotlin.coroutines.suspendCoroutine


internal suspend fun CameraDevice.createCaptureSession(
    surfaces: List<Surface>,
    handlerThread: HandlerThread
) = suspendCoroutine<CameraCaptureSession> { continuation ->

    createCaptureSession(
        surfaces, object : CameraCaptureSession.StateCallback() {

            override fun onConfigured(session: CameraCaptureSession) {
                continuation.resume(session)
            }

            override fun onConfigureFailed(session: CameraCaptureSession) {
                continuation.resumeWithException(CameraException.CameraSessionConfigurationFailed())
            }
        }, Handler(handlerThread.looper)
    )
}

internal suspend fun CameraCaptureSession.setRepeatingRequest(
    request: CaptureRequest,
    handlerThread: HandlerThread? = null
) = suspendCoroutine<Unit> { continuation ->

    setRepeatingRequest(request, null,
        handlerThread?.looper?.let { Handler(it) })

    continuation.resume(Unit)
}

internal suspend fun CameraCaptureSession.capture(
    request: CaptureRequest,
    handlerThread: HandlerThread? = null
) = suspendCoroutine<Unit> { continuation ->

    capture(request, null,
        handlerThread?.looper?.let { Handler(it) })

    continuation.resume(Unit)
}
