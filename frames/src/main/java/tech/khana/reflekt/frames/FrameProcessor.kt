package tech.khana.reflekt.frames

import android.media.Image
import android.media.ImageReader
import android.os.Handler
import android.os.HandlerThread
import android.view.Surface
import kotlinx.coroutines.android.asCoroutineDispatcher
import kotlinx.coroutines.withContext
import tech.khana.reflekt.core.ReflektSurface
import tech.khana.reflekt.models.*
import tech.khana.reflekt.utils.REFLEKT_TAG

private const val MAX_SIDE = 500

class FrameProcessor(
    private val handlerThread: HandlerThread = HandlerThread(REFLEKT_TAG).apply { start() },
    private val onFrameReceived: (Image) -> Unit = {}
) : ReflektSurface, ImageReader.OnImageAvailableListener {

    override val format = ReflektFormat.Image.Yuv
    override val supportedModes = setOf(CameraMode.PREVIEW)

    private val dispatcher = Handler(handlerThread.looper).asCoroutineDispatcher(REFLEKT_TAG)
    private var imageReader: ImageReader? = null

    override suspend fun acquireSurface(config: SurfaceConfig): Surface = withContext(dispatcher) {
        val resolution = config.resolutions.chooseOptimalResolution(config.aspectRatio)

        imageReader?.close()

        ImageReader.newInstance(resolution.width, resolution.height, format.format, 2).apply {
            setOnImageAvailableListener(this@FrameProcessor, Handler(handlerThread.looper))
        }.also {
            imageReader = it
        }.surface
    }

    private fun List<Resolution>.chooseOptimalResolution(aspectRatio: AspectRatio): Resolution =
        asSequence()
            .filter { it.ratio == aspectRatio.value }
            .filter { it.width <= MAX_SIDE && it.height <= MAX_SIDE }
            .maxBy { it.area } ?: throw IllegalStateException()

    override fun onImageAvailable(reader: ImageReader) {
        reader.acquireNextImage()?.use {
            onFrameReceived(it)
        }
    }

    override suspend fun release() = withContext(dispatcher) {
        handlerThread.quitSafely()
        imageReader?.close()
        Unit
    }
}
