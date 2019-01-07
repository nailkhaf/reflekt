package tech.khana.reflekt

import android.app.AlertDialog
import android.os.Bundle
import android.support.v4.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import kotlinx.android.synthetic.main.fragment_camera.*
import kotlinx.coroutines.*
import tech.khana.reflekt.core.ReflektCamera
import tech.khana.reflekt.core.ReflektCameraImpl
import tech.khana.reflekt.core.UserSettings
import tech.khana.reflekt.core.rotationOf
import tech.khana.reflekt.preview.ReflektPreview
import kotlin.coroutines.CoroutineContext

class CameraFragment : Fragment(), CoroutineScope {

    private val job = Job()

    override val coroutineContext: CoroutineContext
        get() = Dispatchers.Main + job

    private lateinit var preview: ReflektPreview

    private lateinit var camera: ReflektCamera

    private var toggle = false

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
        preview.setOnClickListener {
            launch {
                toggle = toggle.not()
                when (toggle) {
                    true -> {
                        cameraControls.visibility = View.VISIBLE
                    }
                    false -> {
                        cameraControls.visibility = View.INVISIBLE
                    }
                }
            }
        }

        val rotation = rotationOf(requireActivity().windowManager.defaultDisplay.rotation)
        camera = ReflektCameraImpl(
            requireActivity(), UserSettings(
                surfaces = listOf(preview),
                rotation = rotation
            )
        )


        val builder = AlertDialog.Builder(requireActivity()).apply {
            setTitle(R.string.pick_aspect_ratio)
        }
        aspectRatioButton.setOnClickListener {
            launch {
                val aspectRatios = camera.availablePreviewAspectRatios()
                builder.setItems(aspectRatios.map { it.name }.toTypedArray()) { _, id ->
                    this@CameraFragment.launch {
                        camera.previewAspectRatio(aspectRatios[id])
                    }
                }
                builder.show()
            }
        }
    }

    override fun onResume() {
        super.onResume()
        launch {
            camera.open()
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