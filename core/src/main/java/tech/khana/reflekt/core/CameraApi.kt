package tech.khana.reflekt.core

import android.graphics.Rect
import android.hardware.camera2.CameraDevice
import android.hardware.camera2.CaptureRequest
import tech.khana.reflekt.models.*

interface ReflektCamera {

    suspend fun open()

    suspend fun startSession()

    suspend fun stopSession()

    suspend fun startPreview()

    suspend fun stopPreview()

    suspend fun capture()

    suspend fun startRecord()

    suspend fun stopRecord()

    suspend fun close()

    suspend fun previewAspectRatio(aspectRatio: AspectRatio)

    suspend fun availablePreviewAspectRatios(): List<AspectRatio>

    suspend fun lens(lensDirect: LensDirect)

    suspend fun availableLenses(): List<LensDirect>

    suspend fun flash(flashMode: FlashMode)

    suspend fun availableFlashModes(): List<FlashMode>

    suspend fun maxZoom(): Float

    suspend fun zoom(zoom: Float)
}

interface ReflektSurface {

    val format: ReflektFormat

    suspend fun acquireSurface(config: SurfaceConfig): CameraSurface
}

interface SettingsProvider {

    val currentSettings: ReflektSettings

    suspend fun lens(lensDirect: LensDirect)

    suspend fun flash(flashMode: FlashMode)

    suspend fun sessionActive(active: Boolean)

    suspend fun previewActive(active: Boolean)

    suspend fun previewAspectRation(aspectRatio: AspectRatio)

    suspend fun supportLevel(supportLevel: SupportLevel)

    suspend fun zoom(zoom: Float)

    suspend fun sensorRect(sensorRect: Rect)
}

interface RequestFactory {

    fun CameraDevice.createPreviewRequest(block: CaptureRequest.Builder.() -> Unit): CaptureRequest

    fun CameraDevice.createStillRequest(block: CaptureRequest.Builder.() -> Unit): CaptureRequest
}