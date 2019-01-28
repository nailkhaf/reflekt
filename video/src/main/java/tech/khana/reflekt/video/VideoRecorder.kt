package tech.khana.reflekt.video

import android.media.MediaRecorder
import android.os.Handler
import android.os.HandlerThread
import android.view.Surface
import kotlinx.coroutines.android.asCoroutineDispatcher
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.withContext
import tech.khana.reflekt.core.ReflektSurface
import tech.khana.reflekt.models.*
import tech.khana.reflekt.utils.REFLEKT_TAG
import java.io.File

class VideoRecorder(
    private val folder: File,
    private val handlerThread: HandlerThread = HandlerThread(REFLEKT_TAG).apply { start() }
) : ReflektSurface {

    override val format: ReflektFormat = ReflektFormat.Clazz.MediaRecorder

    private val dispatcher = Handler(handlerThread.looper).asCoroutineDispatcher()
    override val supportedModes = setOf(CameraMode.RECORD)
    private var mediaRecorder: MediaRecorder? = null

    init {
        folder.mkdirs()
    }

    override suspend fun acquireSurface(config: SurfaceConfig): Surface = coroutineScope {
        withContext(dispatcher) {

            val resolution = config.resolutions.chooseOptimalResolution(config.aspectRatio)
            val videoFile = File(folder, "${System.currentTimeMillis()}.mp4")
            videoFile.createNewFile()

            mediaRecorder?.release()

            val mediaRecorder = MediaRecorder().apply {
                setAudioSource(MediaRecorder.AudioSource.MIC)
                setVideoSource(MediaRecorder.VideoSource.SURFACE)
                setOutputFormat(MediaRecorder.OutputFormat.MPEG_4)
                setOutputFile(videoFile.absolutePath)
                setVideoEncodingBitRate(10000000)
                setVideoFrameRate(30)
                setVideoSize(resolution.width, resolution.height)
                setVideoEncoder(MediaRecorder.VideoEncoder.H264)
                setAudioEncoder(MediaRecorder.AudioEncoder.AAC)
                prepare()
            }

            this@VideoRecorder.mediaRecorder = mediaRecorder

//            mediaRecorder.setOrientationHint()

            mediaRecorder.surface
        }
    }

    override suspend fun onStart(cameraMode: CameraMode) = coroutineScope {
        withContext(dispatcher) {
            mediaRecorder?.start()
            Unit
        }
    }

    override suspend fun onStop(cameraMode: CameraMode) = coroutineScope {
        withContext(dispatcher) {
            mediaRecorder?.stop()
            mediaRecorder?.reset()
            Unit
        }
    }

    private fun List<Resolution>.chooseOptimalResolution(aspectRatio: AspectRatio): Resolution =
        asSequence()
            .filter { it.ratio == aspectRatio.value }
            .sortedBy { it.area }
            .last()

    override suspend fun release() = coroutineScope {
        withContext(dispatcher) {
            mediaRecorder?.release()
            handlerThread.quitSafely()
            Unit
        }
    }
}