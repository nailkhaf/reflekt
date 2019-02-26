package tech.khana.reflekt.session.frames

import android.view.Surface
import kotlinx.coroutines.CoroutineScope
import tech.khana.reflekt.api.Session
import tech.khana.reflekt.api.SessionFactory
import tech.khana.reflekt.api.models.CameraMode


class FrameProcessorSessionFactory(
    private val scope: CoroutineScope
) : SessionFactory {

    override fun invoke(
        surfaces: Map<CameraMode, List<Surface>>
    ): Session = FrameProcessorSession(
        scope = scope,
        surfaces = surfaces
    )
}
