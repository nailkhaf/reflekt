package tech.khana.reflekt.preview

import android.content.Context
import android.graphics.Matrix
import android.util.AttributeSet
import android.view.Surface
import android.view.TextureView
import android.view.View.MeasureSpec.getSize
import android.view.ViewGroup
import android.view.ViewGroup.LayoutParams.MATCH_PARENT
import android.view.ViewGroup.LayoutParams.WRAP_CONTENT
import android.widget.FrameLayout
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.withContext
import tech.khana.reflekt.core.*
import tech.khana.reflekt.core.AspectRatio.AR_16X9
import tech.khana.reflekt.preview.Side.HEIGHT
import tech.khana.reflekt.preview.Side.WIDTH


const val MAX_PREVIEW_WIDTH = 1920
const val MAX_PREVIEW_HEIGHT = 1080

class ReflektPreview constructor(
    ctx: Context,
    attrs: AttributeSet? = null
) : FrameLayout(ctx, attrs), ReflektSurface {

    private var previewRotation = Rotation._0
    private var previewAspectRatio: AspectRatio = AR_16X9

    override val format: ReflektFormat = ReflektFormat.Clazz.Texture

    private val layoutMutex = Mutex()
    private val textureMatrix = Matrix()
    private val mutableSize: MutableSize = MutableSize()

    private val textureView = TextureView(context).apply {
        layoutParams = ViewGroup.LayoutParams(MATCH_PARENT, MATCH_PARENT)
    }

    init {
        addView(textureView)

        addOnLayoutChangeListener { _, _, _, _, _, _, _, _, _ ->
            previewLogger.debug { "#onLayout" }
            if (layoutMutex.isLocked) {
                layoutMutex.unlock()
            }
        }
    }

    override suspend fun acquireSurface(config: SurfaceConfig): TypedSurface = coroutineScope {
        previewLogger.debug { "#acquireSurface" }
        withContext(Dispatchers.Main) {
            val previewResolution = config.resolutions
                .chooseOptimalResolution(config.previewAspectRatio)
            this@ReflektPreview.previewAspectRatio = config.previewAspectRatio
            previewRotation = config.displayRotation

            requestLayout()
            layoutMutex.twiceLock()

            val surfaceTexture = textureView.onSurfaceTextureAvailable()
            surfaceTexture.setDefaultBufferSize(
                previewResolution.width, previewResolution.height
            )

            previewLogger.debug { "#acquireSurface acquired" }
            TypedSurface(SurfaceType.PREVIEW, Surface(surfaceTexture))
        }
    }

    private fun List<Resolution>.chooseOptimalResolution(aspectRatio: AspectRatio): Resolution =
        asSequence()
            .filter { it.ratio == aspectRatio.value }
            .filter { it.width <= MAX_PREVIEW_WIDTH }
            .filter { it.height <= MAX_PREVIEW_HEIGHT }
            .sortedBy { it.area }
            .last()

    override fun onMeasure(widthMeasureSpec: Int, heightMeasureSpec: Int) {
        previewLogger.debug { "#onMeasure" }

        val width = getSize(widthMeasureSpec)
        val height = getSize(heightMeasureSpec)

        val previewAspectRatio = when (previewRotation) {
            Rotation._0, Rotation._180 -> previewAspectRatio.value
            Rotation._90, Rotation._270 -> 1f / previewAspectRatio.value
        }

        val (newWidth, newHeight) = when {
            layoutParams.width == MATCH_PARENT && layoutParams.height == MATCH_PARENT -> {
                val viewAspectRation = width.toFloat() / height.toFloat()
                if (viewAspectRation <= previewAspectRatio) {
                    onMeasureByMatchParent(width, height, previewAspectRatio, previewRotation, HEIGHT)
                } else {
                    onMeasureByMatchParent(width, height, previewAspectRatio, previewRotation, WIDTH)
                }
            }
            layoutParams.width == WRAP_CONTENT && layoutParams.height == WRAP_CONTENT -> {
                val viewAspectRation = width.toFloat() / height.toFloat()
                if (viewAspectRation >= previewAspectRatio) {
                    onMeasureByMatchParent(width, height, previewAspectRatio, previewRotation, HEIGHT)
                } else {
                    onMeasureByMatchParent(width, height, previewAspectRatio, previewRotation, WIDTH)
                }
            }
            layoutParams.width == WRAP_CONTENT && layoutParams.height == MATCH_PARENT ->
                onMeasureByMatchParent(width, height, previewAspectRatio, previewRotation, HEIGHT)
            layoutParams.width == MATCH_PARENT && layoutParams.height == WRAP_CONTENT ->
                onMeasureByMatchParent(width, height, previewAspectRatio, previewRotation, WIDTH)
            else -> throw IllegalStateException("unknown layout settings")
        }

        super.onMeasure(
            MeasureSpec.makeMeasureSpec(newWidth, MeasureSpec.EXACTLY),
            MeasureSpec.makeMeasureSpec(newHeight, MeasureSpec.EXACTLY)
        )
    }

    private fun onMeasureByMatchParent(
        width: Int, height: Int, aspectRatio: Float, rotation: Rotation, side: Side
    ): MutableSize = when (side) {
        HEIGHT -> {
            val newWidth = (height / aspectRatio).toInt()
            textureView.setTransform(textureMatrix.apply {
                reset()

                if (newWidth > width) {
                    when (rotation) {
                        Rotation._0 -> {
                            postTranslate(-(newWidth - width) / 2f, 0f)
                        }
                        Rotation._90 -> { // FIXME check that image in center
                            postTranslate(0f, -(newWidth - width) / 2f)
                        }
                        Rotation._270 -> { // FIXME check that image in center
                            postTranslate(0f, (newWidth - width) / 2f)
                        }
                        else -> {
                        }
                    }
                }

                when (rotation) { // FIXME 270 -> 90, 90 -> 270 view is not recreated
                    Rotation._90 -> {
                        postRotate(270f, newWidth / 2f, height / 2f)
                        postScale(1f / aspectRatio, aspectRatio, newWidth / 2f, height / 2f)
                    }
                    Rotation._270 -> {
                        postRotate(90f, newWidth / 2f, height / 2f)
                        postScale(1f / aspectRatio, aspectRatio, newWidth / 2f, height / 2f)
                    }
                    else -> {
                    }
                }
            })
            previewLogger.debug {
                "#setMeasuredDimension(newWidth=$newWidth, height=$height)"
            }
            mutableSize.apply {
                this.width = newWidth
                this.height = height
            }
        }
        WIDTH -> {
            val newHeight = (width * aspectRatio).toInt()
            textureView.setTransform(textureMatrix.apply {
                reset()
                if (newHeight > height) {
                    when (rotation) {
                        Rotation._0 -> {
                            postTranslate(0f, -(newHeight - height) / 2f)
                        }
                        Rotation._90 -> { // FIXME check that image in center
                            postTranslate((newHeight - height) / 2f, 0f)
                        }
                        Rotation._270 -> { // FIXME check that image in center
                            postTranslate(-(newHeight - height) / 2f, 0f)
                        }
                        else -> {
                        }
                    }
                }

                when (rotation) { // FIXME 270 -> 90, 90 -> 270 view is not recreated
                    Rotation._90 -> {
                        postRotate(270f, width / 2f, newHeight / 2f)
                        postScale(1f / aspectRatio, aspectRatio, width / 2f, newHeight / 2f)
                    }
                    Rotation._270 -> {
                        postRotate(90f, width / 2f, newHeight / 2f)
                        postScale(1f / aspectRatio, aspectRatio, width / 2f, newHeight / 2f)
                    }
                    else -> {
                    }
                }
            })

            previewLogger.debug {
                "#setMeasuredDimension(width=$width, newHeight=$newHeight)"
            }
            mutableSize.apply {
                this.width = width
                this.height = newHeight
            }
        }
    }

//    private fun configureTransform(
//        screenResolution: Resolution,
//        previewResolution: Resolution,
//        rotation: Rotation
//    ) {
//        textureView.setTransform(Matrix().apply {
//
//            val centerX = screenResolution.width * 0.5f
//            val centerY = screenResolution.height * 0.5f
//
//            when (rotation) {
//                Rotation._0 -> {
//                }
//                Rotation._90 -> {
//                    val scale =
//                        previewResolution.width.toFloat() / previewResolution.height.toFloat()
//                    postScale(1 / scale, scale, centerX, centerY)
//                    postRotate(270f, centerX, centerY)
//                }
//                Rotation._180 -> {
//                    postRotate(180f, centerX, centerY)
//                }
//                Rotation._270 -> {
//                    val scale =
//                        previewResolution.width.toFloat() / previewResolution.height.toFloat()
//                    postScale(1 / scale, scale, centerX, centerY)
//                    postRotate(90f, centerX, centerY)
//                }
//            }
//
//            val tempRatio = screenResolution.width.toFloat() / screenResolution.height
//            val screenWidth = if (tempRatio > 1) screenResolution.width else screenResolution.height
//            val screenHeight =
//                if (tempRatio > 1) screenResolution.height else screenResolution.width
//            val screenAspectRatio: Float = screenWidth.toFloat() / screenHeight.toFloat()
//            val previewAspectRatio: Float =
//                previewResolution.width.toFloat() / previewResolution.height.toFloat()
//
//            val scaleFactor: Float = screenAspectRatio / previewAspectRatio
//
//            val heightCorrection = when (rotation) {
//                Rotation._0, Rotation._180 -> (screenHeight.toFloat() * scaleFactor -
//                        screenHeight.toFloat()) / 2f
//                Rotation._90, Rotation._270 -> (screenWidth.toFloat() * scaleFactor -
//                        screenWidth.toFloat()) / 2f
//            }
//
//            postScale(scaleFactor, 1f)
//            postTranslate(-heightCorrection, 0f)
//
//            if (scaleFactor < 1f) {
//                postScale(1f / scaleFactor, 1f / scaleFactor)
//                postTranslate(heightCorrection, heightCorrection)
//            }
//        })
//    }

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

internal val previewLogger = object : Tag {

    override val tag: String = "ReflektPreview"

    override val level: LogLevel = LogLevel.DEFAULT
}

private suspend fun Mutex.twiceLock() {
    lock()
    lock()
}

private enum class Side {
    HEIGHT,
    WIDTH
}

data class MutableSize(var width: Int = 0, var height: Int = 0)