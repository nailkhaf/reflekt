package tech.khana.reflekt.core

import android.content.Context
import android.hardware.camera2.CameraCaptureSession
import android.hardware.camera2.CameraDevice
import android.os.Handler
import android.os.HandlerThread
import kotlinx.coroutines.android.asCoroutineDispatcher
import kotlinx.coroutines.async
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.withContext
import kotlinx.coroutines.yield
import tech.khana.reflekt.ext.*
import tech.khana.reflekt.models.*
import tech.khana.reflekt.utils.Logger
import tech.khana.reflekt.utils.debug

class ReflektCameraImpl(
    ctx: Context,
    settings: Settings,
    private val handlerThread: HandlerThread = HandlerThread("").apply { start() },
    private val settingsProvider: SettingsProvider = SettingsProviderImpl(settings.toReflektSettings()),
    private val requestFactory: RequestFactory = RequestFactoryImpl(ctx.cameraManager, settingsProvider)
) : ReflektCamera, Logger by Logger.defaultLogger {

    private val cameraManager = ctx.cameraManager

    private val cameraDispatcher = Handler(handlerThread.looper).asCoroutineDispatcher("")

    private var cameraDevice: CameraDevice? = null
    private var captureSession: CameraCaptureSession? = null
    private var currentSurfaces: List<CameraSurface>? = null

    override suspend fun open() {
        debug { "#open" }
        withContext(cameraDispatcher) {

            check(cameraDevice == null) { "camera already is opened" }

            val currentSettings = settingsProvider.currentSettings

            val id = cameraManager.findCameraByLens(currentSettings.lensDirect)

            settingsProvider.supportLevel(cameraManager.supportedLevel(id))
            debug { "supported level=${currentSettings.supportLevel.description}" }

            cameraDevice = cameraManager.openCamera(id, handlerThread)

            settingsProvider.sensorRect(cameraManager.sensorRect(id))
        }
    }

    override suspend fun startSession() = coroutineScope {
        debug { "#startSession" }
        withContext(cameraDispatcher) {
            val device = cameraDevice
            val currentSettings = settingsProvider.currentSettings
            check(device != null) { "camera is not opened" }
            check(currentSettings.sessionActive.not()) { "session is active" }
            check(currentSettings.previewActive.not()) { "preview is active" }

            val asyncSurfaces = currentSettings.surfaces.map { cameraSurface ->
                yield()

                val format = cameraSurface.format
                val outputResolutions = when (format) {
                    is ReflektFormat.Image -> cameraManager.outputResolutions(
                        device.id, format.format
                    )
                    is ReflektFormat.Clazz -> cameraManager.outputResolutions(
                        device.id, format.clazz
                    )
                }

                val hardwareRotation =
                    hardwareRotationOf(cameraManager.hardwareRotation(device.id))

                val surfaceConfig = SurfaceConfig(
                    outputResolutions,
                    currentSettings.previewAspectRatio,
                    currentSettings.displayRotation,
                    hardwareRotation
                )

                async {
                    val typedSurface = cameraSurface.acquireSurface(surfaceConfig)

                    require(cameraManager.surfaceSupported(device.id, typedSurface.surface))
                    { "surface is not supported" }

                    typedSurface
                }
            }

            val surfaces = asyncSurfaces.map { it.await() }
            captureSession = device.createCaptureSession(surfaces.map { it.surface }, handlerThread)
            currentSurfaces = surfaces
            settingsProvider.sessionActive(true)
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
            settingsProvider.sessionActive(false)
        }
    }

    override suspend fun startPreview() = coroutineScope {
        debug { "#startPreview" }
        withContext(cameraDispatcher) {
            val device = cameraDevice
            val session = captureSession
            val surfaces = currentSurfaces
            val currentSettings = settingsProvider.currentSettings
            check(device != null) { "camera is not opened" }
            check(session != null) { "session is not started" }
            check(surfaces != null)

            with(requestFactory) {
                val previewRequest = device.createPreviewRequest {
                    addAllSurfaces(surfaces.byType(CameraMode.PREVIEW))
                }
                session.setRepeatingRequest(previewRequest)
            }
            settingsProvider.previewActive(true)
        }
    }

    override suspend fun stopPreview() = coroutineScope {
        debug { "#stopPreview" }
        withContext(cameraDispatcher) {
            captureSession?.abortCaptures()
            captureSession?.stopRepeating()
            settingsProvider.previewActive(false)
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

    override suspend fun close() = coroutineScope {
        debug { "#close" }
        withContext(cameraDispatcher) {
            captureSession?.stopRepeating()
            captureSession?.abortCaptures()
            captureSession?.close()
            cameraDevice?.close()
            captureSession = null
            currentSurfaces = null
            cameraDevice = null
        }
    }

    override suspend fun previewAspectRatio(aspectRatio: AspectRatio) = coroutineScope {
        debug { "#aspectRatio" }
        withContext(cameraDispatcher) {
            val device = cameraDevice
            check(device != null) { "camera is not opened" }
            val currentSettings = settingsProvider.currentSettings

            if (currentSettings.previewAspectRatio == aspectRatio) return@withContext

            val shouldStartSession = currentSettings.sessionActive
            val shouldStartPreview = currentSettings.previewActive
            stopPreview()
            stopSession()
            settingsProvider.previewAspectRation(aspectRatio)
            if (shouldStartSession) {
                startSession()
                if (shouldStartPreview) {
                    startPreview()
                }
            }
        }
    }

    override suspend fun availablePreviewAspectRatios(): List<AspectRatio> = coroutineScope {
        debug { "#availablePreviewAspectRatios" }
        AspectRatio.values().toList() // FIXME
    }

    override suspend fun lens(lensDirect: LensDirect) {
        debug { "#lensDirect" }
        withContext(cameraDispatcher) {
            val device = cameraDevice
            check(device != null) { "camera is not opened" }
            val currentSettings = settingsProvider.currentSettings

            if (currentSettings.lensDirect == lensDirect) return@withContext

            val shouldStartSession = currentSettings.sessionActive
            val shouldStartPreview = currentSettings.previewActive
            stopPreview()
            stopSession()
            close()
            settingsProvider.lens(lensDirect)
            open()
            if (shouldStartSession) {
                startSession()
                if (shouldStartPreview) {
                    startPreview()
                }
            }
        }
    }

    override suspend fun availableLenses(): List<LensDirect> = coroutineScope {
        debug { "#availablePreviewAspectRatios" }
        cameraManager.cameraIdList.map { cameraManager.directCamera(it) }
    }

    override suspend fun flash(flashMode: FlashMode) {
        debug { "#flash" }
        withContext(cameraDispatcher) {
            val device = cameraDevice
            check(device != null) { "camera is not opened" }
            val currentSettings = settingsProvider.currentSettings

            if (currentSettings.flashMode == flashMode) return@withContext

            val shouldStartPreview = currentSettings.previewActive
            stopPreview()
            settingsProvider.flash(flashMode)
            if (shouldStartPreview) {
                startPreview()
            }
        }
    }

    override suspend fun availableFlashModes(): List<FlashMode> = coroutineScope {
        debug { "#availableFlashModes" }
        val lens = settingsProvider.currentSettings.lensDirect
        val availableFlash = cameraManager.availableFlash(cameraManager.findCameraByLens(lens))
        when {
            availableFlash && lens == LensDirect.BACK -> FlashMode.values().toList()
            else -> listOf(FlashMode.OFF)
        }
    }

    override suspend fun maxZoom(): Float {
        debug { "#maxZoom" }
        val lens = settingsProvider.currentSettings.lensDirect
        return cameraManager.availableMaxZoom(cameraManager.findCameraByLens(lens))
    }

    override suspend fun zoom(zoom: Float) {
        debug { "#zoom" }
        withContext(cameraDispatcher) {
            val device = cameraDevice
            check(device != null) { "camera is not opened" }
            val currentSettings = settingsProvider.currentSettings

            val maxZoom = cameraManager.availableMaxZoom(cameraManager.findCameraByLens(currentSettings.lensDirect))
            require(zoom <= maxZoom) { "zoom more max zoom" }

            val shouldStartPreview = currentSettings.previewActive
            settingsProvider.zoom(zoom)
            if (shouldStartPreview) {
                startPreview()
            }
        }
    }
}
