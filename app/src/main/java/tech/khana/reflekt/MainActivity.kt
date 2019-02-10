package tech.khana.reflekt

import android.Manifest.permission.*
import android.annotation.SuppressLint
import android.content.Context
import android.content.pm.PackageManager.PERMISSION_GRANTED
import android.graphics.Color
import android.hardware.camera2.CameraManager
import android.location.LocationManager
import android.location.LocationProvider
import android.os.Bundle
import android.support.v4.app.ActivityCompat
import android.support.v4.content.ContextCompat
import android.support.v7.app.AppCompatActivity
import android.view.View.*
import kotlinx.coroutines.*

private const val PERMISSION_REQ_CODE = 345

class MainActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        window.decorView.setBackgroundColor(Color.BLACK)

        window.decorView.setOnSystemUiVisibilityChangeListener {
            when (it) {
                SYSTEM_UI_FLAG_VISIBLE -> {
                    GlobalScope.launch(Dispatchers.Default) {
                        delay(1000)
                        withContext(Dispatchers.Main) {
                            hideSystemUI()
                        }
                    }
                }
            }
        }
    }

    @SuppressLint("MissingPermission")
    override fun onWindowFocusChanged(hasFocus: Boolean) {
        super.onWindowFocusChanged(hasFocus)
        if (hasFocus) hideSystemUI()
    }

    private fun hideSystemUI() {
        window.decorView.systemUiVisibility = (
                SYSTEM_UI_FLAG_LAYOUT_STABLE or
                        SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION or
                        SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN or

                        SYSTEM_UI_FLAG_IMMERSIVE or
                        SYSTEM_UI_FLAG_FULLSCREEN or
                        SYSTEM_UI_FLAG_HIDE_NAVIGATION
                )
    }

    override fun onResume() {
        super.onResume()
        checkPermissions()
    }

    override fun onRequestPermissionsResult(
        requestCode: Int, permissions: Array<out String>, grantResults: IntArray
    ) {
        if (requestCode == PERMISSION_REQ_CODE) {
            val allPermissionsGranted = grantResults.all { it == PERMISSION_GRANTED }
            if (allPermissionsGranted) {
                addCamera()
            } else {
                checkPermissions()
            }
        } else {
            super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        }
    }

    private fun addCamera() {
        val fragment = supportFragmentManager.findFragmentByTag(CameraFragment.TAG)
        if (fragment == null) {
            supportFragmentManager.beginTransaction().apply {
                add(R.id.container, CameraFragment.newInstance(), CameraFragment.TAG)
            }.commit()
        }
    }

    private fun checkPermissions() {
        if (ContextCompat.checkSelfPermission(this, CAMERA) != PERMISSION_GRANTED ||
            ContextCompat.checkSelfPermission(this, WRITE_EXTERNAL_STORAGE) != PERMISSION_GRANTED ||
            ContextCompat.checkSelfPermission(this, READ_EXTERNAL_STORAGE) != PERMISSION_GRANTED ||
            ContextCompat.checkSelfPermission(this, RECORD_AUDIO) != PERMISSION_GRANTED ||
            ContextCompat.checkSelfPermission(this, ACCESS_COARSE_LOCATION) != PERMISSION_GRANTED
        ) {
            ActivityCompat.requestPermissions(
                this,
                arrayOf(CAMERA, WRITE_EXTERNAL_STORAGE, READ_EXTERNAL_STORAGE, RECORD_AUDIO, ACCESS_COARSE_LOCATION),
                PERMISSION_REQ_CODE
            )
        } else {
            addCamera()
        }
    }
}
