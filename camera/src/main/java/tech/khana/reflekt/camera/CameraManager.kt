package tech.khana.reflekt.camera

import android.Manifest
import android.content.Context
import android.os.Handler
import android.os.HandlerThread
import android.support.annotation.RequiresPermission
import kotlinx.coroutines.*
import kotlinx.coroutines.android.asCoroutineDispatcher
import tech.khana.reflekt.api.Logger
import tech.khana.reflekt.api.Manager
import tech.khana.reflekt.api.SessionFactory
import tech.khana.reflekt.api.debug
import tech.khana.reflekt.api.models.CameraConfig
import tech.khana.reflekt.api.models.Lens
import tech.khana.reflekt.camera.extensions.cameraManager
import tech.khana.reflekt.camera.extensions.findCameraByLens
import tech.khana.reflekt.camera.extensions.openCameraDevice
import tech.khana.reflekt.camera.extensions.requireCameraPermission
import kotlin.coroutines.CoroutineContext

// FIXME move to another module
class CameraManager(
    private val ctx: Context,
    private val cameraConfig: CameraConfig,
    sessionFactory: (Handler, CoroutineScope) -> SessionFactory
) : Manager(), CoroutineScope, Logger by Logger {

    private val handlerThread = HandlerThread("reflekt").apply {
        start()
    }
    private val handler = Handler(handlerThread.looper)
    private val errorHandler = CoroutineExceptionHandler { _, e -> }
    private val job = SupervisorJob()
    private val cameraManager = ctx.cameraManager
    private val cameraFactory = CameraFactory(
        coroutineScope = this,
        cameraManager = cameraManager,
        sessionFactory = sessionFactory(handler, this),
        handler = handler
    )

    override val logPrefix: String = "CameraManager"
    override val coroutineContext: CoroutineContext
        get() = handler.asCoroutineDispatcher() + errorHandler + job

    private var camera: CameraDevice? = null
    private val availableCameras = mutableSetOf<String>()

    init {
        cameraConfig.surfaces.forEach { requireNotNull(it) }
        cameraManager.registerAvailabilityCallback(this, handler)
    }

    override fun onCameraAvailable(cameraId: String) {
        launch { availableCameras += cameraId }
    }

    override fun onCameraUnavailable(cameraId: String) {
        launch { availableCameras -= cameraId }
    }

    @RequiresPermission(Manifest.permission.CAMERA)
    override suspend fun open(lens: Lens) = withContext(coroutineContext) {
        debug { "#open $lens" }
        check(camera == null) { "camera is already opened" }
        ctx.requireCameraPermission()
        val camera = cameraFactory()
        val id = cameraManager.findCameraByLens(lens)
        withTimeoutOrNull(1_000) {
            while (id !in availableCameras) {
                delay(10)
            }
        } ?: error("camera is not available")
        cameraManager.openCameraDevice(id, camera, handler)
        camera.startSession(cameraConfig)
        this@CameraManager.camera = camera
        debug { "#opened id=$id" }
    }

    override fun close() = runBlocking {
        withContext(coroutineContext) {
            debug { "#close" }
            job.cancelChildren()
            camera.close()
            camera = null
            debug { "#closed" }
        }
    }

    override fun release() = runBlocking {
        withContext(coroutineContext) {
            debug { "#release" }
            job.cancelChildren()
            camera.close()
            cameraManager.unregisterAvailabilityCallback(this@CameraManager)
            cameraConfig.surfaces.forEach { it.release() }
            job.cancel()
            handlerThread.quitSafely()
            debug { "#released" }
        }
    }
}