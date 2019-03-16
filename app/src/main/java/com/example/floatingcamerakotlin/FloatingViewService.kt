package com.example.floatingcamerakotlin

import android.app.Service
import android.content.Context
import android.content.Intent
import android.graphics.PixelFormat
import android.hardware.Camera
import android.os.Build
import android.os.IBinder
import android.view.Gravity
import android.view.LayoutInflater
import android.view.View
import android.view.WindowManager
import android.widget.FrameLayout
import android.widget.LinearLayout

class FloatingViewService: Service() {

    private lateinit var floatingView: View
    private var layoutParams: WindowManager.LayoutParams? = null
    private var mWindowManager: WindowManager? = null
    private var setBackCamera = true
    private var flagCanTakePicture = true
    private var mCamera: Camera? = null
    override fun onBind(intent: Intent?): IBinder? = null

    override fun onCreate() {
        super.onCreate()

        createFloatingView()
        createLayoutParams()
        createWindowManager()
        addFloatingViewToWindowManager()

        prepareCameraView(setBackCamera)
    }

    private fun createFloatingView() {
        floatingView = LayoutInflater.from(this).inflate(R.layout.floating_camera_view, null)
    }

    private fun createLayoutParams() {
        val layoutFlag: Int = createLayoutFlag()
        //Create view params
        val createdLayoutParams: WindowManager.LayoutParams

        createdLayoutParams = WindowManager.LayoutParams(
            WindowManager.LayoutParams.WRAP_CONTENT,
            WindowManager.LayoutParams.WRAP_CONTENT,
            layoutFlag, 0,
            PixelFormat.TRANSLUCENT
        )
        createdLayoutParams.flags =
            WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE// | WindowManager.LayoutParams.FLAG_LAYOUT_NO_LIMITS;
        //Specify the view position
        //Initially view will be added to top-left corner
        createdLayoutParams.gravity = Gravity.TOP or Gravity.START
        createdLayoutParams.x = 0
        createdLayoutParams.y = 100

        this.layoutParams = createdLayoutParams
    }

    private fun createLayoutFlag(): Int {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY
        } else {
            WindowManager.LayoutParams.TYPE_PHONE
        }
    }

    private fun createWindowManager() {
        mWindowManager = getSystemService(Context.WINDOW_SERVICE) as WindowManager?
    }

    private fun addFloatingViewToWindowManager() {
        mWindowManager?.addView(floatingView, layoutParams)
    }

    private fun prepareCameraView(setBackCamera: Boolean) {
        mCamera = getCameraInstance(setBackCamera)
        setCameraParameters(mCamera)
        val mPreview = mCamera?.let {
            CameraPreview(this, it)
        }
        val preview: FrameLayout  = floatingView.findViewById(R.id.camera_preview)
        preview.removeAllViews()
        preview.addView(mPreview)
    }

    /** A safe way to get an instance of the Camera object.  */
    private fun getCameraInstance(setBackCamera: Boolean): Camera? {
        var c: Camera? = null
        try {
            c = Camera.open(if (setBackCamera) 0 else 1) // attempt to get a Camera instance
        } catch (e: Exception) {
            e.printStackTrace()
        }
        return c //Returns null if camera is unavailable
    }

    private fun setCameraParameters(camera: Camera?) {
        val cameraParameters = camera?.parameters
        cameraParameters?.setPictureSize(
            cameraParameters.supportedPictureSizes[0].width,
            cameraParameters.supportedPictureSizes[0].height
        )
        camera?.parameters = cameraParameters
    }

}