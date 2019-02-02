package tech.khana.reflekt.video

import android.media.MediaRecorder
import android.os.Handler
import android.os.HandlerThread
import android.view.Surface
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.android.asCoroutineDispatcher
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import tech.khana.reflekt.core.ReflektSurface
import tech.khana.reflekt.models.*
import tech.khana.reflekt.preferences.JpegPreference.getJpegOrientation
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
        GlobalScope.launch(Dispatchers.IO) { folder.mkdirs() }
    }

    override suspend fun acquireSurface(config: SurfaceConfig): Surface = withContext(dispatcher) {
        val resolution = config.resolutions.chooseOptimalResolution(config.aspectRatio)
        val rotation = getJpegOrientation(config.lensDirect, config.hardwareRotation, config.displayRotation)

        val videoFile = File(folder, "${System.currentTimeMillis()}.mp4")
        withContext(Dispatchers.IO) { videoFile.createNewFile() }

        mediaRecorder?.release()

        MediaRecorder().apply {
            setAudioSource(MediaRecorder.AudioSource.MIC)
            setVideoSource(MediaRecorder.VideoSource.SURFACE)
            setOutputFormat(MediaRecorder.OutputFormat.MPEG_4)
            setOutputFile(videoFile.absolutePath)
            setVideoEncodingBitRate(10000000)
            setVideoFrameRate(30)
            setVideoSize(resolution.width, resolution.height)
            setVideoEncoder(MediaRecorder.VideoEncoder.H264)
            setAudioEncoder(MediaRecorder.AudioEncoder.AAC)
            setOrientationHint(rotation.value)
            withContext(Dispatchers.IO) { prepare() }
        }.also {
            mediaRecorder = it
        }.surface
    }

    override suspend fun onStart(cameraMode: CameraMode) = withContext(dispatcher) {
        if (cameraMode == CameraMode.RECORD) {
            mediaRecorder?.start()
        }
        Unit
    }


    override suspend fun onStop(cameraMode: CameraMode) = withContext(dispatcher) {
        if (cameraMode == CameraMode.RECORD) {
            mediaRecorder?.stop()
            mediaRecorder?.reset()
        }
        Unit
    }


    private fun List<Resolution>.chooseOptimalResolution(aspectRatio: AspectRatio): Resolution =
        asSequence()
            .filter { it.ratio == aspectRatio.value }
            .sortedBy { it.area }
            .last()

    override suspend fun release() = withContext(dispatcher) {
        mediaRecorder?.release()
        mediaRecorder = null
        handlerThread.quitSafely()
        Unit
    }
}