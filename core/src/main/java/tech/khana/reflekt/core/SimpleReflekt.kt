package tech.khana.reflekt.core

import android.content.Context
import android.os.HandlerThread
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.withContext
import tech.khana.reflekt.models.Settings
import tech.khana.reflekt.models.switch
import tech.khana.reflekt.preferences.JpegPreference

class SimpleReflekt(
    ctx: Context,
    settings: Settings,
    handlerThread: HandlerThread = HandlerThread("").apply { start() },
    cameraPreferences: List<CameraPreference> = listOf(JpegPreference)
) : AbstractReflekt(
    ctx = ctx,
    settings = settings,
    cameraPreferences = cameraPreferences,
    handlerThread = handlerThread
) {

    suspend fun start() = coroutineScope {
        withContext(cameraDispatcher) {
            camera.open(currentSettings.lensDirect)
            camera.startSession(currentSettings.surfaces, currentSettings.displayRotation, currentSettings.aspectRatio)
            camera.startPreview()
        }
    }

    suspend fun switchLens() = coroutineScope {
        withContext(cameraDispatcher) {
            lens(currentSettings.lensDirect.switch())
        }
    }

    suspend fun startRecord() = coroutineScope {
        withContext(cameraDispatcher) {
            camera.stopPreview()
            camera.startRecord()
        }
    }

    suspend fun stopRecord() = coroutineScope {
        withContext(cameraDispatcher) {
            camera.stopRecord()
            camera.startPreview()
        }
    }

    suspend fun capture() = coroutineScope {
        withContext(cameraDispatcher) {
            camera.capture()
        }
    }

    suspend fun stop() = coroutineScope {
        withContext(cameraDispatcher) {
            camera.close()
        }
    }
}