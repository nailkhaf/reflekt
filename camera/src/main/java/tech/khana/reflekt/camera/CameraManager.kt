package tech.khana.reflekt.camera

import android.Manifest
import android.content.Context
import android.os.Handler
import android.os.HandlerThread
import android.support.annotation.RequiresPermission
import kotlinx.coroutines.*
import kotlinx.coroutines.android.asCoroutineDispatcher
import tech.khana.reflekt.api.*
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
    sessionFactory: SessionFactory
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
        sessionFactory = sessionFactory,
        handler = handler
    )

    override val logPrefix: String = "CameraManager"
    override val coroutineContext: CoroutineContext
        get() = handler.asCoroutineDispatcher() + errorHandler + job

    private var camera: Camera? = null
    private val availableCameras = mutableSetOf<String>()

    init {
        cameraManager.registerAvailabilityCallback(this, handler)
    }

    override fun onCameraAvailable(cameraId: String) {
        availableCameras += cameraId
    }

    override fun onCameraUnavailable(cameraId: String) {
        availableCameras -= cameraId
    }

    @RequiresPermission(Manifest.permission.CAMERA)
    override suspend fun open(lens: Lens) = withContext(coroutineContext) {
        debug { "#open $lens" }
        camera?.close()
        ctx.requireCameraPermission()
        val camera = cameraFactory()
        val id = cameraManager.findCameraByLens(lens)
        check(id in availableCameras) { "camera is not available" }
        cameraManager.openCameraDevice(id, camera, handler)
        camera.start(cameraConfig)
        debug { "#opened id=$id" }
    }


    override fun release() = runBlocking {
        debug { "#release" }
        withContext(coroutineContext) {
            cameraConfig.surfaces.forEach { it.release() }
            cameraManager.unregisterAvailabilityCallback(this@CameraManager)
            camera?.close()
            Unit
        }
        debug { "#released" }
    }
}