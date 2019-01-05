package tech.khana.reflekt.core

import android.content.Context
import android.os.Handler
import android.os.HandlerThread
import kotlinx.coroutines.android.asCoroutineDispatcher
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.withContext
import kotlinx.coroutines.yield

internal class ReflektCameraImpl(
    ctx: Context,
    private val cameraConfiguration: CameraConfiguration,
    private val handlerThread: HandlerThread = HandlerThread("").apply { start() }
) : ReflektCamera {

    private val cameraManager = ctx.cameraManager

    private val requestFactory by lazy { RequestFactory(cameraManager) }

    private val cameraDispatcher = Handler(handlerThread.looper).asCoroutineDispatcher("")

    private var reflektDevice: ReflektDevice? = null

    override suspend fun open() = coroutineScope {

        val cameraId = cameraManager.findCameraByDirect(cameraConfiguration.direct)

        val supportedLevel = cameraManager.supportedLevel(cameraId)
        cameraLogger.debug { "supported level=${supportedLevel.description}" }

        val surfaces = cameraConfiguration.surfaces.map { cameraSurface ->
            yield()

            val format = cameraSurface.format
            val outputResolutions = when (format) {
                is LensFormat.Image -> cameraManager.outputResolutions(cameraId, format.format)
                is LensFormat.Clazz -> cameraManager.outputResolutions(cameraId, format.clazz)
            }

            val surfaceConfig = SurfaceConfig(outputResolutions, cameraConfiguration.rotation)

            val typedSurface = cameraSurface.acquireSurface(surfaceConfig)

            require(cameraManager.surfaceSupported(cameraId, typedSurface.surface))
            { "surface is not supported" }

            typedSurface
        }

        val cameraDevice = cameraManager.openCamera(handlerThread)
        withContext(cameraDispatcher) {
            reflektDevice = ReflektDeviceImpl(
                cameraDevice, requestFactory, surfaces, handlerThread
            )
        }
    }

    override suspend fun startPreview() {
        withContext(cameraDispatcher) {
            val device = reflektDevice
            check(device != null) { "camera is not opened" }
            device.startPreview()
        }
    }

    override suspend fun stop() {
        withContext(cameraDispatcher) {
            val device = reflektDevice
            check(device != null) { "camera is not opened" }
            device.release()
        }
    }

    override fun getAvailableLenses(): List<Lens> =
        cameraManager.cameraIdList.map { cameraManager.directCamera(it) }
}
