package tech.khana.reflekt.preview

import android.graphics.SurfaceTexture
import android.view.TextureView
import kotlin.coroutines.resume
import kotlin.coroutines.suspendCoroutine

// FIXME
internal suspend fun TextureView.onSurfaceTextureAvailable() =
    suspendCoroutine<SurfaceTexture> { continuation ->

        if (isAvailable) {
            continuation.resume(surfaceTexture)
            return@suspendCoroutine
        }

        surfaceTextureListener = object : TextureView.SurfaceTextureListener {

            override fun onSurfaceTextureAvailable(
                surface: SurfaceTexture, width: Int, height: Int
            ) {
                continuation.resume(surface)
            }

            override fun onSurfaceTextureSizeChanged(
                surface: SurfaceTexture, width: Int, height: Int
            ) {
            }

            override fun onSurfaceTextureUpdated(surface: SurfaceTexture) {
            }

            override fun onSurfaceTextureDestroyed(surface: SurfaceTexture): Boolean = true
        }
    }
