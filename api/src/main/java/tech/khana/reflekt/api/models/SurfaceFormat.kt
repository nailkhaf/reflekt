package tech.khana.reflekt.api.models

import android.graphics.ImageFormat
import android.graphics.SurfaceTexture
import android.media.ImageReader
import android.media.ImageWriter
import android.media.MediaRecorder

sealed class SurfaceFormat {

    sealed class Image(val format: Int) : SurfaceFormat() {
        object Jpeg : Image(ImageFormat.JPEG)
        object Yuv : Image(ImageFormat.YUV_420_888)
    }

    sealed class Priv(val klass: Class<out Any>) : SurfaceFormat() {
        object Texture : Priv(SurfaceTexture::class.java)
        object Recorder : Priv(MediaRecorder::class.java)
        object Reader : Priv(ImageReader::class.java)
        object Writer : Priv(ImageWriter::class.java)
    }
}

enum class OutputType {
    PREVIEW,
    MAXIMUM,
    RECORD
}