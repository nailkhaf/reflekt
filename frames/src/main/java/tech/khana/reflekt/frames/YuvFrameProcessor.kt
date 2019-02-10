package tech.khana.reflekt.frames

import android.media.Image
import android.media.ImageReader
import android.os.HandlerThread
import tech.khana.reflekt.utils.REFLEKT_TAG

class YuvFrameProcessor(
    handlerThread: HandlerThread = HandlerThread(REFLEKT_TAG).apply { start() },
    private val onFrameReceived: (Image) -> Unit = {}
) : AbstractFrameProcessor(handlerThread), ImageReader.OnImageAvailableListener {

    override fun onImageAvailable(reader: ImageReader) {
        reader.acquireNextImage()?.use {
            onFrameReceived(it)
        }
    }
}
