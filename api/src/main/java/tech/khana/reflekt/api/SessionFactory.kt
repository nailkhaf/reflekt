package tech.khana.reflekt.api

import android.view.Surface
import tech.khana.reflekt.api.models.CameraMode

interface SessionFactory {

    operator fun invoke(
        surfaces: Map<CameraMode, List<Surface>>,
        surfaceFeatures: List<FeatureFactory>
    ): CaptureSession
}
