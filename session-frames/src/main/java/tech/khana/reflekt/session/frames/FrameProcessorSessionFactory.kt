package tech.khana.reflekt.session.frames

import android.os.Handler
import android.view.Surface
import kotlinx.coroutines.CoroutineScope
import tech.khana.reflekt.api.Session
import tech.khana.reflekt.api.SessionFactory
import tech.khana.reflekt.api.models.CameraMode


class FrameProcessorSessionFactory(
    private val scope: CoroutineScope,
    private val handler: Handler
) : SessionFactory {

    override fun invoke(
        surfaces: Map<CameraMode, List<Surface>>
    ): Session = FrameProcessorSession(
        scope = scope,
        surfaces = surfaces,
        handler = handler
    )
}
