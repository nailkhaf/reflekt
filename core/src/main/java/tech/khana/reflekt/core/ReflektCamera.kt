package tech.khana.reflekt.core

import android.content.Context
import android.os.Handler
import android.os.HandlerThread
import kotlinx.coroutines.*
import kotlinx.coroutines.android.asCoroutineDispatcher

class ReflektCameraImpl(
    ctx: Context,
    userSettings: UserSettings,
    private val handlerThread: HandlerThread = HandlerThread("").apply { start() },
    private val settingsProvider: SettingsProvider = SettingsProviderImpl(userSettings),
    private val requestFactory: RequestFactory =
        RequestFactoryImpl(ctx.cameraManager, settingsProvider)
    ) : ReflektCamera {

    private val cameraManager = ctx.cameraManager

    private val cameraDispatcher = Handler(handlerThread.looper).asCoroutineDispatcher("")

    private var reflektDevice: ReflektDevice? = null

    private val currentSettings: UserSettings
        get() = settingsProvider.currentSettings

    override suspend fun open() = coroutineScope {
        cameraLogger.debug { "#open" }
        withContext(cameraDispatcher) {

            check(reflektDevice == null) { "camera already is opened" }

            val cameraId = cameraManager.findCameraByDirect(currentSettings.direct)

            settingsProvider.supportLevel(cameraManager.supportedLevel(cameraId))
            cameraLogger.debug { "supported level=${currentSettings.supportLevel.description}" }

            val cameraDevice = cameraManager.openCamera(cameraId, handlerThread)

            reflektDevice = ReflektDeviceImpl(
                cameraDevice, requestFactory, cameraId, handlerThread
            )
        }
    }

    override suspend fun startSession() = coroutineScope {
        cameraLogger.debug { "#startSession" }
        withContext(cameraDispatcher) {
            val device = reflektDevice
            check(device != null) { "camera is not opened" }

            val asyncSurfaces = currentSettings.surfaces.map { cameraSurface ->
                yield()

                val format = cameraSurface.format
                val outputResolutions = when (format) {
                    is ReflektFormat.Image -> cameraManager.outputResolutions(
                        device.cameraId, format.format
                    )
                    is ReflektFormat.Clazz -> cameraManager.outputResolutions(
                        device.cameraId, format.clazz
                    )
                }

                val surfaceConfig = SurfaceConfig(
                    outputResolutions,
                    currentSettings.rotation,
                    currentSettings.previewAspectRatio
                )

                async(Dispatchers.Default) {
                    val typedSurface = cameraSurface.acquireSurface(surfaceConfig)

                    require(cameraManager.surfaceSupported(device.cameraId, typedSurface.surface))
                    { "surface is not supported" }

                    typedSurface
                }
            }


            val surfaces = asyncSurfaces.map { it.await() }
            device.startSession(surfaces)
        }
    }

    override suspend fun stopSession() = coroutineScope {
        cameraLogger.debug { "#stopSession" }
        withContext(cameraDispatcher) {
            val device = reflektDevice
            check(device != null) { "camera is not opened" }
            device.stopSession()
        }
    }

    override suspend fun startPreview() = coroutineScope {
        cameraLogger.debug { "#startPreview" }
        withContext(cameraDispatcher) {
            val device = reflektDevice
            check(device != null) { "camera is not opened" }
            device.startPreview()
            settingsProvider.previewActive(true)
        }
    }

    override suspend fun stopPreview() = coroutineScope {
        cameraLogger.debug { "#stopPreview" }
        withContext(cameraDispatcher) {
            val device = reflektDevice
            check(device != null) { "camera is not opened" }
            device.stopPreview()
            settingsProvider.previewActive(false)
        }
    }

    override suspend fun previewAspectRatio(aspectRatio: AspectRatio) = coroutineScope {
        cameraLogger.debug { "#previewAspectRatio" }
        withContext(cameraDispatcher) {
            val device = reflektDevice
            check(device != null) { "camera is not opened" }
            if (currentSettings.previewAspectRatio == aspectRatio) return@withContext

            stopSession()
            settingsProvider.previewAspectRation(aspectRatio)
            startSession()
            if (currentSettings.previewActive) {
                device.startPreview()
            }
        }
    }

    override suspend fun availablePreviewAspectRatios(): List<AspectRatio> = coroutineScope {
        cameraLogger.debug { "#availablePreviewAspectRatios" }
        withContext(cameraDispatcher) {
            val device = reflektDevice
            check(device != null) { "camera is not opened" }
            AspectRatio.values().toList()
        }
    }

    override suspend fun close() = coroutineScope {
        cameraLogger.debug { "#close" }
        withContext(cameraDispatcher) {
            reflektDevice?.release()
            reflektDevice = null
        }
        Unit
    }

    override suspend fun getAvailableLenses(): List<Lens> = coroutineScope {
        cameraManager.cameraIdList.map { cameraManager.directCamera(it) }
    }
}
