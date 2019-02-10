package tech.khana.reflekt.frames

import android.content.Context
import android.graphics.Bitmap
import android.media.ImageReader
import android.os.HandlerThread
import kotlinx.coroutines.withContext
import tech.khana.reflekt.utils.REFLEKT_TAG

class RgbFrameProcessor(
    context: Context,
    handlerThread: HandlerThread = HandlerThread(REFLEKT_TAG).apply { start() },
    private val onFrameReceived: (Bitmap) -> Unit = {}
) : AbstractFrameProcessor(handlerThread), ImageReader.OnImageAvailableListener {

    private val converter: YuvToRgbaConverter = YuvToRgbaConverter(context)

    override fun onImageAvailable(reader: ImageReader) {
        reader.acquireNextImage()?.use {
            onFrameReceived(converter.convert(it))
        }
    }

    override suspend fun release() = withContext(dispatcher) {
        converter.release()
        super.release()
    }
}
