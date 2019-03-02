package tech.khana.reflekt.frames.app

import android.Manifest.permission.CAMERA
import android.os.Bundle
import android.support.v4.app.ActivityCompat
import android.support.v4.content.ContextCompat
import android.support.v4.content.PermissionChecker.PERMISSION_GRANTED
import android.support.v7.app.AppCompatActivity
import tech.khana.reflekt.frames.app.ui.main.MainFragment

private const val PERMISSION_REQ_CODE = 3289

class MainActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.main_activity)
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
        supportFragmentManager.beginTransaction()
            .replace(R.id.container, MainFragment.newInstance())
            .commitNow()
    }

    private fun checkPermissions() {
        if (ContextCompat.checkSelfPermission(this, CAMERA) != PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(
                this,
                arrayOf(CAMERA),
                PERMISSION_REQ_CODE
            )
        } else {
            addCamera()
        }
    }

}
