package tech.khana.reflekt.core

import android.hardware.camera2.CaptureRequest
import android.view.Surface
import tech.khana.reflekt.models.*

interface Reflekt {

    suspend fun previewAspectRatio(aspectRatio: AspectRatio)

    suspend fun lens(lensDirect: LensDirect)

    suspend fun flash(flashMode: FlashMode)

    suspend fun zoom(zoom: Float)

    suspend fun release(): Any
}

interface ReflektCamera {

    suspend fun open(lensDirect: LensDirect)

    suspend fun startSession(
        reflektSurfaces: List<ReflektSurface>,
        displayRotation: Rotation,
        aspectRatio: AspectRatio
    )

    suspend fun startPreview()

    suspend fun capture()

    suspend fun startRecord()

    suspend fun stopSession()

    suspend fun stopPreview()

    suspend fun stopRecord()

    suspend fun close()
}

interface CameraPreference {

    fun CaptureRequest.Builder.apply(cameraMode: CameraMode)
}

interface ReflektSurface {

    val format: ReflektFormat

    val supportedModes: Set<CameraMode>

    suspend fun acquireSurface(config: SurfaceConfig): Surface?

    suspend fun onStart(cameraMode: CameraMode) {
    }

    suspend fun onStop(cameraMode: CameraMode) {
    }

    suspend fun release() {
    }
}
