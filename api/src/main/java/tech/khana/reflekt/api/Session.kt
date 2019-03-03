package tech.khana.reflekt.api

import android.hardware.camera2.CameraCaptureSession

interface Session {

    suspend fun startPreview()

    suspend fun stopPreview()

    suspend fun startRecording()

    suspend fun stopRecording()

    suspend fun takePicture()
}

abstract class CaptureSession : Session, AutoCloseable,
    CameraCaptureSession.StateCallback()
