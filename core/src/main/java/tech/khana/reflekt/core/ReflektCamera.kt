package tech.khana.reflekt.core

import android.Manifest.permission.CAMERA
import android.content.Context
import android.hardware.camera2.CameraCaptureSession
import android.hardware.camera2.CameraDevice
import android.hardware.camera2.CameraDevice.TEMPLATE_PREVIEW
import android.hardware.camera2.CameraDevice.TEMPLATE_STILL_CAPTURE
import android.hardware.camera2.CaptureResult
import android.os.Handler
import android.os.HandlerThread
import android.support.v4.content.ContextCompat
import android.support.v4.content.PermissionChecker.PERMISSION_GRANTED
import kotlinx.coroutines.*
import kotlinx.coroutines.android.asCoroutineDispatcher
import kotlinx.coroutines.sync.Mutex
import tech.khana.reflekt.ext.*
import tech.khana.reflekt.models.*
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
    private var currentSurfaces: List<CameraSurface>? = null

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

            val surfaces = reflektSurfaces.map { cameraSurface ->
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

                val surfaceConfig = SurfaceConfig(
                    outputResolutions,
                    aspectRatio,
                    displayRotation,
                    hardwareRotation,
                    cameraManager.directCamera(cameraDevice.id)
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

    override suspend fun startPreview() = coroutineScope {
        debug { "#startPreview" }
        withContext(cameraDispatcher) {
            val session = captureSession
            val surfaces = currentSurfaces
            pendingException?.let { throw it }
            check(cameraDevice != null) { "camera is not opened" }
            check(session != null) { "session is not started" }
            check(surfaces != null)
            val previewSurfaces = surfaces.byType(CameraMode.PREVIEW)
            check(previewSurfaces.isNotEmpty()) { "preview surfaces is empty" }

            val request = session.device.createCaptureRequest(TEMPLATE_PREVIEW).run {
                addAllSurfaces(previewSurfaces)
                cameraPreferences.forEach { with(it) { apply(CameraMode.PREVIEW) } }
                build()
            }
            session.setRepeatingRequest(request)
        }
    }

    override suspend fun capture() = coroutineScope {
        withContext(cameraDispatcher) {
            val session = captureSession
            val surfaces = currentSurfaces
            pendingException?.let { throw it }
            check(cameraDevice != null) { "camera is not opened" }
            check(session != null) { "session is not started" }
            check(surfaces != null)
            val previewSurfaces = surfaces.byType(CameraMode.PREVIEW)
            val captureSurfaces = surfaces.byType(CameraMode.CAPTURE)
            check(previewSurfaces.isNotEmpty()) { "preview surfaces is empty" } // FIXME
            check(captureSurfaces.isNotEmpty()) { "capture surfaces is empty" }

            if (cameraManager.supportFocus(session.device.id)) {
                var focusState = -1
                while (focusState != CaptureResult.CONTROL_AF_STATE_FOCUSED_LOCKED) {
                    focusState = session.lockFocus(handlerThread, previewSurfaces)
                }
            }

            if (cameraManager.supportExposure(session.device.id)) {
                var exposureState = -1
                while (exposureState != CaptureResult.CONTROL_AE_STATE_CONVERGED) {
                    exposureState = session.preCaptureExposure(handlerThread, previewSurfaces)
                }
            }

            val request = session.device.createCaptureRequest(TEMPLATE_STILL_CAPTURE).run {
                addAllSurfaces(captureSurfaces)
                cameraPreferences.forEach { with(it) { apply(CameraMode.CAPTURE) } }
                build()
            }
            session.capture(request, handlerThread)

            if (cameraManager.supportFocus(session.device.id)) {
                session.unLockFocus(handlerThread, previewSurfaces)
            }

            Unit
        }
    }

    override suspend fun startRecord() {
        TODO("not implemented") //To change body of created functions use File | Settings | File Templates.
    }

    override suspend fun stopRecord() {
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
            stopPreview()
            stopRecord()
            captureSession?.close()
            captureSession = null
            currentSurfaces = null
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
