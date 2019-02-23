package tech.khana.reflekt.core

import android.hardware.camera2.CameraCaptureSession
import android.view.Surface
import kotlinx.coroutines.channels.BroadcastChannel
import tech.khana.reflekt.utils.Logger
import tech.khana.reflekt.utils.debug

class SessionEventChannel(
    broadcastChannel: BroadcastChannel<SessionState> = BroadcastChannel(64)
) : CameraCaptureSession.StateCallback(),
    BroadcastChannel<SessionState> by broadcastChannel,
    Logger by Logger.defaultLogger {

    override val tag: String = "reflekt-camera-session"

    var state: SessionState = SessionState()
        private set

    override fun onConfigured(session: CameraCaptureSession) {
        debug { "#onConfigured state=$state" }
        state = state.copy(
            session = session,
            error = null
        )
        offer(state)
    }

    override fun onConfigureFailed(session: CameraCaptureSession) {
        debug { "onConfigureFailed state=$state" }
        state = state.copy(
            session = null,
            error = CameraException.CameraSessionConfigurationFailed()
        )
        offer(state)
    }

    override fun onReady(session: CameraCaptureSession) {
        debug { "onReady state=$state" }
        offer(state)
    }

    override fun onActive(session: CameraCaptureSession) {
        debug { "onActive state=$state" }
        offer(state)
    }

    override fun onClosed(session: CameraCaptureSession) {
        debug { "onClosed state=$state" }
        state = state.copy(session = null)
        offer(state)
    }

    override fun onSurfacePrepared(session: CameraCaptureSession, surface: Surface) {
        debug { "onSurfacePrepared state=$state" }
        offer(state)
    }

    override fun onCaptureQueueEmpty(session: CameraCaptureSession) {
        debug { "onCaptureQueueEmpty state=$state" }
        offer(state)
    }
}

data class SessionState(
    val session: CameraCaptureSession? = null,
    val error: Exception? = null
)