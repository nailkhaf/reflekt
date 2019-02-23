package tech.khana.reflekt.frames

import android.content.Context
import android.graphics.Bitmap
import android.graphics.ImageFormat
import android.media.Image
import android.support.v8.renderscript.*
import tech.khana.reflekt.models.Resolution


class YuvToRgbaConverter(
    context: Context
) {

    private val rs = RenderScript.create(context)
    private val script = ScriptIntrinsicYuvToRGB.create(rs, Element.U8(rs))

    private lateinit var input: Allocation
    private lateinit var output: Allocation
    private lateinit var bitmap: Bitmap
    private lateinit var nv21: ByteArray

    private var currentResolution: Resolution = Resolution()

    fun convert(image: Image): Bitmap {

        if (currentResolution.width != image.width || currentResolution.height != image.height) {
            allocate(image.width, image.height)
            currentResolution = Resolution(image.width, image.height)
        }

        YUV420toNV21(image, nv21)
        input.copyFrom(nv21)

        script.setInput(input)
        script.forEach(output)

        output.copyTo(bitmap)

        return bitmap
    }

    private fun allocate(width: Int, height: Int) {
        val inputType = Type.Builder(rs, Element.U8(rs)).run {
            setX(width)
            setY(height)
            setYuvFormat(ImageFormat.NV21)
            create()
        }

        val outputType = Type.Builder(rs, Element.RGBA_8888(rs)).run {
            setX(width)
            setY(height)
            create()
        }

        input = Allocation.createTyped(rs, inputType, Allocation.USAGE_SCRIPT)

        output = Allocation.createTyped(rs, outputType, Allocation.USAGE_SCRIPT)

        bitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)

        nv21 = ByteArray(width * height * ImageFormat.getBitsPerPixel(ImageFormat.YUV_420_888) / 8)
    }

    fun release() {
        if (::input.isInitialized) input.destroy()
        if (::output.isInitialized) output.destroy()
    }

    private fun YUV420toNV21(image: Image, data: ByteArray) {
        val crop = image.cropRect
        val width = crop.width()
        val height = crop.height()
        val planes = image.planes
        val rowData = ByteArray(planes[0].rowStride)

        var channelOffset = 0
        var outputStride = 1
        for (i in planes.indices) {
            when (i) {
                0 -> {
                    channelOffset = 0
                    outputStride = 1
                }
                1 -> {
                    channelOffset = width * height + 1
                    outputStride = 2
                }
                2 -> {
                    channelOffset = width * height
                    outputStride = 2
                }
            }

            val buffer = planes[i].buffer
            val rowStride = planes[i].rowStride
            val pixelStride = planes[i].pixelStride

            val shift = if (i == 0) 0 else 1
            val w = width shr shift
            val h = height shr shift
            buffer.position(rowStride * (crop.top shr shift) + pixelStride * (crop.left shr shift))
            for (row in 0 until h) {
                val length: Int
                if (pixelStride == 1 && outputStride == 1) {
                    length = w
                    buffer.get(data, channelOffset, length)
                    channelOffset += length
                } else {
                    length = (w - 1) * pixelStride + 1
                    buffer.get(rowData, 0, length)
                    for (col in 0 until w) {
                        data[channelOffset] = rowData[col * pixelStride]
                        channelOffset += outputStride
                    }
                }
                if (row < h - 1) {
                    buffer.position(buffer.position() + rowStride - length)
                }
            }
        }
    }
}
