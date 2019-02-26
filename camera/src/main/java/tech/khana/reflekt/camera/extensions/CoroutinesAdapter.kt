package tech.khana.reflekt.camera.extensions

import android.Manifest
import android.annotation.TargetApi
import android.hardware.camera2.CameraCaptureSession
import android.hardware.camera2.CameraDevice
import android.hardware.camera2.CameraManager
import android.os.Build
import android.os.Handler
import android.support.annotation.RequiresPermission
import android.view.Surface
import tech.khana.reflekt.api.CameraException
import tech.khana.reflekt.api.SessionException
import tech.khana.reflekt.api.cameraExceptionByErrorCode
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException
import kotlin.coroutines.suspendCoroutine

@RequiresPermission(Manifest.permission.CAMERA)
internal suspend fun CameraManager.openCameraDevice(
    id: String,
    callback: CameraDevice.StateCallback,
    handler: Handler
) = suspendCoroutine<CameraDevice> { continuation ->

    var continued = false

    runCatching {
        openCamera(id, object : CameraDevice.StateCallback() {

            override fun onOpened(camera: CameraDevice) {
                callback.onOpened(camera)
                if (continued.not()) continuation.resume(camera)
                continued = true
            }

            override fun onDisconnected(camera: CameraDevice) {
                callback.onDisconnected(camera)
                if (continued.not())
                    continuation.resumeWithException(CameraException.CameraUnknownException())
                continued = true
            }

            override fun onClosed(camera: CameraDevice) {
                callback.onClosed(camera)
                if (continued.not())
                    continuation.resumeWithException(CameraException.CameraUnknownException())
                continued = true
            }

            override fun onError(camera: CameraDevice, error: Int) {
                callback.onError(camera, error)
                if (continued.not())
                    continuation.resumeWithException(cameraExceptionByErrorCode(error))
                continued = true
            }
        }, handler)

    }.onFailure { continuation.resumeWithException(it) }
}

internal suspend fun CameraDevice.createSession(
    outputs: List<Surface>,
    callback: CameraCaptureSession.StateCallback,
    handler: Handler
) = suspendCoroutine<CameraCaptureSession> { continuation ->

    var continued = false

    runCatching {
        createCaptureSession(outputs, object : CameraCaptureSession.StateCallback() {

            override fun onConfigured(session: CameraCaptureSession) {
                callback.onConfigured(session)
                if (continued.not())
                    continuation.resume(session)
                continued = true
            }

            override fun onConfigureFailed(session: CameraCaptureSession) {
                callback.onConfigureFailed(session)
                if (continued.not())
                    continuation.resumeWithException(SessionException.ConfigureFailedException())
                continued = true
            }

            override fun onReady(session: CameraCaptureSession) {
                callback.onReady(session)
                if (continued.not())
                    continuation.resumeWithException(SessionException.SessionUnknownException())
                continued = true
            }

            @TargetApi(Build.VERSION_CODES.O)
            override fun onCaptureQueueEmpty(session: CameraCaptureSession) {
                callback.onCaptureQueueEmpty(session)
                if (continued.not())
                    continuation.resumeWithException(SessionException.SessionUnknownException())
                continued = true
            }

            override fun onClosed(session: CameraCaptureSession) {
                callback.onClosed(session)
                if (continued.not())
                    continuation.resumeWithException(SessionException.SessionUnknownException())
                continued = true
            }

            @TargetApi(Build.VERSION_CODES.M)
            override fun onSurfacePrepared(
                session: CameraCaptureSession, surface: Surface
            ) {
                callback.onSurfacePrepared(session, surface)
                if (continued.not())
                    continuation.resumeWithException(SessionException.SessionUnknownException())
                continued = true
            }

            override fun onActive(session: CameraCaptureSession) {
                callback.onActive(session)
                if (continued.not())
                    continuation.resumeWithException(SessionException.SessionUnknownException())
                continued = true
            }
        }, handler)

    }.onFailure { continuation.resumeWithException(it) }
}
