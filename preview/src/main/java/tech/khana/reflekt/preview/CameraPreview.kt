package tech.khana.reflekt.preview

import android.content.Context
import android.graphics.Matrix
import android.graphics.SurfaceTexture
import android.util.AttributeSet
import android.util.Size
import android.view.Surface
import android.view.TextureView
import android.view.ViewGroup
import android.view.ViewGroup.LayoutParams.MATCH_PARENT
import android.widget.FrameLayout
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.channels.ReceiveChannel
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.withTimeoutOrNull
import tech.khana.reflekt.api.Logger
import tech.khana.reflekt.api.debug
import tech.khana.reflekt.api.models.*
import tech.khana.reflekt.api.Surface as ReflektSurface

private const val MAX_PREVIEW_WIDTH = 1920
private const val MAX_PREVIEW_HEIGHT = 1080
private const val PREVIEW_ASPECT_RATIO = 16f / 9f

class CameraPreview @JvmOverloads constructor(
    ctx: Context,
    attrs: AttributeSet? = null
) : FrameLayout(ctx, attrs), ReflektSurface, Logger by Logger {

    override val logPrefix: String = "preview"

    private val textureView = TextureView(context).apply {
        layoutParams = ViewGroup.LayoutParams(MATCH_PARENT, MATCH_PARENT)
    }

    override val format: SurfaceFormat = SurfaceFormat.Priv.Texture
    override val modes: Set<CameraMode> = CameraMode.values().toSet()

    private var orientation: Int = 0
    private var displayOrientation: Int = 0
    private var previewSize: Size? = null
    private val availableTextureMutex = Mutex()
    private val transformMatrix: Matrix = Matrix()

    private val surfaceTextureListener = object : TextureView.SurfaceTextureListener {

        override fun onSurfaceTextureAvailable(surface: SurfaceTexture, width: Int, height: Int) {
            debug { "#onSurfaceTextureAvailable width=$width, height=$height" }
            if (availableTextureMutex.isLocked) availableTextureMutex.unlock()
            transformTextureView(width, height)
        }

        override fun onSurfaceTextureSizeChanged(surface: SurfaceTexture, width: Int, height: Int) {
            debug { "#onSurfaceTextureSizeChanged width=$width, height=$height" }
            transformTextureView(width, height)
        }

        override fun onSurfaceTextureUpdated(surface: SurfaceTexture) {
//            debug { "#onSurfaceTextureUpdated" }
        }

        override fun onSurfaceTextureDestroyed(surface: SurfaceTexture): Boolean {
            debug { "#onSurfaceTextureDestroyed" }
            return true
        }
    }

    init {
        addView(textureView)
        textureView.surfaceTextureListener = surfaceTextureListener
    }

    override fun acquireSurface(
        surfaceConfig: SurfaceConfig
    ): ReceiveChannel<Surface> = Channel<Surface>(10).also { channel ->
        CoroutineScope(Dispatchers.Main).launch {
            debug { "#acquireSurface" }

            val previewSize = surfaceConfig.outputSizes.chooseOptimalResolution()

            withTimeoutOrNull(10_000) {
                if (textureView.isAvailable) return@withTimeoutOrNull
                if (availableTextureMutex.isLocked) error("availableTextureMutex already locked")
                availableTextureMutex.lock()
                availableTextureMutex.lock()
            } ?: error("can't get surface for preview")

            val surfaceTexture = textureView.surfaceTexture
            surfaceTexture.setDefaultBufferSize(previewSize.width, previewSize.height)

            this@CameraPreview.orientation = getCorrectOrientation(surfaceConfig)
            this@CameraPreview.displayOrientation = surfaceConfig.screenOrientation
            this@CameraPreview.previewSize = previewSize

            transformTextureView(width, height)

            channel.send(Surface(surfaceTexture))
            debug { "#acquiredSurface" }
        }
    }

    private fun List<Size>.chooseOptimalResolution(): Size = asSequence()
        .filter { it.width <= MAX_PREVIEW_WIDTH }
        .filter { it.height <= MAX_PREVIEW_HEIGHT }
        .filter { it.ratio == PREVIEW_ASPECT_RATIO }
        .sortedBy { it.area }
        .last()

    private fun transformTextureView(width: Int, height: Int) {
        previewSize?.let {
            debug { "#transformTextureView preview=${it.width}X${it.height} view=${width}X$height" }
            textureView.setTransform(transformMatrix.apply {
                reset()
                configureTransform(it, width, height)
            })
        }
    }

    private fun Matrix.configureTransform(previewSize: Size, width: Int, height: Int) {
        val centerX = width / 2f
        val centerY = height / 2f
        val displayOrientation = displayOrientation
        val shouldSwap = when (displayOrientation) {
            0, 180 -> true
            90, 270 -> false
            else -> error("")
        }
        val viewWidth = if (shouldSwap) height else width
        val viewHeight = if (shouldSwap) width else height
        val viewRatio = viewWidth.toFloat() / viewHeight.toFloat()
        val previewRatio = previewSize.width.toFloat() / previewSize.height.toFloat()
        val scale = viewRatio / previewRatio

        val rotation = when (displayOrientation) {
            0 -> 0f
            180 -> 180f
            90 -> 270f
            270 -> 90f
            else -> error("unknown display rotation")
        }

        val scaleCompensation = when (displayOrientation) {
            0 -> 1f
            180 -> 1f
            90 -> previewRatio
            270 -> previewRatio
            else -> error("unknown display rotation")
        }

        postRotate(rotation, centerX, centerY)

        postScale(
            scaleCompensation * if (scale > 1) scale else 1f,
            1f / scaleCompensation * if (scale < 1) 1f / scale else 1f,
            centerX,
            centerY
        )
    }

    override fun release() {
    }
}
