package tech.khana.reflekt.preview

import android.content.Context
import android.graphics.Matrix
import android.graphics.SurfaceTexture
import android.util.AttributeSet
import android.view.Surface
import android.view.TextureView
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.withContext
import tech.khana.reflekt.core.*

const val MAX_PREVIEW_WIDTH = 1920
const val MAX_PREVIEW_HEIGHT = 1080

class ReflektPreview constructor(
    ctx: Context,
    attrs: AttributeSet
) : TextureView(ctx, attrs), ReflektSurface {

    override val format: ReflektFormat = ReflektFormat.Clazz.Texture

    override suspend fun acquireSurface(config: SurfaceConfig): TypedSurface = coroutineScope {
        val previewResolution = choseOptimalResolution(config.resolutions)
        withContext(Dispatchers.Main) {
            val textureData = onSurfaceTextureAvailable()
            configureTransform(textureData.resolution, previewResolution, config.rotation)
            TypedSurface(SurfaceType.PREVIEW, Surface(textureData.surfaceTexture))
        }
    }

    private fun choseOptimalResolution(resolutions: List<Resolution>): Resolution =
        resolutions.filter { it.ratio == 16f / 9f }.sortedBy { it.area }.last()
            .let { res ->
                if (res.width > MAX_PREVIEW_WIDTH)
                    Resolution(MAX_PREVIEW_WIDTH, MAX_PREVIEW_HEIGHT)
                else res
            }

    private fun configureTransform(
        screenResolution: Resolution,
        previewResolution: Resolution,
        rotation: Rotation
    ) {
        setTransform(Matrix().apply {

            val centerX = screenResolution.width * 0.5f
            val centerY = screenResolution.height * 0.5f

            when (rotation) {
                Rotation._0 -> {
                }
                Rotation._90 -> {
                    val scale =
                        previewResolution.width.toFloat() / previewResolution.height.toFloat()
                    postScale(1 / scale, scale, centerX, centerY)
                    postRotate(270f, centerX, centerY)
                }
                Rotation._180 -> {
                    postRotate(180f, centerX, centerY)
                }
                Rotation._270 -> {
                    val scale =
                        previewResolution.width.toFloat() / previewResolution.height.toFloat()
                    postScale(1 / scale, scale, centerX, centerY)
                    postRotate(90f, centerX, centerY)
                }
            }

            val tempRatio = screenResolution.width.toFloat() / screenResolution.height
            val screenWidth = if (tempRatio > 1) screenResolution.width else screenResolution.height
            val screenHeight =
                if (tempRatio > 1) screenResolution.height else screenResolution.width
            val screenAspectRatio: Float = screenWidth.toFloat() / screenHeight.toFloat()
            val previewAspectRatio: Float =
                previewResolution.width.toFloat() / previewResolution.height.toFloat()

            val scaleFactor: Float = screenAspectRatio / previewAspectRatio

            val heightCorrection = when (rotation) {
                Rotation._0, Rotation._180 -> (screenHeight.toFloat() * scaleFactor - screenHeight.toFloat()) / 2f
                Rotation._90, Rotation._270 -> (screenWidth.toFloat() * scaleFactor - screenWidth.toFloat()) / 2f
            }

            postScale(scaleFactor, 1f)
            postTranslate(-heightCorrection, 0f)

            if (scaleFactor < 1f) {
                postScale(1f / scaleFactor, 1f / scaleFactor)
                postTranslate(heightCorrection, heightCorrection)
            }
        })
    }

//    private inner class TextureListener(
//        val listener: SurfaceListener,
//        val previewResolution: Resolution,
//        val rotation: Rotation
//    ) : TextureView.SurfaceTextureListener {
//
//        override fun onSurfaceTextureAvailable(surface: SurfaceTexture, width: Int, height: Int) {
////            Log.d(TAG, "#onSurfaceTextureAvailable")
//            val screenResolution = Resolution(width, height)
//            surface.setDefaultBufferSize(previewResolution.width, previewResolution.height)
//            configureTransform(screenResolution, previewResolution, rotation)
//            listener(TypedSurface(SurfaceType.PREVIEW, Surface(surface)))
//        }
//
//        override fun onSurfaceTextureSizeChanged(surface: SurfaceTexture, width: Int, height: Int) {
////            Log.d(TAG, "#onSurfaceTextureSizeChanged")
//            val screenResolution = Resolution(width, height)
//            configureTransform(screenResolution, previewResolution, rotation)
//        }
//
//        override fun onSurfaceTextureUpdated(surface: SurfaceTexture) {
////            Log.d(TAG, "#onSurfaceTextureUpdated")
//        }
//
//        override fun onSurfaceTextureDestroyed(surface: SurfaceTexture): Boolean {
////            Log.d(TAG, "#onSurfaceTextureDestroyed")
//            return false
//        }
//    }
}

internal data class TextureData(val resolution: Resolution, val surfaceTexture: SurfaceTexture)

internal val previewLogger = object : Tag {

    override val tag: String = "ReflektPreview"

    override val level: LogLevel = LogLevel.DEFAULT
}