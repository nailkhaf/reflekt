package tech.khana.reflekt.core

import android.Manifest.permission.CAMERA
import android.content.Context
import android.hardware.camera2.CameraCaptureSession
import android.hardware.camera2.CameraDevice
import android.os.Handler
import android.os.HandlerThread
import android.support.v4.content.ContextCompat
import android.support.v4.content.PermissionChecker.PERMISSION_GRANTED
import kotlinx.coroutines.*
import kotlinx.coroutines.android.asCoroutineDispatcher
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import tech.khana.reflekt.ext.*
import tech.khana.reflekt.models.*
import tech.khana.reflekt.preferences.ZoomPreference
import tech.khana.reflekt.utils.Logger
import tech.khana.reflekt.utils.debug
import tech.khana.reflekt.utils.error
import tech.khana.reflekt.utils.warn

class ReflektCameraImpl(
    private val ctx: Context,
    private val handlerThread: HandlerThread = HandlerThread("").apply { start() },
    private val cameraPreferences: List<CameraPreference> = emptyList()
) : ReflektCamera, Logger by Logger.defaultLogger {

    private val cameraManager = ctx.cameraManager

    private val cameraDispatcher = Handler(handlerThread.looper).asCoroutineDispatcher("")

    private var cameraDevice: CameraDevice? = null
    private var captureSession: CameraCaptureSession? = null
    private var currentSurfaces: List<CameraSurface>? = null
    private var currentSettings: Settings = Settings()

    private var pendingException: CameraException? = null
    private val cameraOpenMutex = Mutex()
    private val commonLock = Mutex()

    private val cameraDeviceCallbacks = object : CameraDevice.StateCallback() {

        override fun onOpened(camera: CameraDevice) {
            debug { "#onOpened" }
            cameraDevice = camera
            if (cameraOpenMutex.isLocked) cameraOpenMutex.unlock()
        }

        override fun onDisconnected(camera: CameraDevice) {
            debug { "#onDisconnected" }
            camera.close()
        }

        override fun onError(camera: CameraDevice, error: Int) {
            warn { "#onError" }
            pendingException = cameraExceptionByErrorCode(error).also {
                warn { it.message ?: "" }
                error { it }
            }
            camera.close()
        }

        override fun onClosed(camera: CameraDevice) {
            debug { "#onClosed" }
            cameraDevice = null
            if (cameraOpenMutex.isLocked) cameraOpenMutex.unlock()
        }
    }

    override suspend fun open(settings: Settings) = coroutineScope {
        debug { "#open" }
        withContext(cameraDispatcher) {
            commonLock.withLock {
                pendingException?.let { throw it }
                check(captureSession == null) { "session is not closed" }
                check(cameraDevice == null) { "camera already is opened" }

                val id = cameraManager.findCameraByLens(settings.lensDirect)

                if (ContextCompat.checkSelfPermission(ctx, CAMERA) == PERMISSION_GRANTED) {
                    try {
                        cameraManager.openCamera(id, cameraDeviceCallbacks, Handler(handlerThread.looper))
                    } catch (e: Exception) {
                        warn { e.message ?: "" }
                        error { e }
                        throw e
                    }
                } else {
                    throw CameraException.CameraPermissionRequired()
                }

                ZoomPreference.sensorRect = cameraManager.sensorRect(id)

                cameraOpenMutex.lockSelf()
                pendingException?.let { throw it }
                currentSettings = settings
            }
        }
    }

    override suspend fun startSession() = coroutineScope {
        debug { "#startSession" }
        withContext(cameraDispatcher) {
            commonLock.withLock {
                val cameraDevice = cameraDevice

                pendingException?.let { throw it }
                check(captureSession == null) { "session is not closed" }
                check(cameraDevice != null) { "camera is not opened" }

                val surfaces = currentSettings.surfaces.map { cameraSurface ->
                    yield()

                    val format = cameraSurface.format
                    val outputResolutions = when (format) {
                        is ReflektFormat.Image -> cameraManager.outputResolutions(
                            cameraDevice.id, format.format
                        )
                        is ReflektFormat.Clazz -> cameraManager.outputResolutions(
                            cameraDevice.id, format.clazz
                        )
                    }

                    val hardwareRotation = hardwareRotationOf(cameraManager.hardwareRotation(cameraDevice.id))

                    val surfaceConfig = SurfaceConfig(
                        outputResolutions,
                        currentSettings.aspectRatio,
                        currentSettings.displayRotation,
                        hardwareRotation
                    )

                    async {
                        withTimeout(5000) {
                            cameraSurface.acquireSurface(surfaceConfig)
                        }
                    }
                }.map { it.await() }

                captureSession = cameraDevice.createCaptureSession(surfaces.map { it.surface }, handlerThread)
                currentSurfaces = surfaces
            }
        }
    }

    override suspend fun startPreview() = coroutineScope {
        debug { "#startPreview" }
        withContext(cameraDispatcher) {
            val device = cameraDevice
            val session = captureSession
            val surfaces = currentSurfaces
            check(device != null) { "camera is not opened" }
            check(session != null) { "session is not started" }
            check(surfaces != null)

            device.createCaptureRequest(CameraDevice.TEMPLATE_PREVIEW).let { requestBuilder ->
                requestBuilder.addAllSurfaces(surfaces.byType(CameraMode.PREVIEW))
                cameraPreferences.forEach {
                    with(it) { requestBuilder.apply(CameraMode.PREVIEW) }
                }
                session.setRepeatingRequest(requestBuilder.build())
            }
        }
    }

    override suspend fun capture() {
        TODO("not implemented") //To change body of created functions use File | Settings | File Templates.
    }

    override suspend fun startRecord() {
        TODO("not implemented") //To change body of created functions use File | Settings | File Templates.
    }

    override suspend fun stopRecord() {
        TODO("not implemented") //To change body of created functions use File | Settings | File Templates.
    }

    override suspend fun stopPreview() = coroutineScope {
        debug { "#stopPreview" }
        withContext(cameraDispatcher) {
            captureSession?.abortCaptures()
            captureSession?.stopRepeating()
            Unit
        }
    }

    override suspend fun stopSession() = coroutineScope {
        debug { "#stopSession" }
        withContext(cameraDispatcher) {
            captureSession?.stopRepeating()
            captureSession?.abortCaptures()
            captureSession?.close()
            captureSession = null
            currentSurfaces = null
        }
    }

    override suspend fun close() = coroutineScope {
        debug { "#close" }
        withContext(cameraDispatcher) {
            captureSession?.stopRepeating()
            captureSession?.abortCaptures()
            captureSession?.close()
            cameraDevice?.close()
            captureSession = null
            currentSurfaces = null
        }
    }

//    override suspend fun previewAspectRatio(aspectRatio: AspectRatio) = coroutineScope {
//        debug { "#aspectRatio" }
//        withContext(cameraDispatcher) {
//            val device = cameraDevice
//            check(device != null) { "camera is not opened" }
//            val currentSettings = settingsProvider.currentSettings
//
//            if (currentSettings.previewAspectRatio == aspectRatio) return@withContext
//
//            val shouldStartSession = currentSettings.sessionActive
//            val shouldStartPreview = currentSettings.previewActive
//            stopPreview()
//            stopSession()
//            settingsProvider.previewAspectRation(aspectRatio)
//            if (shouldStartSession) {
//                startSession()
//                if (shouldStartPreview) {
//                    startPreview()
//                }
//            }
//        }
//    }

//    override suspend fun availablePreviewAspectRatios(): List<AspectRatio> = coroutineScope {
//        debug { "#availablePreviewAspectRatios" }
//        AspectRatio.values().toList() // FIXME
//    }

//    override suspend fun lens(lensDirect: LensDirect) {
//        debug { "#lensDirect" }
//        withContext(cameraDispatcher) {
//            val device = cameraDevice
//            check(device != null) { "camera is not opened" }
//            val currentSettings = settingsProvider.currentSettings
//
//            if (currentSettings.lensDirect == lensDirect) return@withContext
//
//            val shouldStartSession = currentSettings.sessionActive
//            val shouldStartPreview = currentSettings.previewActive
//            stopPreview()
//            stopSession()
//            close()
//            settingsProvider.lens(lensDirect)
//            open()
//            if (shouldStartSession) {
//                startSession()
//                if (shouldStartPreview) {
//                    startPreview()
//                }
//            }
//        }
//    }

//    override suspend fun availableLenses(): List<LensDirect> = coroutineScope {
//        debug { "#availablePreviewAspectRatios" }
//        cameraManager.cameraIdList.map { cameraManager.directCamera(it) }
//}

//    override suspend fun flash(flashMode: FlashMode) {
//        debug { "#flash" }
//        withContext(cameraDispatcher) {
//            val device = cameraDevice
//            check(device != null) { "camera is not opened" }
//            val currentSettings = settingsProvider.currentSettings
//
//            if (currentSettings.flashMode == flashMode) return@withContext
//
//            val shouldStartPreview = currentSettings.previewActive
//            stopPreview()
//            settingsProvider.flash(flashMode)
//            if (shouldStartPreview) {
//                startPreview()
//            }
//        }
//    }

//    override suspend fun availableFlashModes(): List<FlashMode> = coroutineScope {
//        debug { "#availableFlashModes" }
//        val lens = settingsProvider.currentSettings.lensDirect
//        val availableFlash = cameraManager.availableFlash(cameraManager.findCameraByLens(lens))
//        when {
//            availableFlash && lens == LensDirect.BACK -> FlashMode.values().toList()
//            else -> listOf(FlashMode.OFF)
//        }
//    }

//    override suspend fun maxZoom(): Float {
//        debug { "#maxZoom" }
//        val lens = settingsProvider.currentSettings.lensDirect
//        return cameraManager.availableMaxZoom(cameraManager.findCameraByLens(lens))
//    }

//    override suspend fun zoom(zoom: Float) {
//        debug { "#zoom" }
//        withContext(cameraDispatcher) {
//            val device = cameraDevice
//            check(device != null) { "camera is not opened" }
//            val currentSettings = settingsProvider.currentSettings
//
//            val maxZoom = cameraManager.availableMaxZoom(cameraManager.findCameraByLens(currentSettings.lensDirect))
//            require(zoom <= maxZoom) { "zoom more max zoom" }
//
//            val shouldStartPreview = currentSettings.previewActive
//            settingsProvider.zoom(zoom)
//            if (shouldStartPreview) {
//                startPreview()
//            }
//        }
//    }
}
