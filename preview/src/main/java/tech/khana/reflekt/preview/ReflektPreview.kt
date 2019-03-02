//package tech.khana.reflekt.preview
//
//import android.annotation.SuppressLint
//import android.content.Context
//import android.graphics.Matrix
//import android.graphics.SurfaceTexture
//import android.util.AttributeSet
//import android.view.Surface
//import android.view.TextureView
//import android.view.View.MeasureSpec.getSize
//import android.view.ViewGroup
//import android.view.ViewGroup.LayoutParams.MATCH_PARENT
//import android.view.ViewGroup.LayoutParams.WRAP_CONTENT
//import android.widget.FrameLayout
//import kotlinx.coroutines.CoroutineScope
//import kotlinx.coroutines.Dispatchers
//import kotlinx.coroutines.SupervisorJob
//import kotlinx.coroutines.sync.Mutex
//import kotlinx.coroutines.withContext
//import tech.khana.reflekt.core.ReflektSurface
//import tech.khana.reflekt.ext.lockSelf
//import tech.khana.reflekt.models.*
//import tech.khana.reflekt.preview.Side.HEIGHT
//import tech.khana.reflekt.preview.Side.WIDTH
//import tech.khana.reflekt.utils.Logger
//import tech.khana.reflekt.utils.debug
//import kotlin.coroutines.CoroutineContext
//
//@SuppressLint("Recycle")
//class ReflektPreview @JvmOverloads constructor(
//    ctx: Context,
//    attrs: AttributeSet? = null
//) : FrameLayout(ctx, attrs), ReflektSurface, Logger by Logger.defaultLogger {
//
//    private var previewRotation = Rotation._0
//    private var previewAspectRatio: AspectRatio? = null
//
//    override val format: ReflektFormat = ReflektFormat.Priv.Texture
//
//    private val textureMatrix = Matrix()
//    private val mutableSize: MutableSize = MutableSize()
//
//    private val textureView = TextureView(context).apply {
//        layoutParams = ViewGroup.LayoutParams(MATCH_PARENT, MATCH_PARENT)
//    }
//
//    override val supportedModes = CameraMode.values().toSet()
//
//    private var currentSurface: Surface? = null
//
//    private val availableTextureMutex = Mutex()
//
//    private var needRelayout = false
//
//    private val surfaceTextureListener = object : TextureView.SurfaceTextureListener {
//
//        override fun onSurfaceTextureAvailable(surface: SurfaceTexture, width: Int, height: Int) {
//            if (availableTextureMutex.isLocked) availableTextureMutex.unlock()
//        }
//
//        override fun onSurfaceTextureSizeChanged(surface: SurfaceTexture, width: Int, height: Int) {
//        }
//
//        override fun onSurfaceTextureUpdated(surface: SurfaceTexture) {
//        }
//
//        override fun onSurfaceTextureDestroyed(surface: SurfaceTexture): Boolean {
//            currentSurface?.release()
//            currentSurface = null
//            return true
//        }
//    }
//
//    init {
//        addView(textureView)
//
//        textureView.surfaceTextureListener = surfaceTextureListener
//
//        addOnLayoutChangeListener { _, _, _, _, _, _, _, _, _ ->
//            debug { "#onLayout" }
//        }
//    }
//
//    override suspend fun acquireSurface(config: SurfaceConfig): Surface = withContext(Dispatchers.Main) {
//        debug { "#acquireSurface" }
//        val previewResolution = config.resolutions.chooseOptimalResolution(config.aspectRatio)
//        this@ReflektPreview.previewAspectRatio = config.aspectRatio
//        previewRotation = config.displayRotation
//
//        val surfaceTexture = textureView.surfaceTexture ?: run {
//            availableTextureMutex.lockSelf()
//            textureView.surfaceTexture
//        }
//        surfaceTexture.setDefaultBufferSize(previewResolution.width, previewResolution.height)
//
//        Surface(surfaceTexture).also {
//            currentSurface?.release()
//            currentSurface = it
//            needRelayout = true
//            debug { "#acquireSurface acquired" }
//        }
//    }
//
//    private fun List<Resolution>.chooseOptimalResolution(aspectRatio: AspectRatio): Resolution =
//        asSequence()
//            .filter { it.ratio == aspectRatio.value }
//            .filter { it.width <= MAX_PREVIEW_WIDTH }
//            .filter { it.height <= MAX_PREVIEW_HEIGHT }
//            .sortedBy { it.area }
//            .last()
//
//    override suspend fun onStart(cameraMode: CameraMode) = withContext(Dispatchers.Main) {
//        if (cameraMode in supportedModes) {
//            if (needRelayout) {
//                requestLayout()
//                needRelayout = false
//            }
//        }
//    }
//
//    override fun onMeasure(widthMeasureSpec: Int, heightMeasureSpec: Int) {
//        debug { "#onMeasure" }
//
//        val width = getSize(widthMeasureSpec)
//        val height = getSize(heightMeasureSpec)
//
//        val aspectRatio = previewAspectRatio
//            ?: return super.onMeasure(widthMeasureSpec, heightMeasureSpec)
//
//        val previewAspectRatio = when (previewRotation) {
//            Rotation._0, Rotation._180 -> aspectRatio.value
//            Rotation._90, Rotation._270 -> 1f / aspectRatio.value
//        }
//
//        val (newWidth, newHeight) = when {
//
//            layoutParams.width == MATCH_PARENT && layoutParams.height == MATCH_PARENT -> {
//                val viewAspectRation = width.toFloat() / height.toFloat()
//                if (viewAspectRation <= previewAspectRatio) {
//                    onMeasureByMatchParent(HEIGHT, width, height, previewAspectRatio, previewRotation)
//                } else {
//                    onMeasureByMatchParent(WIDTH, width, height, previewAspectRatio, previewRotation)
//                }
//            }
//
//            layoutParams.width == WRAP_CONTENT && layoutParams.height == WRAP_CONTENT -> {
//                val viewAspectRation = width.toFloat() / height.toFloat()
//                if (viewAspectRation >= previewAspectRatio) {
//                    onMeasureByMatchParent(HEIGHT, width, height, previewAspectRatio, previewRotation)
//                } else {
//                    onMeasureByMatchParent(WIDTH, width, height, previewAspectRatio, previewRotation)
//                }
//            }
//
//            layoutParams.width == WRAP_CONTENT && layoutParams.height == MATCH_PARENT ->
//                onMeasureByMatchParent(HEIGHT, width, height, previewAspectRatio, previewRotation)
//
//            layoutParams.width == MATCH_PARENT && layoutParams.height == WRAP_CONTENT ->
//                onMeasureByMatchParent(WIDTH, width, height, previewAspectRatio, previewRotation)
//
//            else -> throw IllegalStateException("unknown layout settings")
//        }
//
//        super.onMeasure(
//            MeasureSpec.makeMeasureSpec(newWidth, MeasureSpec.EXACTLY),
//            MeasureSpec.makeMeasureSpec(newHeight, MeasureSpec.EXACTLY)
//        )
//    }
//
//    private fun onMeasureByMatchParent(
//        side: Side, width: Int, height: Int, aspectRatio: Float, rotation: Rotation
//    ): MutableSize = when (side) {
//        HEIGHT -> {
//            val newWidth = (height / aspectRatio).toInt()
//            textureView.setTransform(textureMatrix.apply {
//                reset()
//
//                if (newWidth > width) {
//                    when (rotation) {
//                        Rotation._0 -> {
//                            postTranslate(-(newWidth - width) / 2f, 0f)
//                        }
//                        Rotation._90 -> { // FIXME check that image in center
//                            postTranslate(0f, -(newWidth - width) / 2f)
//                        }
//                        Rotation._270 -> { // FIXME check that image in center
//                            postTranslate(0f, (newWidth - width) / 2f)
//                        }
//                        else -> {
//                        }
//                    }
//                }
//
//                when (rotation) { // FIXME 270 -> 90, 90 -> 270 view is not recreated
//                    Rotation._90 -> {
//                        postRotate(270f, newWidth / 2f, height / 2f)
//                        postScale(1f / aspectRatio, aspectRatio, newWidth / 2f, height / 2f)
//                    }
//                    Rotation._270 -> {
//                        postRotate(90f, newWidth / 2f, height / 2f)
//                        postScale(1f / aspectRatio, aspectRatio, newWidth / 2f, height / 2f)
//                    }
//                    else -> {
//                    }
//                }
//            })
//            debug {
//                "#setMeasuredDimension(newWidth=$newWidth, height=$height)"
//            }
//            mutableSize.apply {
//                this.width = newWidth
//                this.height = height
//            }
//        }
//        WIDTH -> {
//            val newHeight = (width * aspectRatio).toInt()
//            textureView.setTransform(textureMatrix.apply {
//                reset()
//                if (newHeight > height) {
//                    when (rotation) {
//                        Rotation._0 -> {
//                            postTranslate(0f, -(newHeight - height) / 2f)
//                        }
//                        Rotation._90 -> { // FIXME check that image in center
//                            postTranslate((newHeight - height) / 2f, 0f)
//                        }
//                        Rotation._270 -> { // FIXME check that image in center
//                            postTranslate(-(newHeight - height) / 2f, 0f)
//                        }
//                        else -> {
//                        }
//                    }
//                }
//
//                when (rotation) { // FIXME 270 -> 90, 90 -> 270 view is not recreated
//                    Rotation._90 -> {
//                        postRotate(270f, width / 2f, newHeight / 2f)
//                        postScale(1f / aspectRatio, aspectRatio, width / 2f, newHeight / 2f)
//                    }
//                    Rotation._270 -> {
//                        postRotate(90f, width / 2f, newHeight / 2f)
//                        postScale(1f / aspectRatio, aspectRatio, width / 2f, newHeight / 2f)
//                    }
//                    else -> {
//                    }
//                }
//            })
//
//            debug {
//                "#setMeasuredDimension(width=$width, newHeight=$newHeight)"
//            }
//            mutableSize.apply {
//                this.width = width
//                this.height = newHeight
//            }
//        }
//    }
//}
//
//private enum class Side {
//    HEIGHT,
//    WIDTH
//}
//
//private data class MutableSize(var width: Int = 0, var height: Int = 0)