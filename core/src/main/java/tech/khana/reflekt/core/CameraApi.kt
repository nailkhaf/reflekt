package tech.khana.reflekt.core

import android.hardware.camera2.CaptureRequest
import tech.khana.reflekt.models.*

interface Reflekt {

    fun availableLenses(): List<LensDirect>

    fun availableFlashModes(lensDirect: LensDirect): List<FlashMode>

    fun availablePreviewAspectRatios(lensDirect: LensDirect): List<AspectRatio>

    fun maxZoom(lensDirect: LensDirect): Float

    suspend fun previewAspectRatio(aspectRatio: AspectRatio)

    suspend fun lens(lensDirect: LensDirect)

    suspend fun flash(flashMode: FlashMode)

    suspend fun zoom(zoom: Float)
}

interface ReflektCamera {

    suspend fun open(settings: Settings)

    suspend fun startSession()

    suspend fun stopSession()

    suspend fun startPreview()

    suspend fun stopPreview()

    suspend fun capture()

    suspend fun startRecord()

    suspend fun stopRecord()

    suspend fun close()
}

interface CameraPreference {

    fun CaptureRequest.Builder.apply(cameraMode: CameraMode)
}

interface ReflektSurface {

    val format: ReflektFormat

    suspend fun acquireSurface(config: SurfaceConfig): CameraSurface
}
