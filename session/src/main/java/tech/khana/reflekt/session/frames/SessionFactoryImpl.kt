package tech.khana.reflekt.session.frames

import android.os.Handler
import android.view.Surface
import kotlinx.coroutines.CoroutineScope
import tech.khana.reflekt.api.CaptureSession
import tech.khana.reflekt.api.FeatureFactory
import tech.khana.reflekt.api.SessionFactory
import tech.khana.reflekt.api.models.CameraMode


class SessionFactoryImpl(
    private val scope: CoroutineScope,
    private val handler: Handler,
    private val features: List<FeatureFactory>
) : SessionFactory {

    override fun invoke(
        surfaces: Map<CameraMode, List<Surface>>,
        surfaceFeatures: List<FeatureFactory>
    ): CaptureSession = CaptureSessionImpl(
        scope = scope,
        surfaces = surfaces,
        handler = handler,
        features = (surfaceFeatures + features)
    )
}
