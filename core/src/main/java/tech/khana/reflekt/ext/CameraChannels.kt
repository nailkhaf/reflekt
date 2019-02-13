package tech.khana.reflekt.ext

import android.hardware.camera2.*
import android.view.Surface
import kotlinx.coroutines.channels.BroadcastChannel
import tech.khana.reflekt.core.cameraExceptionByErrorCode

class CameraEventChannel :
    CameraDevice.StateCallback(),
    BroadcastChannel<CameraEvent> by BroadcastChannel(64) {

    override fun onOpened(camera: CameraDevice) {
        offer(CameraEvent.onOpened(camera))
    }

    override fun onDisconnected(camera: CameraDevice) {
        offer(CameraEvent.onDisconnected)
    }

    override fun onError(camera: CameraDevice, error: Int) {
        offer(CameraEvent.onError(cameraExceptionByErrorCode(error)))
    }

    override fun onClosed(camera: CameraDevice) {
        offer(CameraEvent.onClosed)
    }
}

@Suppress("ClassName")
sealed class CameraEvent {
    class onOpened(val camera: CameraDevice) : CameraEvent()
    object onDisconnected : CameraEvent()
    object onClosed : CameraEvent()
    class onError(exception: Exception) : CameraEvent()
}

class SessionEventChannel :
    CameraCaptureSession.StateCallback(),
    BroadcastChannel<SessionEvent> by BroadcastChannel(64) {

    override fun onReady(session: CameraCaptureSession) {
        offer(SessionEvent.onReady)
    }

    override fun onCaptureQueueEmpty(session: CameraCaptureSession) {
        offer(SessionEvent.onCaptureQueueEmpty)
    }

    override fun onConfigureFailed(session: CameraCaptureSession) {
        offer(SessionEvent.onConfigureFailed)
    }

    override fun onClosed(session: CameraCaptureSession) {
        offer(SessionEvent.onClosed)
    }

    override fun onSurfacePrepared(session: CameraCaptureSession, surface: Surface) {
        offer(SessionEvent.onSurfacePrepared(surface))
    }

    override fun onConfigured(session: CameraCaptureSession) {
        offer(SessionEvent.onConfigured(session))
    }

    override fun onActive(session: CameraCaptureSession) {
        offer(SessionEvent.onActive)
    }
}

@Suppress("ClassName")
sealed class SessionEvent {
    class onConfigured(val session: CameraCaptureSession) : SessionEvent()
    object onConfigureFailed : SessionEvent()
    object onReady : SessionEvent()
    object onActive : SessionEvent()
    object onClosed : SessionEvent()
    class onSurfacePrepared(val surface: Surface) : SessionEvent()
    object onCaptureQueueEmpty : SessionEvent()
}

class CaptureStateChannel :
    CameraCaptureSession.CaptureCallback(),
    BroadcastChannel<CaptureEvent> by BroadcastChannel(64) {

    override fun onCaptureStarted(
        session: CameraCaptureSession,
        request: CaptureRequest,
        timestamp: Long,
        frameNumber: Long
    ) {
        offer(CaptureEvent.onCaptureStarted(request, timestamp, frameNumber))
    }

    override fun onCaptureProgressed(
        session: CameraCaptureSession,
        request: CaptureRequest,
        partialResult: CaptureResult
    ) {
        offer(CaptureEvent.onCaptureProgressed(request, partialResult))
    }

    override fun onCaptureCompleted(
        session: CameraCaptureSession,
        request: CaptureRequest,
        result: TotalCaptureResult
    ) {
        offer(CaptureEvent.onCaptureCompleted(request, result))
    }

    override fun onCaptureFailed(session: CameraCaptureSession, request: CaptureRequest, failure: CaptureFailure) {
        offer(CaptureEvent.onCaptureFailed(request, failure))
    }

    override fun onCaptureBufferLost(
        session: CameraCaptureSession,
        request: CaptureRequest,
        target: Surface,
        frameNumber: Long
    ) {
        offer(CaptureEvent.onCaptureBufferLost(request, target, frameNumber))
    }

    override fun onCaptureSequenceCompleted(session: CameraCaptureSession, sequenceId: Int, frameNumber: Long) {
        offer(CaptureEvent.onCaptureSequenceCompleted(sequenceId, frameNumber))
    }

    override fun onCaptureSequenceAborted(session: CameraCaptureSession, sequenceId: Int) {
        offer(CaptureEvent.onCaptureSequenceAborted(sequenceId))
    }
}

@Suppress("ClassName")
sealed class CaptureEvent {

    class onCaptureStarted(val request: CaptureRequest, val timestamp: Long, val frameNumber: Long) : CaptureEvent()
    class onCaptureProgressed(val request: CaptureRequest, val partialResult: CaptureResult) : CaptureEvent()
    class onCaptureCompleted(val request: CaptureRequest, val result: TotalCaptureResult) : CaptureEvent()
    class onCaptureFailed(val request: CaptureRequest, val failure: CaptureFailure) : CaptureEvent()
    class onCaptureBufferLost(val request: CaptureRequest, val target: Surface, val frameNumber: Long) : CaptureEvent()
    class onCaptureSequenceCompleted(val sequenceId: Int, val frameNumber: Long) : CaptureEvent()
    class onCaptureSequenceAborted(val sequenceId: Int) : CaptureEvent()
}