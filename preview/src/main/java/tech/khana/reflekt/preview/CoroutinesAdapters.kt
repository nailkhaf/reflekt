package tech.khana.reflekt.preview

import android.graphics.SurfaceTexture
import android.view.TextureView
import tech.khana.reflekt.core.Resolution
import kotlin.coroutines.resume
import kotlin.coroutines.suspendCoroutine

internal suspend fun TextureView.onSurfaceTextureAvailable() =
    suspendCoroutine<TextureData> { continuation ->

        if (isAvailable) {
            val resolution = Resolution(width, height)
            continuation.resume(TextureData(resolution, surfaceTexture))
            return@suspendCoroutine
        }

        surfaceTextureListener = object : TextureView.SurfaceTextureListener {

            override fun onSurfaceTextureAvailable(
                surface: SurfaceTexture, width: Int, height: Int
            ) {
                val resolution = Resolution(width, height)
                continuation.resume(TextureData(resolution, surface))
            }

            override fun onSurfaceTextureSizeChanged(
                surface: SurfaceTexture, width: Int, height: Int
            ) {
            }

            override fun onSurfaceTextureUpdated(surface: SurfaceTexture) {
            }

            override fun onSurfaceTextureDestroyed(surface: SurfaceTexture): Boolean = false
        }
    }
