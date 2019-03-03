package tech.khana.reflekt.frames.app.ui.main

import android.arch.lifecycle.ViewModelProviders
import android.graphics.Point
import android.os.Bundle
import android.support.v4.app.Fragment
import android.util.Log
import android.util.Size
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import kotlinx.android.synthetic.main.main_fragment.*
import kotlinx.coroutines.*
import tech.khana.reflekt.api.Manager
import tech.khana.reflekt.api.models.CameraConfig
import tech.khana.reflekt.api.models.Lens
import tech.khana.reflekt.api.models.surfaceOrientationToInt
import tech.khana.reflekt.camera.CameraManager
import tech.khana.reflekt.frames.app.R
import tech.khana.reflekt.session.frames.FrameProcessorSessionFactory
import kotlin.coroutines.CoroutineContext

class MainFragment : Fragment(), CoroutineScope {

    private val exceptionHandler = CoroutineExceptionHandler { coroutineContext, throwable ->
        Log.e("frames-app", throwable.message ?: "", throwable)
    }

    private val job = SupervisorJob()

    override val coroutineContext: CoroutineContext
        get() = Dispatchers.Main + exceptionHandler + job

    companion object {
        fun newInstance() = MainFragment()
    }

    private lateinit var viewModel: MainViewModel
    private lateinit var cameraManager: Manager

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        return inflater.inflate(R.layout.main_fragment, container, false)
    }

    override fun onActivityCreated(savedInstanceState: Bundle?) {
        super.onActivityCreated(savedInstanceState)
        viewModel = ViewModelProviders.of(this).get(MainViewModel::class.java)

        val rotation = surfaceOrientationToInt(requireActivity().windowManager.defaultDisplay.rotation)
        val size = Point()
        requireActivity().windowManager.defaultDisplay.getSize(size)
        val cameraConfig = CameraConfig(
            surfaces = listOf(cameraPreview),
            screenOrientation = rotation,
            screenSize = Size(size.x, size.y)
        )
        cameraManager = CameraManager(requireContext(), cameraConfig) { handler, coroutineScope ->
            FrameProcessorSessionFactory(coroutineScope, handler)
        }
    }

    override fun onResume() {
        super.onResume()
        launch {
            cameraManager.open(Lens.FRONT)
        }
    }

    override fun onPause() {
        cameraManager.close()
        super.onPause()
    }

    override fun onDestroy() {
        cameraManager.release()
        super.onDestroy()
    }
}
