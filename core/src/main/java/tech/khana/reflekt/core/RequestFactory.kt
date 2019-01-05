package tech.khana.reflekt.core

import android.hardware.camera2.*
import android.os.Build
import android.support.annotation.RequiresApi

internal class RequestFactory(
    private val cameraManager: CameraManager
) {

    var flashMode: FlashMode = FlashMode.OFF

    fun createRequest(
        type: SurfaceType,
        cameraDevice: CameraDevice,
        block: CaptureRequest.Builder.() -> Unit = {}
    ): CaptureRequest = when (type) {
        SurfaceType.PREVIEW -> createPreviewRequest(cameraDevice, block)
        SurfaceType.CAPTURE -> createStillRequest(cameraDevice, block)
    }

    private fun createPreviewRequest(
        cameraDevice: CameraDevice,
        block: CaptureRequest.Builder.() -> Unit = {}
    ) = cameraDevice.createCaptureRequest(CameraDevice.TEMPLATE_PREVIEW)
        .apply {
            // val cameraCharacteristics = cameraManager.getCameraCharacteristics(cameraDevice.id)
            set(CaptureRequest.CONTROL_MODE, CaptureRequest.CONTROL_MODE_AUTO)
            set(CaptureRequest.FLASH_MODE, flashMode.value)
            block()
        }.build()

    private fun createStillRequest(
        cameraDevice: CameraDevice,
        block: CaptureRequest.Builder.() -> Unit = {}
    ) = cameraDevice.createCaptureRequest(CameraDevice.TEMPLATE_STILL_CAPTURE)
        .apply {
            set(CaptureRequest.CONTROL_MODE, CaptureRequest.CONTROL_MODE_AUTO)
            set(CaptureRequest.FLASH_MODE, flashMode.value)
            block()
        }.build()

    private fun CaptureRequest.Builder.setPreviewSceneMode(cameraCharacteristics: CameraCharacteristics) {
//        val scenes = cameraCharacteristics.get(CameraCharacteristics.CONTROL_AVAILABLE_SCENE_MODES)!!
//        val mode = ControlMode.values().first { modes.contains(it.value) }
//        Log.d(TAG, "AE: ${mode.desc}")
//        set(CaptureRequest.CONTROL_MODE, mode.value)
    }

    @RequiresApi(Build.VERSION_CODES.M)
    private fun CaptureRequest.Builder.setPreviewControlMode(cameraCharacteristics: CameraCharacteristics) {
        val modes = cameraCharacteristics.get(CameraCharacteristics.CONTROL_AVAILABLE_MODES)!!
        val mode = ControlMode.values().first { modes.contains(it.value) }
//        Log.d(TAG, "AE: ${mode.desc}")
        set(CaptureRequest.CONTROL_MODE, mode.value)
    }

    private fun CaptureRequest.Builder.setPreviewAfMode(cameraCharacteristics: CameraCharacteristics) {
        val afModes = cameraCharacteristics.get(CameraCharacteristics.CONTROL_AF_AVAILABLE_MODES)!!
        val afMode = AutoFocusMode.values().first { afModes.contains(it.value) }
//        Log.d(TAG, "AF: ${afMode.desc}")
        set(CaptureRequest.CONTROL_AF_MODE, afMode.value)
    }

    private fun CaptureRequest.Builder.setPreviewAeMode(cameraCharacteristics: CameraCharacteristics) {
        val aeModes = cameraCharacteristics.get(CameraCharacteristics.CONTROL_AE_AVAILABLE_MODES)!!
        val aeMode = AutoExposureMode.values().first { aeModes.contains(it.value) }
//        Log.d(TAG, "AE: ${aeMode.desc}")
        set(CaptureRequest.CONTROL_AE_MODE, aeMode.value)
    }

    private fun CaptureRequest.Builder.setPreviewAntibandingMode(cameraCharacteristics: CameraCharacteristics) {
        val aeAntiBandingModes =
            cameraCharacteristics.get(CameraCharacteristics.CONTROL_AE_AVAILABLE_ANTIBANDING_MODES)!!
        val aeAntiBandingMode =
            AntiBandingMode.values().first { aeAntiBandingModes.contains(it.value) }
//        Log.d(TAG, "AE ANTIBANDING: ${aeAntiBandingMode.desc}")
        set(CaptureRequest.CONTROL_AE_MODE, aeAntiBandingMode.value)
    }

    private enum class ControlMode(val value: Int, val desc: String) {
        AUTO(CameraMetadata.CONTROL_MODE_AUTO, "CONTROL_MODE_AUTO"),
        SCENE(CameraMetadata.CONTROL_MODE_USE_SCENE_MODE, "CONTROL_MODE_USE_SCENE_MODE"),
        KEEP_STATE(CameraMetadata.CONTROL_MODE_OFF_KEEP_STATE, "CONTROL_MODE_OFF_KEEP_STATE"),
        OFF(CameraMetadata.CONTROL_MODE_OFF, "CONTROL_MODE_OFF"),
    }

    private enum class AutoFocusMode(val value: Int, val desc: String) {
        PICTURE(
            CameraMetadata.CONTROL_AF_MODE_CONTINUOUS_PICTURE,
            "CONTROL_AF_MODE_CONTINUOUS_PICTURE"
        ),
        AUTO(CameraMetadata.CONTROL_AF_MODE_AUTO, "CONTROL_AF_MODE_AUTO"),
        OFF(CameraMetadata.CONTROL_AF_MODE_OFF, "CONTROL_AF_MODE_OFF"),
//        EDOF(CameraMetadata.CONTROL_AF_MODE_EDOF),
    }

    private enum class AutoExposureMode(val value: Int, val desc: String) {
        ON(CameraMetadata.CONTROL_AE_MODE_ON, "CONTROL_AE_MODE_ON"),
    }

    @Suppress("EnumEntryName")
    private enum class AntiBandingMode(val value: Int, val desc: String) {
        AUTO(CameraMetadata.CONTROL_AE_ANTIBANDING_MODE_AUTO, "CONTROL_AE_MODE_ON"),
        _50HZ(CameraMetadata.CONTROL_AE_ANTIBANDING_MODE_50HZ, "CONTROL_AE_ANTIBANDING_MODE_50HZ"),
        _60HZ(CameraMetadata.CONTROL_AE_ANTIBANDING_MODE_60HZ, "CONTROL_AE_ANTIBANDING_MODE_60HZ"),
        OFF(CameraMetadata.CONTROL_AE_ANTIBANDING_MODE_OFF, "CONTROL_AE_ANTIBANDING_MODE_OFF"),
    }
}