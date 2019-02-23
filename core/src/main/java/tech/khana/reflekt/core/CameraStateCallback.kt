package tech.khana.reflekt.core

import android.hardware.camera2.CameraDevice
import kotlinx.coroutines.channels.BroadcastChannel
import tech.khana.reflekt.utils.Logger
import tech.khana.reflekt.utils.debug

class CameraEventChannel(
    broadcastChannel: BroadcastChannel<CameraState> = BroadcastChannel(64)
) : CameraDevice.StateCallback(),
    BroadcastChannel<CameraState> by broadcastChannel,
    Logger by Logger.defaultLogger {

    override val tag: String = "reflekt-camera-device"

    private var state: CameraState = CameraState()

    override fun onOpened(camera: CameraDevice) {
        debug { "#onOpened state=$state" }
        state = state.copy(
            camera = camera,
            error = null
        )
        offer(state)
    }

    override fun onDisconnected(camera: CameraDevice) {
        debug { "#onDisconnected state=$state" }
        camera.close()
        state = state.copy(camera = null)
        offer(state)
    }

    override fun onError(camera: CameraDevice, error: Int) {
        debug { "#onError state=$state" }
        camera.close()
        state = state.copy(
            camera = null,
            error = cameraExceptionByErrorCode(error)
        )
        offer(state)
    }

    override fun onClosed(camera: CameraDevice) {
        debug { "#onClosed state=$state" }
        camera.close()
        state = state.copy(camera = null)
        offer(state)
    }
}

data class CameraState(
    val camera: CameraDevice? = null,
    val error: Exception? = null
)

