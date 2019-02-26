package tech.khana.reflekt.session.frames

import android.graphics.Rect
import android.hardware.camera2.CameraCaptureSession
import android.hardware.camera2.CameraDevice
import android.hardware.camera2.CaptureResult
import android.os.Handler
import android.view.Surface
import kotlinx.coroutines.*
import kotlinx.coroutines.channels.ReceiveChannel
import tech.khana.reflekt.api.Logger
import tech.khana.reflekt.api.Session
import tech.khana.reflekt.api.SessionException
import tech.khana.reflekt.api.debug
import tech.khana.reflekt.api.models.CameraMode
import tech.khana.reflekt.api.models.FocusMode
import tech.khana.reflekt.session.frames.extensions.repeatingRequestChannel

internal class FrameProcessorSession(
    private val scope: CoroutineScope,
    private val handler: Handler,
    private val surfaces: Map<CameraMode, List<Surface>>
) : Session(), Logger by Logger, CoroutineScope by scope {

    override val logPrefix: String = "FrameProcessorSession"

    private val previewRequest by lazy {
        session.device.createCaptureRequest(CameraDevice.TEMPLATE_PREVIEW)
    }

    private lateinit var session: CameraCaptureSession

    init {
        require(surfaces.containsKey(CameraMode.PREVIEW))

        require(surfaces.containsKey(CameraMode.RECORD).not())
        require(surfaces.containsKey(CameraMode.RECORD_SNAPSHOT).not())
        require(surfaces.containsKey(CameraMode.ZERO_SHUTTER_LUG).not())
    }

    override suspend fun startPreview() = sessionContext {
        debug { "#startPreview" }
        check(::session.isInitialized) { "session is not initialized" }

        val resultChannel = session.repeatingRequestChannel(previewRequest.build(), handler)
        resultChannel.receive()
        listenResult(resultChannel)
        Unit
    }

    private fun listenResult(channel: ReceiveChannel<CaptureResult>) {
        launch {
            for (result in channel) {
                // TODO modify state
            }
        }
    }

    override suspend fun stopPreview() = sessionContext {
        debug { "#stopPreview" }
        session.stopRepeating()
        session.abortCaptures()
    }

    override suspend fun startRecording() = sessionContext {
        debug { "#startRecording" }
        throw UnsupportedOperationException("#recording is unsupported")
    }

    override suspend fun stopRecording() = sessionContext {
        debug { "#stopRecording" }
        throw UnsupportedOperationException("#recording is unsupported")
    }

    override suspend fun takePicture() = sessionContext {
        debug { "#takePicture" }
    }

    override suspend fun focusMode(focusMode: FocusMode) = sessionContext {
        debug { "#focusMode" }
    }

    override suspend fun lockFocus(rect: Rect?) = sessionContext {
        debug { "#lockFocus" }
    }

    override suspend fun unlockFocus() = sessionContext {
        debug { "#unlockFocus" }
    }

    override fun onConfigured(session: CameraCaptureSession) {
        debug { "#onConfigured" }
        this.session = session
    }

    override fun onConfigureFailed(session: CameraCaptureSession) {
        debug { "#onConfigureFailed" }
        this.session = session
        coroutineContext[CoroutineExceptionHandler]?.handleException(
            coroutineContext, SessionException.ConfigureFailedException()
        )
    }

    override fun onReady(session: CameraCaptureSession) {
        debug { "#onReady" }
        this.session = session
    }

    override fun onActive(session: CameraCaptureSession) {
        debug { "#onActive" }
        this.session = session
    }

    override fun onClosed(session: CameraCaptureSession) {
        debug { "#onClosed" }
    }

    override fun close() = runBlocking {
        debug { "#close" }
        session.stopRepeating()
        session.abortCaptures()
        session.close()
    }

    private suspend inline fun <R> sessionContext(
        crossinline block: suspend (CoroutineScope) -> R
    ): R = withTimeoutOrNull(10_000) {
        withContext(this@FrameProcessorSession.coroutineContext) {
            block(this)
        }
    } ?: error("camera is hang")
}
