package tech.khana.reflekt.core

import android.hardware.camera2.CaptureRequest
import android.location.Location
import android.view.Surface
import tech.khana.reflekt.models.*

interface Reflekt {

    suspend fun previewAspectRatio(aspectRatio: AspectRatio)

    suspend fun lens(lensDirect: LensDirect)

    suspend fun flash(flashMode: FlashMode)

    suspend fun zoom(zoom: Float)

    suspend fun location(location: Location)

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

    suspend fun trigger3A()

    suspend fun lock3A()

    suspend fun unlock3A()

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

typealias Reducer<STATE, EVENT> = (STATE, EVENT) -> STATE
