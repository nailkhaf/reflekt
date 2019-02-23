package tech.khana.reflekt.core

import android.hardware.camera2.CameraCaptureSession
import android.hardware.camera2.CaptureRequest
import android.hardware.camera2.TotalCaptureResult
import kotlinx.coroutines.channels.BroadcastChannel
import tech.khana.reflekt.utils.Logger

class CaptureStateCallback(
    broadcastChannel: BroadcastChannel<CaptureState>
) : CameraCaptureSession.CaptureCallback(),
    BroadcastChannel<CaptureState> by broadcastChannel,
    Logger by Logger.defaultLogger {

    override val tag: String = "reflekt-camera-capture"

    override fun onCaptureCompleted(
        session: CameraCaptureSession,
        request: CaptureRequest,
        result: TotalCaptureResult
    ) {
        request.tag
        super.onCaptureCompleted(session, request, result)
    }

}

private const val COMPLETED_TAG = "completed_tag"

fun CaptureState.isComplete() = tag == COMPLETED_TAG

data class CaptureState(
    val tag: String? = null,
    val focus: Focus = Focus.UNFOCUSED,
    val exposure: Exposure = Exposure.INACTIVE,
    val whiteBalance: WhiteBalance = WhiteBalance.INACTIVE,
    val flash: Flash = Flash.UNAVAILABLE,
    val lens: Lens = Lens.STATIONARY
)

enum class Focus(val focused: Boolean, val locked: Boolean) {
    ACTIVE_SCAN(false, false),
    PASSIVE_SCAN(false, false),
    UNFOCUSED(false, false),
    UNFOCUSED_LOCKED(false, true),
    FOCUSED(true, false),
    FOCUSED_LOCKED(true, true)
}

enum class Exposure {
    CONVERGED,
    INACTIVE,
    SEARCHING,
    LOCKED,
    FLASH_REQUIRED,
    PRECAPTURE
}

enum class WhiteBalance {
    CONVERGED,
    INACTIVE,
    SEARCHING,
    LOCKED
}

enum class Flash {
    UNAVAILABLE,
    CHARGING,
    FIRED,
    PARTIAL,
    READY
}

enum class Lens {
    MOVING,
    STATIONARY
}