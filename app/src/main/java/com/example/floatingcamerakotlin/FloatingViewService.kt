package com.example.floatingcamerakotlin

import android.annotation.SuppressLint
import android.app.Service
import android.content.Context
import android.content.Intent
import android.graphics.PixelFormat
import android.hardware.Camera
import android.net.Uri
import android.os.Build
import android.os.Environment
import android.os.IBinder
import android.provider.MediaStore.Files.FileColumns.MEDIA_TYPE_IMAGE
import android.provider.MediaStore.Files.FileColumns.MEDIA_TYPE_VIDEO
import android.util.Log
import android.view.*
import android.widget.Button
import android.widget.FrameLayout
import android.widget.LinearLayout
import java.io.File
import java.io.FileOutputStream
import java.text.SimpleDateFormat
import java.util.*

class FloatingViewService : Service() {

    private lateinit var floatingView: View
    private var layoutParams: WindowManager.LayoutParams? = null
    private var mWindowManager: WindowManager? = null
    private var setBackCamera = true
    private var flagCanTakePicture = true
    private var mCamera: Camera? = null
    private lateinit var mPicture: Camera.PictureCallback

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onCreate() {
        super.onCreate()

        createFloatingView()
        createLayoutParams()
        createWindowManager()
        addFloatingViewToWindowManager()

        prepareCameraView(setBackCamera)
        createCameraPictureCallback()

        setListeners()
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
        val mPreview = createCameraPreviewFromCamera(mCamera)
        addCameraPreviewToWidget(mPreview)
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

    private fun createCameraPreviewFromCamera(mCamera: Camera?) = mCamera?.let { CameraPreview(this, it) }

    private fun addCameraPreviewToWidget(mPreview: CameraPreview?) {
        val preview: FrameLayout = floatingView.findViewById(R.id.camera_preview)
        preview.removeAllViews()
        preview.addView(mPreview)
    }

    private fun createCameraPictureCallback() {
        mPicture = Camera.PictureCallback { byteData, camera ->
            val fileToSavePicture: File? = getOutputMediaFile(MEDIA_TYPE_IMAGE)
            mCamera?.startPreview()
            if (fileToSavePicture == null) {
                flagCanTakePicture = false
                return@PictureCallback
            }

            try {
                val fos = FileOutputStream(fileToSavePicture)
                fos.write(byteData)
                fos.close()
                sendBroadcast(
                    Intent(
                        Intent.ACTION_MEDIA_SCANNER_SCAN_FILE,
                        Uri.parse("file://$fileToSavePicture")
                    )
                )
                flagCanTakePicture = true
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }

    /** Create a File for saving an image or video  */
    private fun getOutputMediaFile(type: Int): File? {
        // To be safe, you should check that the SDCard is mounted
        // using Environment.getExternalStorageState() before doing this.

        val mediaStorageDir =
            File(Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_PICTURES), "MyCameraApp")
        // This location works best if you want the created images to be shared
        // between applications and persist after your app has been uninstalled.

        // Create the storage directory if it does not exist
        if (!mediaStorageDir.exists()) {
            if (!mediaStorageDir.mkdirs()) {
                Log.d("MyCameraApp", "failed to create directory")
                return null
            }
        }

        // Create a media file name
        val timeStamp = SimpleDateFormat("yyyyMMdd_HHmmss").format(Date())
        val mediaFile: File
        mediaFile = when (type) {
            MEDIA_TYPE_IMAGE -> File(
                mediaStorageDir.path + File.separator +
                        "IMG_" + timeStamp + ".jpg"
            )
            MEDIA_TYPE_VIDEO -> File(
                (mediaStorageDir.path + File.separator +
                        "VID_" + timeStamp + ".mp4")
            )
            else -> return null
        }
        return mediaFile
    }


    private fun setListeners() {
        setBtnMakePhotoListener()
        setBtnCloseListener()
        setLayoutListener()
    }

    private fun setBtnMakePhotoListener() {
        val btnMakePhoto: Button = floatingView.findViewById(R.id.btnMakePhoto)
        btnMakePhoto.setOnClickListener { v ->
            if (flagCanTakePicture) {
                flagCanTakePicture = false
                mCamera?.takePicture(null, null, mPicture)
            }
        }
    }

    private fun setBtnCloseListener() {
        val btnClose: Button = floatingView.findViewById(R.id.btnClose)
        btnClose.setOnClickListener{
            releaseCamera()
            stopSelf()
        }
    }

    private fun releaseCamera() {
        mCamera?.stopPreview()
        mCamera?.release()
    }

    @SuppressLint("ClickableViewAccessibility")
    private fun setLayoutListener() {
        val linearLayout: LinearLayout = floatingView.findViewById(R.id.linearLayout)

        linearLayout.setOnTouchListener(object : View.OnTouchListener {
            var initialX: Int = 0
            var initialY: Int = 0
            var initialTouchX: Float = 0.toFloat()
            var initialTouchY: Float = 0.toFloat()

            override fun onTouch(v: View, event: MotionEvent): Boolean {
                when (event.action) {
                    MotionEvent.ACTION_DOWN -> {

                        //Remember the initial position.
                        initialX = layoutParams?.x ?: 0
                        initialY = layoutParams?.y ?: 0

                        //Get the touch location
                        initialTouchX = event.rawX
                        initialTouchY = event.rawY
                        return true
                    }

                    MotionEvent.ACTION_UP -> {
                        val Xdiff = (event.rawX - initialTouchX).toInt()
                        val Ydiff = (event.rawY - initialTouchY).toInt()

                        //The check for Xdiff <10 && YDiff< 10 because sometime elements moves a little while clicking.
                        if (Xdiff < 10 && Ydiff < 10) {
                            //stopSelf();
                        }
                        return true
                    }
                    MotionEvent.ACTION_MOVE -> {
                        //Calculate the X and Y coordinates of the view.
                        layoutParams?.x = initialX + (event.rawX - initialTouchX).toInt()
                        layoutParams?.y = initialY + (event.rawY - initialTouchY).toInt()
                        //Update the layout with new X & Y coordinate
                        mWindowManager?.updateViewLayout(floatingView, layoutParams)
                        return true
                    }
                }
                return false
            }
        })
    }

    override fun onDestroy() {
        super.onDestroy()
        mWindowManager?.removeView(floatingView)
    }
}