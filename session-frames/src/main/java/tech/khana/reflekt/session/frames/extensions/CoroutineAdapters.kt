package tech.khana.reflekt.session.frames.extensions

import android.hardware.camera2.*
import android.os.Handler
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.channels.ReceiveChannel
import tech.khana.reflekt.api.SessionException

fun CameraCaptureSession.repeatingRequestChannel(
    request: CaptureRequest,
    handler: Handler
): ReceiveChannel<CaptureResult> = Channel<CaptureResult>().also { channel ->

    setRepeatingRequest(request, object : CameraCaptureSession.CaptureCallback() {

        override fun onCaptureCompleted(
            session: CameraCaptureSession,
            request: CaptureRequest,
            result: TotalCaptureResult
        ) {
            channel.offer(result)
        }

        override fun onCaptureFailed(session: CameraCaptureSession, request: CaptureRequest, failure: CaptureFailure) {
            super.onCaptureFailed(session, request, failure)
            channel.close(SessionException.CaptureFailedException(request.tag as? String))
        }
    }, handler)
}