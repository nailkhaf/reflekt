package tech.khana.reflekt.ext

import android.hardware.camera2.*
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
    request: CaptureRequest
) = suspendCoroutine<Unit> { continuation ->

    setRepeatingRequest(request, null, null)

    continuation.resume(Unit)
}

internal suspend fun CameraCaptureSession.capture(
    request: CaptureRequest,
    handlerThread: HandlerThread
) = suspendCoroutine<Unit> { continuation ->

    capture(request, object : CameraCaptureSession.CaptureCallback() {

        override fun onCaptureCompleted(
            session: CameraCaptureSession,
            request: CaptureRequest,
            result: TotalCaptureResult
        ) {
            continuation.resume(Unit)
        }

        override fun onCaptureFailed(
            session: CameraCaptureSession,
            request: CaptureRequest,
            failure: CaptureFailure
        ) {
            continuation.resumeWithException(IllegalStateException())
        }
    }, Handler(handlerThread.looper))
}

internal suspend fun CameraCaptureSession.lockFocus(
    handlerThread: HandlerThread, surfaces: List<Surface>
) = suspendCoroutine<Int> { continuation ->
    val request = device.createCaptureRequest(CameraDevice.TEMPLATE_PREVIEW).run {
        set(CaptureRequest.CONTROL_AF_TRIGGER, CaptureRequest.CONTROL_AF_TRIGGER_START)
        addAllSurfaces(surfaces)
        build()
    }
    capture(request, object : CameraCaptureSession.CaptureCallback() {

        override fun onCaptureCompleted(
            session: CameraCaptureSession,
            request: CaptureRequest,
            result: TotalCaptureResult
        ) {
            continuation.resume(result.get(CaptureResult.CONTROL_AF_STATE))
        }

        override fun onCaptureFailed(
            session: CameraCaptureSession,
            request: CaptureRequest,
            failure: CaptureFailure
        ) {
            continuation.resumeWithException(IllegalStateException())
        }
    }, Handler(handlerThread.looper))
}

internal suspend fun CameraCaptureSession.preCaptureExposure(
    handlerThread: HandlerThread, surfaces: List<Surface>
) = suspendCoroutine<Int> { continuation ->
    val request = device.createCaptureRequest(CameraDevice.TEMPLATE_PREVIEW).run {
        addAllSurfaces(surfaces)
        set(CaptureRequest.CONTROL_AE_PRECAPTURE_TRIGGER, CaptureRequest.CONTROL_AE_PRECAPTURE_TRIGGER_START)
        build()
    }
    capture(request, object : CameraCaptureSession.CaptureCallback() {

        override fun onCaptureCompleted(
            session: CameraCaptureSession,
            request: CaptureRequest,
            result: TotalCaptureResult
        ) {
            continuation.resume(result.get(CaptureResult.CONTROL_AE_STATE))
        }

        override fun onCaptureFailed(
            session: CameraCaptureSession,
            request: CaptureRequest,
            failure: CaptureFailure
        ) {
            continuation.resumeWithException(IllegalStateException())
        }
    }, Handler(handlerThread.looper))
}

internal suspend fun CameraCaptureSession.unLockFocus(
    handlerThread: HandlerThread, surfaces: List<Surface>
) = suspendCoroutine<Unit> { continuation ->
    val request = device.createCaptureRequest(CameraDevice.TEMPLATE_PREVIEW).run {
        addAllSurfaces(surfaces)
        set(CaptureRequest.CONTROL_AF_TRIGGER, CaptureRequest.CONTROL_AF_TRIGGER_CANCEL)
        build()
    }
    capture(request, object : CameraCaptureSession.CaptureCallback() {

        override fun onCaptureCompleted(
            session: CameraCaptureSession,
            request: CaptureRequest,
            result: TotalCaptureResult
        ) {
            continuation.resume(Unit)
        }

        override fun onCaptureFailed(
            session: CameraCaptureSession,
            request: CaptureRequest,
            failure: CaptureFailure
        ) {
            continuation.resumeWithException(IllegalStateException())
        }
    }, Handler(handlerThread.looper))
}
