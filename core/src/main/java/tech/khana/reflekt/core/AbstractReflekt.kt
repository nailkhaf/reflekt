package tech.khana.reflekt.core

import android.content.Context
import android.hardware.camera2.CameraManager
import android.location.Location
import android.os.Handler
import android.os.HandlerThread
import kotlinx.coroutines.android.asCoroutineDispatcher
import kotlinx.coroutines.withContext
import tech.khana.reflekt.ext.availableMaxZoom
import tech.khana.reflekt.ext.cameraManager
import tech.khana.reflekt.ext.findCameraByLens
import tech.khana.reflekt.models.AspectRatio
import tech.khana.reflekt.models.FlashMode
import tech.khana.reflekt.models.LensDirect
import tech.khana.reflekt.models.Settings
import tech.khana.reflekt.preferences.FlashPreference
import tech.khana.reflekt.preferences.JpegPreference
import tech.khana.reflekt.preferences.ZoomPreference
import tech.khana.reflekt.utils.REFLEKT_TAG

abstract class AbstractReflekt(
    ctx: Context,
    settings: Settings,
    private val handlerThread: HandlerThread,
    cameraPreferences: List<CameraPreference>
) : Reflekt {

    protected val cameraManager: CameraManager = ctx.cameraManager

    protected val camera: ReflektCamera = ReflektCameraImpl(ctx, handlerThread, cameraPreferences)

    protected val cameraDispatcher = Handler(handlerThread.looper).asCoroutineDispatcher(REFLEKT_TAG)

    protected var currentSettings = settings

    override suspend fun previewAspectRatio(aspectRatio: AspectRatio) = withContext(cameraDispatcher) {
        if (currentSettings.aspectRatio == aspectRatio) return@withContext

        camera.stopSession()
        camera.startSession(
            currentSettings.surfaces,
            currentSettings.displayRotation,
            currentSettings.displayResolution,
            aspectRatio
        )
        camera.startPreview()
        currentSettings = currentSettings.copy(aspectRatio = aspectRatio)
    }

    override suspend fun lens(lensDirect: LensDirect) = withContext(cameraDispatcher) {
        if (lensDirect == currentSettings.lensDirect) return@withContext

        camera.close()
        camera.open(lensDirect)
        camera.startSession(
            currentSettings.surfaces,
            currentSettings.displayRotation,
            currentSettings.displayResolution,
            currentSettings.aspectRatio
        )
        camera.startPreview()
        currentSettings = currentSettings.copy(lensDirect = lensDirect)
    }

    override suspend fun flash(flashMode: FlashMode) = withContext(cameraDispatcher) {
        if (flashMode == FlashPreference.flashMode) return@withContext

        camera.stopPreview()
        FlashPreference.flashMode = flashMode
        camera.startPreview()
    }

    override suspend fun zoom(zoom: Float) = withContext(cameraDispatcher) {
        val maxZoom = cameraManager.availableMaxZoom(cameraManager.findCameraByLens(currentSettings.lensDirect))
        require(zoom <= maxZoom) { "zoom more max zoom" }
        if (zoom == ZoomPreference.zoomLevel) return@withContext

        camera.stopPreview()
        ZoomPreference.zoomLevel = zoom
        camera.startPreview()
    }

    override suspend fun location(location: Location) = withContext(cameraDispatcher) {
        JpegPreference.location = location
    }

    override suspend fun release() = withContext(cameraDispatcher) {
        camera.close()
        currentSettings.surfaces.forEach { it.release() } // FIXME sometimes reflekt surfaces can be not released
        handlerThread.quitSafely()
    }
}