package tech.khana.reflekt.session.frames

import android.hardware.camera2.CameraCaptureSession
import android.hardware.camera2.CameraDevice.TEMPLATE_PREVIEW
import android.hardware.camera2.CaptureResult
import android.os.Handler
import android.view.Surface
import kotlinx.coroutines.*
import kotlinx.coroutines.channels.ReceiveChannel
import tech.khana.reflekt.api.*
import tech.khana.reflekt.api.models.CameraMode
import tech.khana.reflekt.api.models.CameraMode.PREVIEW
import tech.khana.reflekt.session.frames.extensions.repeatingRequestChannel
import tech.khana.reflekt.session.frames.extensions.weak
import kotlin.reflect.KClass

internal class CaptureSessionImpl(
    private val scope: CoroutineScope,
    private val handler: Handler,
    private val surfaces: Map<CameraMode, List<Surface>>,
    features: List<FeatureFactory>
) : CaptureSession(), FeatureHolder, Logger by Logger, CoroutineScope by scope {

    override val logPrefix: String = "CaptureSessionImpl"

    private lateinit var session: CameraCaptureSession

    private val features: Map<CameraMode, List<Feature<*>>>

    init {
        require(surfaces.containsKey(PREVIEW))
        this.features = features
            .map { it(weak(this)) }
            .map { it to it.supportedModes }
            .groupByCameraMode()
    }

    override suspend fun startPreview() = sessionContext {
        debug { "#startPreview" }
        check(::session.isInitialized) { "session is not initialized" }
        val requestChannel = with(session) {
            val previewRequest = device.createCaptureRequest(TEMPLATE_PREVIEW)
            features[PREVIEW]?.forEach { it.prepareRequest(previewRequest) }
            surfaces[PREVIEW]?.forEach { previewRequest.addTarget(it) }
            previewRequest.setTag(PREVIEW)
            repeatingRequestChannel(previewRequest.build(), handler)
        }
        requestChannel.receive()
        processResult(requestChannel)
        Unit
    }

    private fun processResult(
        resultChannel:
        ReceiveChannel<Pair<CameraMode, CaptureResult>>
    ) {
        launch {
            for ((cameraMode, result) in resultChannel) {
                features[cameraMode]?.forEach { it.readResult(result) }
            }
        }
    }

    override fun getFeature(klass: KClass<out Feature<*>>): Feature<*> =
        features.values
            .flatten()
            .distinct()
            .find { it::class == klass }
            ?: NotSupportedFeature

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
        throw UnsupportedOperationException("#recording is unsupported")
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

    override fun close() {
        debug { "#close" }
        session.stopRepeating()
        session.abortCaptures()
        session.close()
        debug { "#closed" }
    }

    private suspend inline fun <R> sessionContext(
        crossinline block: suspend (CoroutineScope) -> R
    ): R = withTimeoutOrNull(10_000) {
        withContext(this@CaptureSessionImpl.coroutineContext) {
            block(this)
        }
    } ?: error("camera is hang")
}
