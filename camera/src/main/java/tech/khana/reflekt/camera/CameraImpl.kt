package tech.khana.reflekt.camera

import android.hardware.camera2.CameraDevice
import android.hardware.camera2.CameraManager
import android.os.Handler
import android.view.Surface
import kotlinx.coroutines.*
import kotlinx.coroutines.channels.ReceiveChannel
import kotlinx.coroutines.channels.map
import tech.khana.reflekt.api.*
import tech.khana.reflekt.api.models.CameraConfig
import tech.khana.reflekt.api.models.CameraMode
import tech.khana.reflekt.api.models.SurfaceConfig
import tech.khana.reflekt.camera.extensions.*
import kotlin.coroutines.CoroutineContext
import tech.khana.reflekt.api.Surface as ReflektSurface

internal class CameraImpl(
    private val scope: CoroutineScope,
    private val handler: Handler,
    private val cameraManager: CameraManager,
    private val sessionFactory: SessionFactory
) : Camera(),
    Logger by Logger,
    CoroutineScope by scope {

    private val job = SupervisorJob()

    override val coroutineContext: CoroutineContext
        get() = scope.coroutineContext + job

    override val logPrefix: String = "CameraImpl"

    override val id: String
        get() = if (::camera.isInitialized.not()) ""
        else camera.id

    private lateinit var camera: CameraDevice

    private var session: CaptureSession? = null

    override suspend fun start(
        cameraConfig: CameraConfig
    ) = cameraContext {
        debug { "#startSession" }
        check(::camera.isInitialized) { "camera is not initialized" }
        check(session == null) { "session is already started" }

        val maxRecordSize = camera.maxRecordSize()
        val cameraCharacteristics =
            cameraManager.getCameraCharacteristics(camera.id)
        val surfaceOutputConfigurator = SurfaceOutputConfigurator(
            cameraCharacteristics,
            cameraConfig.screenSize,
            maxRecordSize,
            cameraConfig.surfaces
        )
        val sensorOrientation = cameraCharacteristics.hardwareOrientation()
        val lens = cameraManager.directCamera(camera.id)

        val features = cameraConfig.surfaces.map { it.features }
            .flatten()
            .distinct()

        val surfaces = cameraConfig.surfaces
            .map {
                val outputSizes = surfaceOutputConfigurator.getOutputSizes(it)
                it to SurfaceConfig(
                    outputSizes, cameraConfig.screenOrientation,
                    sensorOrientation, lens
                )
            }
            .map { (surface, config) ->
                surface.acquireSurface(config).map { it to surface.modes }
            }

        val surfacesChannel = surfaces
            .combineLatest(this@CameraImpl)

        val surfaceList = withTimeoutOrNull(10_000) {
            surfacesChannel.receive()
        } ?: error("can't get surfaces")

        val surfacesByMode = surfaceList
            .groupByCameraMode()

        session = sessionFactory(surfacesByMode, features).also { session ->
            camera.createSession(surfaceList.map { it.first }, session, handler)
            session.startPreview()
        }

        listenSurfaces(surfacesChannel, features)

        debug { "#started" }
    }

    private fun listenSurfaces(
        channel: ReceiveChannel<List<Pair<Surface, Set<CameraMode>>>>,
        features: List<FeatureFactory>
    ) = launch {
        for (item in channel) {
            debug { "#onSurfacesChanged" }
            session?.close()
            session = sessionFactory(
                item.groupByCameraMode(),
                features
            ).also { session ->
                camera.createSession(item.map { it.first }, session, handler)
                session.startPreview()
            }
        }
    }

    override fun onOpened(camera: CameraDevice) {
        debug { "#onOpened" }
        this.camera = camera
    }

    override fun onDisconnected(camera: CameraDevice) {
        debug { "#onDisconnected" }
        camera.close()
    }

    override fun onError(camera: CameraDevice, error: Int) {
        debug { "#onError" }
        val err = cameraExceptionByErrorCode(error)
        error(err)
        coroutineContext[CoroutineExceptionHandler]?.handleException(
            coroutineContext,
            err
        )
        camera.close()
    }

    override fun onClosed(camera: CameraDevice) {
        debug { "#onClosed" }
    }

    override fun close() {
        launch {
            debug { "#close" }
            check(::camera.isInitialized) { "camera is not initialized" }
            job.cancelChildren()
            session?.close()
            camera.close()
            job.cancel()
            debug { "#closed" }
        }
    }

    private suspend inline fun <R> cameraContext(
        crossinline block: suspend (CoroutineScope) -> R
    ): R = withTimeoutOrNull(10_000) {
        withContext(this@CameraImpl.coroutineContext) {
            block(this)
        }
    } ?: error("camera is hang")
}
