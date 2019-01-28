package tech.khana.reflekt.capture

import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Matrix
import android.media.ExifInterface
import android.media.ImageReader
import android.os.Handler
import android.os.HandlerThread
import kotlinx.coroutines.android.asCoroutineDispatcher
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.withContext
import tech.khana.reflekt.core.ReflektSurface
import tech.khana.reflekt.models.*
import tech.khana.reflekt.utils.REFLEKT_TAG
import java.io.File
import java.io.FileOutputStream

class CaptureSaver(
    private val folder: File,
    private val handlerThread: HandlerThread = HandlerThread(REFLEKT_TAG).apply { start() },
    private val photoListener: (File) -> Unit
) : ReflektSurface, ImageReader.OnImageAvailableListener {

    private val captureDispatcher = Handler(handlerThread.looper).asCoroutineDispatcher(REFLEKT_TAG)

    private var imageReader: ImageReader? = null

    override val format = ReflektFormat.Image.Jpeg

    override suspend fun acquireSurface(config: SurfaceConfig): CameraSurface = coroutineScope {

        withContext(captureDispatcher) {

            val resolution = config.resolutions.chooseOptimalResolution(config.aspectRatio)

            val imageReader = ImageReader.newInstance(resolution.width, resolution.height, format.format, 2).apply {
                setOnImageAvailableListener(this@CaptureSaver, Handler(handlerThread.looper))
            }

            CameraSurface(CameraMode.CAPTURE, imageReader.surface)
        }
    }

    private fun List<Resolution>.chooseOptimalResolution(aspectRatio: AspectRatio): Resolution =
        asSequence()
            .filter { it.ratio == aspectRatio.value }
            .sortedBy { it.area }
            .last()

    override fun onImageAvailable(reader: ImageReader) {
        reader.acquireLatestImage().use { image ->
            val imageFile = File(folder, "${image.timestamp}.jpg")
            val buffer = image.planes[0].buffer
            val bytes = ByteArray(buffer.remaining())
            buffer.get(bytes)

            FileOutputStream(imageFile).use { fos ->
                fos.write(bytes)
            }

            val orientation = ExifInterface(imageFile.absolutePath).run {
                getAttributeInt(ExifInterface.TAG_ORIENTATION, ExifInterface.ORIENTATION_UNDEFINED)
            }

            val rotate = when (orientation) {
                ExifInterface.ORIENTATION_ROTATE_90 -> 90f
                ExifInterface.ORIENTATION_ROTATE_180 -> 180f
                ExifInterface.ORIENTATION_ROTATE_270 -> 270f
                else -> 0f
            }

            val srcBitmap = BitmapFactory.decodeFile(imageFile.absolutePath)
            val matrix = Matrix().apply { postRotate(rotate) }
            val rotatedBitmap = Bitmap.createBitmap(
                srcBitmap, 0, 0, srcBitmap.width, srcBitmap.height, matrix, false
            )
            imageFile.delete()

            FileOutputStream(imageFile).use { fos ->
                rotatedBitmap.compress(Bitmap.CompressFormat.JPEG, 90, fos)
            }

            ExifInterface(imageFile.absolutePath).run {
                setAttribute(ExifInterface.TAG_ORIENTATION, ExifInterface.ORIENTATION_NORMAL.toString())
                saveAttributes()
            }

            photoListener(imageFile)
        }
    }

    override suspend fun release() = coroutineScope {
        withContext(captureDispatcher) {
            handlerThread.quit()
            imageReader?.close()
            Unit
        }
    }
}
