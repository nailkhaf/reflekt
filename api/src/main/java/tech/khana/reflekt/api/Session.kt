package tech.khana.reflekt.api

import android.graphics.Rect
import android.hardware.camera2.CameraCaptureSession
import android.view.Surface
import tech.khana.reflekt.api.models.CameraMode
import tech.khana.reflekt.api.models.FocusMode
import tech.khana.reflekt.api.models.FocusMode.CONTINUOUS

abstract class Session : AutoCloseable, CameraCaptureSession.StateCallback() {

    abstract suspend fun startPreview()

    abstract suspend fun stopPreview()

    abstract suspend fun startRecording()

    abstract suspend fun stopRecording()

    abstract suspend fun takePicture()

    abstract suspend fun focusMode(focusMode: FocusMode = CONTINUOUS)

    abstract suspend fun lockFocus(rect: Rect? = null)

    abstract suspend fun unlockFocus()
}

interface SessionFactory {

    operator fun invoke(
        surfaces: Map<CameraMode, List<Surface>>
    ): Session
}
