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
import tech.khana.reflekt.core.ReflektSurface
import tech.khana.reflekt.models.*
import tech.khana.reflekt.models.AspectRatio.AR_16X9
import tech.khana.reflekt.preview.Side.HEIGHT
import tech.khana.reflekt.preview.Side.WIDTH
import tech.khana.reflekt.utils.Logger
import tech.khana.reflekt.utils.debug

const val MAX_PREVIEW_WIDTH = 1920
const val MAX_PREVIEW_HEIGHT = 1080

class ReflektPreview @JvmOverloads constructor(
    ctx: Context,
    attrs: AttributeSet? = null
) : FrameLayout(ctx, attrs), ReflektSurface, Logger by Logger.defaultLogger {

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
            debug { "#onLayout" }
            if (layoutMutex.isLocked) {
                layoutMutex.unlock()
            }
        }
    }

    override suspend fun acquireSurface(config: SurfaceConfig): CameraSurface = coroutineScope {
        debug { "#acquireSurface" }
        withContext(Dispatchers.Main) {
            val previewResolution = config.resolutions
                .chooseOptimalResolution(config.aspectRatio)
            this@ReflektPreview.previewAspectRatio = config.aspectRatio
            previewRotation = config.displayRotation

            requestLayout()
            layoutMutex.lockSelf()

            val surfaceTexture = textureView.onSurfaceTextureAvailable()
            surfaceTexture.setDefaultBufferSize(
                previewResolution.width, previewResolution.height
            )

            debug { "#acquireSurface acquired" }
            CameraSurface(
                CameraMode.PREVIEW,
                Surface(surfaceTexture)
            )
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
        debug { "#onMeasure" }

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
            debug {
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

            debug {
                "#setMeasuredDimension(width=$width, newHeight=$newHeight)"
            }
            mutableSize.apply {
                this.width = width
                this.height = newHeight
            }
        }
    }
}

private suspend fun Mutex.lockSelf() {
    lock()
    lock()
}

private enum class Side {
    HEIGHT,
    WIDTH
}

private data class MutableSize(var width: Int = 0, var height: Int = 0)