package tech.khana.reflekt.core

import android.Manifest.permission.CAMERA
import android.content.Context
import android.hardware.camera2.CameraCaptureSession
import android.hardware.camera2.CameraDevice
import android.hardware.camera2.CameraDevice.*
import android.hardware.camera2.CaptureResult
import android.os.Handler
import android.os.HandlerThread
import android.support.v4.content.ContextCompat
import android.support.v4.content.PermissionChecker.PERMISSION_GRANTED
import android.view.Surface
import kotlinx.coroutines.*
import kotlinx.coroutines.android.asCoroutineDispatcher
import kotlinx.coroutines.sync.Mutex
import tech.khana.reflekt.ext.*
import tech.khana.reflekt.models.*
import tech.khana.reflekt.models.CameraMode.*
import tech.khana.reflekt.preferences.JpegPreference
import tech.khana.reflekt.preferences.ZoomPreference
import tech.khana.reflekt.utils.Logger
import tech.khana.reflekt.utils.debug
import tech.khana.reflekt.utils.error
import tech.khana.reflekt.utils.warn

class ReflektCameraImpl(
    private val ctx: Context,
    private val handlerThread: HandlerThread,
    private val cameraPreferences: List<CameraPreference> = listOf(JpegPreference)
) : ReflektCamera, Logger by Logger.defaultLogger {

    private val cameraManager = ctx.cameraManager

    private val cameraDispatcher = Handler(handlerThread.looper).asCoroutineDispatcher("")

    private var cameraDevice: CameraDevice? = null
    private var captureSession: CameraCaptureSession? = null
    private var currentSurfaces: Map<CameraMode, List<Surface>> = emptyMap()
    private var currentReflekts: List<ReflektSurface> = emptyList()

    private var pendingException: CameraException? = null
    private val cameraOpenMutex = Mutex()

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

    override suspend fun open(lensDirect: LensDirect) = coroutineScope {
        debug { "#open" }
        withContext(cameraDispatcher) {
            pendingException?.let { throw it }
            check(captureSession == null) { "session is not closed" }
            check(cameraDevice == null) { "camera already is opened" }

            val id = cameraManager.findCameraByLens(lensDirect)

            ZoomPreference.sensorRect = cameraManager.sensorRect(id)

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

            cameraOpenMutex.lockSelf()
            pendingException?.let { throw it }
            Unit
        }
    }

    override suspend fun startSession(
        reflektSurfaces: List<ReflektSurface>,
        displayRotation: Rotation,
        aspectRatio: AspectRatio
    ) = coroutineScope {
        debug { "#startSession" }
        withContext(cameraDispatcher) {
            val cameraDevice = cameraDevice

            pendingException?.let { throw it }
            require(reflektSurfaces.isNotEmpty()) { "reflektSurfaces is empty" }
            check(cameraDevice != null) { "camera is not opened" }
            check(captureSession == null) { "session is not closed" }

            val hardwareRotation = rotationOf(cameraManager.hardwareRotation(cameraDevice.id))
            JpegPreference.hardwareRotation = hardwareRotation
            JpegPreference.displayRotation = displayRotation

            val surfaces = reflektSurfaces
                .map { reflektSurface ->
                    yield()

                    val outputResolutions = when (val format = reflektSurface.format) {
                        is ReflektFormat.Image -> cameraManager.outputResolutions(
                            cameraDevice.id, format.format
                        )
                        is ReflektFormat.Clazz -> cameraManager.outputResolutions(
                            cameraDevice.id, format.klass
                        )
                    }

                    val surfaceConfig = SurfaceConfig(
                        outputResolutions,
                        aspectRatio,
                        displayRotation,
                        hardwareRotation,
                        cameraManager.directCamera(cameraDevice.id)
                    )

                    async {
                        withTimeout(5000) {
                            reflektSurface to reflektSurface.acquireSurface(surfaceConfig)
                        }
                    }
                }
                .map { it.await() }
                .flatMap { (reflekt, surface) ->
                    reflekt.supportedModes.map { it to surface }
                }
                .groupBy(keySelector = { it.first }, valueTransform = { it.second })

            captureSession = cameraDevice.createCaptureSession(surfaces.values.flatten().distinct(), handlerThread)
            currentSurfaces = surfaces
            currentReflekts = reflektSurfaces
        }
    }

    override suspend fun startPreview() = coroutineScope {
        debug { "#startPreview" }
        withContext(cameraDispatcher) {
            val session = captureSession
            val surfaces = currentSurfaces
            pendingException?.let { throw it }
            check(cameraDevice != null) { "camera is not opened" }
            check(session != null) { "session is not started" }

            val previewSurfaces = surfaces[PREVIEW]
            check(previewSurfaces != null && previewSurfaces.isNotEmpty()) { "preview surfaces is empty" }

            val request = session.device.createCaptureRequest(TEMPLATE_PREVIEW).run {
                addAllSurfaces(previewSurfaces)
                cameraPreferences.forEach { with(it) { apply(PREVIEW) } }
                build()
            }

            session.setRepeatingRequest(request)
            currentReflekts.filter { PREVIEW in it.supportedModes }.forEach { it.onStart(PREVIEW) }
        }
    }

    override suspend fun capture() = coroutineScope {
        withContext(cameraDispatcher) {
            val session = captureSession
            val surfaces = currentSurfaces
            pendingException?.let { throw it }
            check(cameraDevice != null) { "camera is not opened" }
            check(session != null) { "session is not started" }
            val helperSurfaces = surfaces[HELPER]
            val captureSurfaces = surfaces[CAPTURE]
            check(helperSurfaces != null && helperSurfaces.isNotEmpty()) { "preview surfaces is empty" }
            check(captureSurfaces != null && captureSurfaces.isNotEmpty()) { "capture surfaces is empty" }

            currentReflekts.filter { CAPTURE in it.supportedModes }.forEach { it.onStart(CAPTURE) }

            if (cameraManager.supportFocus(session.device.id)) {
                var focusState = -1
                while (focusState != CaptureResult.CONTROL_AF_STATE_FOCUSED_LOCKED) {
                    focusState = session.lockFocus(handlerThread, helperSurfaces)
                }
            }

            if (cameraManager.supportExposure(session.device.id)) {
                var exposureState = -1
                while (exposureState != CaptureResult.CONTROL_AE_STATE_CONVERGED) {
                    exposureState = session.preCaptureExposure(handlerThread, helperSurfaces)
                }
            }

            val request = session.device.createCaptureRequest(TEMPLATE_STILL_CAPTURE).run {
                addAllSurfaces(captureSurfaces)
                cameraPreferences.forEach { with(it) { apply(CAPTURE) } }
                build()
            }

            session.capture(request, handlerThread)

            currentReflekts.filter { CAPTURE in it.supportedModes }.forEach { it.onStop(CAPTURE) }

            if (cameraManager.supportFocus(session.device.id)) {
                session.unLockFocus(handlerThread, helperSurfaces)
            }

            Unit
        }
    }

    override suspend fun startRecord() = coroutineScope {
        debug { "#startRecord" }
        withContext(cameraDispatcher) {
            val session = captureSession
            val surfaces = currentSurfaces
            pendingException?.let { throw it }
            check(cameraDevice != null) { "camera is not opened" }
            check(session != null) { "session is not started" }
            val recordSurfaces = surfaces[RECORD]
            check(recordSurfaces != null && recordSurfaces.isNotEmpty()) { "record surfaces is empty" } // FIXME

            val request = session.device.createCaptureRequest(TEMPLATE_RECORD).run {
                addAllSurfaces(recordSurfaces)
                cameraPreferences.forEach { with(it) { apply(RECORD) } }
                build()
            }

            session.setRepeatingRequest(request)

            currentReflekts.filter { RECORD in it.supportedModes }.forEach { it.onStart(RECORD) }

            Unit
        }
    }

    override suspend fun stopRecord() = coroutineScope {
        debug { "#stopRecord" }
        withContext(cameraDispatcher) {
            captureSession?.abortCaptures()
            captureSession?.stopRepeating()
            currentReflekts.filter { RECORD in it.supportedModes }.forEach { it.onStop(RECORD) }
        }
    }

    override suspend fun stopPreview() = coroutineScope {
        debug { "#stopPreview" }
        withContext(cameraDispatcher) {
            captureSession?.abortCaptures()
            captureSession?.stopRepeating()
            currentReflekts.filter { PREVIEW in it.supportedModes }.forEach { it.onStop(PREVIEW) }
        }
    }

    override suspend fun stopSession() = coroutineScope {
        debug { "#stopSession" }
        withContext(cameraDispatcher) {
            stopPreview()
            stopRecord()
            captureSession?.close()
            captureSession = null
        }
    }

    override suspend fun close() = coroutineScope {
        debug { "#close" }
        withContext(cameraDispatcher) {
            stopSession()
            cameraDevice?.close()
            cameraDevice = null
            Unit
        }
    }
}
