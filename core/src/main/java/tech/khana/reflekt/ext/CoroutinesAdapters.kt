package tech.khana.reflekt.ext

import android.hardware.camera2.*
import android.hardware.camera2.CaptureResult.*
import android.os.Build
import android.os.Handler
import android.os.HandlerThread
import android.view.Surface
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.launch
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
    handlerThread: HandlerThread? = null,
    callback: CameraCaptureSession.CaptureCallback? = null
) {

    setRepeatingRequest(request, callback, handlerThread?.looper?.let { Handler(it) })
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

internal suspend fun CameraCaptureSession.trigger3A(
    scope: CoroutineScope, requestBuilder: CaptureRequest.Builder, handlerThread: HandlerThread
) = Channel<Pair<CaptureResult.Key<Int>, Int>>(Channel.UNLIMITED).also { channel ->

    val request = requestBuilder.run {
        set(CaptureRequest.CONTROL_AF_TRIGGER, CaptureRequest.CONTROL_AF_TRIGGER_START)
        set(CaptureRequest.CONTROL_AE_PRECAPTURE_TRIGGER, CaptureRequest.CONTROL_AE_PRECAPTURE_TRIGGER_START)
        build()
    }

    capture(request, object : CameraCaptureSession.CaptureCallback() {

        override fun onCaptureProgressed(
            session: CameraCaptureSession,
            request: CaptureRequest,
            partialResult: CaptureResult
        ) {
            scope.launch {
                partialResult.get(CONTROL_AF_STATE)?.let { channel.send(CONTROL_AF_STATE to it) }
                partialResult.get(CONTROL_AE_STATE)?.let { channel.send(CONTROL_AE_STATE to it) }
                partialResult.get(CONTROL_AWB_STATE)?.let { channel.send(CONTROL_AWB_STATE to it) }
            }
        }

        override fun onCaptureCompleted(
            session: CameraCaptureSession,
            request: CaptureRequest,
            result: TotalCaptureResult
        ) {
            scope.launch {
                result.get(CONTROL_AF_STATE)?.let { channel.send(CONTROL_AF_STATE to it) }
                result.get(CONTROL_AE_STATE)?.let { channel.send(CONTROL_AE_STATE to it) }
                result.get(CONTROL_AWB_STATE)?.let { channel.send(CONTROL_AWB_STATE to it) }
                channel.close()
            }
        }

        override fun onCaptureFailed(
            session: CameraCaptureSession,
            request: CaptureRequest,
            failure: CaptureFailure
        ) {
            channel.close(IllegalStateException())
        }
    }, Handler(handlerThread.looper))
}

internal suspend fun CameraCaptureSession.lock3A(
    requestBuilder: CaptureRequest.Builder, handlerThread: HandlerThread
) = suspendCoroutine<Unit> { continuation ->

    val request = requestBuilder.run {
        set(CaptureRequest.CONTROL_AE_LOCK, true)
        set(CaptureRequest.CONTROL_AWB_LOCK, true)
        build()
    }

    capture(request,
        object : CameraCaptureSession.CaptureCallback() {
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
        }
        , Handler(handlerThread.looper)
    )
}

internal suspend fun CameraCaptureSession.unlock3A(
    requestBuilder: CaptureRequest.Builder
) {

    val request = requestBuilder.run {
        CaptureResult.FLASH_STATE
        set(CaptureRequest.CONTROL_AE_LOCK, false)
        set(CaptureRequest.CONTROL_AWB_LOCK, false)
        set(CaptureRequest.CONTROL_AF_TRIGGER, CaptureRequest.CONTROL_AF_TRIGGER_CANCEL)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            set(CaptureRequest.CONTROL_AE_PRECAPTURE_TRIGGER, CaptureRequest.CONTROL_AE_PRECAPTURE_TRIGGER_CANCEL)
        }
        build()
    }

    capture(request, null, null)
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
            continuation.resume(result.get(CONTROL_AF_STATE))
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
            continuation.resume(result.get(CONTROL_AE_STATE))
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
