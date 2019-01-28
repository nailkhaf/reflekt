package tech.khana.reflekt

import android.os.Bundle
import android.support.v4.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.SeekBar
import kotlinx.android.synthetic.main.fragment_camera.*
import kotlinx.coroutines.*
import tech.khana.reflekt.core.ReflektCamera
import tech.khana.reflekt.core.ReflektCameraImpl
import tech.khana.reflekt.models.LensDirect
import tech.khana.reflekt.models.Settings
import tech.khana.reflekt.models.displayRotationOf
import tech.khana.reflekt.preview.ReflektPreview
import kotlin.coroutines.CoroutineContext

class CameraFragment : Fragment(), CoroutineScope {

    private val job = Job()

    override val coroutineContext: CoroutineContext
        get() = Dispatchers.Main + job

    private lateinit var preview: ReflektPreview

    private lateinit var camera: ReflektCamera

    private var toggle = false

    private lateinit var settings: Settings

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        return inflater.inflate(R.layout.fragment_camera, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        preview = reflektCamera
//        preview.setOnClickListener {
//            launch {
//                toggle = toggle.not()
//                when (toggle) {
//                    true -> {
//                        cameraControls.animate()
//                            .alpha(1f)
//                            .setDuration(300)
//                            .withStartAction { cameraControls.visibility = View.VISIBLE }
//                    }
//                    false -> {
//                        cameraControls.animate()
//                            .alpha(0f)
//                            .setDuration(300)
//                            .withEndAction { cameraControls.visibility = View.INVISIBLE }
//                    }
//                }
//            }
//        }

        val rotation =
            displayRotationOf(requireActivity().windowManager.defaultDisplay.rotation)
        camera = ReflektCameraImpl(
            ctx = requireActivity()
        )

        settings = Settings(
            surfaces = listOf(preview),
            displayRotation = rotation,
            lensDirect = LensDirect.BACK
        )


//        aspectRatioButton.setOnClickListener {
//            launch {
//                val aspectRatios = camera.availablePreviewAspectRatios()
//                AlertDialog.Builder(requireActivity()).apply {
//                    setTitle(R.string.pick_aspect_ratio)
//                    setItems(aspectRatios.map { it.name }.toTypedArray()) { _, id ->
//                        this@CameraFragment.launch {
//                            camera.previewAspectRatio(aspectRatios[id])
//                        }
//                    }
//                    show()
//                }
//            }
//        }

//        switchLensButton.setOnClickListener {
//            launch {
//                val lenses = camera.availableLenses()
//                AlertDialog.Builder(requireActivity()).apply {
//                    setTitle(R.string.pick_lens_direct)
//                    setItems(lenses.map { it.name }.toTypedArray()) { _, id ->
//                        this@CameraFragment.launch {
//                            camera.lens(lenses[id])
//                        }
//                    }
//                    show()
//                }
//            }
//        }

//        flashButton.setOnClickListener {
//            launch {
//                val flashModes = camera.availableFlashModes()
//                AlertDialog.Builder(requireActivity()).apply {
//                    setTitle(R.string.pick_lens_direct)
//                    setItems(flashModes.map { it.name }.toTypedArray()) { _, id ->
//                        this@CameraFragment.launch {
//                            camera.flash(flashModes[id])
//                        }
//                    }
//                    show()
//                }
//            }
//        }

//        runBlocking {
//            val maxZoom = camera.maxZoom()
//            zoomSeekBar.onProgressChanged {
//                if (it % 5 == 0) {
//                    this@CameraFragment.launch {
//                        camera.zoom(maxZoom * it / zoomSeekBar.max)
//                    }
//                }
//            }
//        }
    }

    override fun onResume() {
        super.onResume()
        launch {
            camera.open(settings)
            camera.startSession()
            camera.startPreview()
        }
    }

    override fun onPause() {
        super.onPause()
        runBlocking {
            camera.close()
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        job.cancel()
    }

    companion object {

        const val TAG = "CameraFragment"

        fun newInstance() = CameraFragment()
    }
}

private fun SeekBar.onProgressChanged(f: (progress: Int) -> Unit) {
    setOnSeekBarChangeListener(object : SeekBar.OnSeekBarChangeListener {
        override fun onProgressChanged(seekBar: SeekBar?, progress: Int, fromUser: Boolean) {
            f(progress)
        }

        override fun onStartTrackingTouch(seekBar: SeekBar?) {
        }

        override fun onStopTrackingTouch(seekBar: SeekBar?) {
        }
    })
}