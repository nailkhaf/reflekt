package tech.khana.reflekt.session.frames.extensions

import android.hardware.camera2.CameraCaptureSession
import android.hardware.camera2.CaptureRequest
import android.hardware.camera2.CaptureResult
import android.hardware.camera2.TotalCaptureResult
import android.os.Handler
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.channels.ReceiveChannel
import tech.khana.reflekt.api.SessionException

fun CameraCaptureSession.repeatingRequestChannel(
    request: CaptureRequest,
    handler: Handler
): ReceiveChannel<CaptureResult> = Channel<CaptureResult>().also { channel ->

    var id: Int = -1

    id = setRepeatingRequest(request, object : CameraCaptureSession.CaptureCallback() {

        override fun onCaptureCompleted(
            session: CameraCaptureSession,
            request: CaptureRequest,
            result: TotalCaptureResult
        ) {
            channel.offer(result)
        }

        override fun onCaptureSequenceCompleted(session: CameraCaptureSession, sequenceId: Int, frameNumber: Long) {
            if (sequenceId == id) {
                channel.close()
            }
        }

        override fun onCaptureSequenceAborted(session: CameraCaptureSession, sequenceId: Int) {
            if (id == sequenceId) {
                channel.close(SessionException.CaptureFailedException())
            }
        }

    }, handler)
}
