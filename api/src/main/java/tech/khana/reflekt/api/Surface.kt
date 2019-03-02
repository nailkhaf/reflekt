package tech.khana.reflekt.api

import android.view.Surface
import kotlinx.coroutines.channels.ReceiveChannel
import tech.khana.reflekt.api.models.CameraMode
import tech.khana.reflekt.api.models.SurfaceConfig
import tech.khana.reflekt.api.models.SurfaceFormat

interface Surface {

    val format: SurfaceFormat

    val modes: Set<CameraMode>

    suspend fun acquireSurface(surfaceConfig: SurfaceConfig): ReceiveChannel<Surface>

    suspend fun release()
}