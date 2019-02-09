package tech.khana.reflekt.core

import android.hardware.camera2.CaptureRequest
import android.view.Surface
import tech.khana.reflekt.models.*
import tech.khana.reflekt.models.ReflektFormat.Image.Jpeg
import tech.khana.reflekt.models.ReflektFormat.Image.Yuv
import tech.khana.reflekt.models.ReflektFormat.Priv

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
        displayResolution: Resolution,
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

interface JpegSurface : ReflektSurface {

    override val format: Jpeg
}

interface YuvSurface : ReflektSurface {

    override val format: Yuv
}

interface PrivSurface : ReflektSurface {

    override val format: Priv
}

interface WatcherSurface : ReflektSurface {

    override val format: ReflektFormat
        get() = ReflektFormat.None

    override val supportedModes: Set<CameraMode>
        get() = CameraMode.values().toSet()

    override suspend fun acquireSurface(config: SurfaceConfig): Surface? = null
}

interface SurfaceOutputConfigurator {

    fun defineOutputType(surface: ReflektSurface): OutputType
}
