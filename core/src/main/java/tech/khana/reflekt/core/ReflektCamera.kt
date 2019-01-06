package tech.khana.reflekt.core

import android.content.Context
import android.os.Handler
import android.os.HandlerThread
import kotlinx.coroutines.android.asCoroutineDispatcher
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.withContext
import kotlinx.coroutines.yield

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

    private val userSettings: UserSettings
        get() = settingsProvider.currentSettings

    override suspend fun open() = coroutineScope {
        cameraLogger.debug { "#open" }
        withContext(cameraDispatcher) {

            check(reflektDevice == null) { "camera already is opened" }

            val cameraId = cameraManager.findCameraByDirect(userSettings.direct)

            settingsProvider.supportLevel(cameraManager.supportedLevel(cameraId))
            cameraLogger.debug { "supported level=${userSettings.supportLevel.description}" }

            val surfaces = userSettings.surfaces.map { cameraSurface ->
                yield()

                val format = cameraSurface.format
                val outputResolutions = when (format) {
                    is ReflektFormat.Image -> cameraManager.outputResolutions(
                        cameraId, format.format
                    )
                    is ReflektFormat.Clazz -> cameraManager.outputResolutions(
                        cameraId, format.clazz
                    )
                }

                val surfaceConfig = SurfaceConfig(
                    outputResolutions,
                    userSettings.rotation,
                    userSettings.previewAspectRatio
                )

//                cameraSurface.acquireSurface(surfaceConfig).surface.release()
                val typedSurface = cameraSurface.acquireSurface(surfaceConfig)

                require(cameraManager.surfaceSupported(cameraId, typedSurface.surface))
                { "surface is not supported" }

                typedSurface
            }

            val cameraDevice = cameraManager.openCamera(cameraId, handlerThread)

            reflektDevice = ReflektDeviceImpl(
                cameraDevice, requestFactory, surfaces, handlerThread
            ).apply { open() }
        }
    }

    override suspend fun startPreview() = coroutineScope {
        cameraLogger.debug { "#startPreview" }
        withContext(cameraDispatcher) {
            val device = reflektDevice
            check(device != null) { "camera is not opened" }
            device.startPreview()
        }
    }

    override suspend fun stopPreview() {
        cameraLogger.debug { "#stopPreview" }
        withContext(cameraDispatcher) {
            val device = reflektDevice
            check(device != null) { "camera is not opened" }
            device.stopPreview()
        }
    }

    override suspend fun previewAspectRatio(aspectRatio: AspectRatio) {
        cameraLogger.debug { "#previewAspectRatio" }
        withContext(cameraDispatcher) {
            val device = reflektDevice
            check(device != null) { "camera is not opened" }
            device.stopPreview()
        }
    }

    override suspend fun stop() = coroutineScope {
        cameraLogger.debug { "#stop" }
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
