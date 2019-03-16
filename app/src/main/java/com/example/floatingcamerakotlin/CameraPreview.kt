package com.example.floatingcamerakotlin

import android.content.ContentValues.TAG
import android.content.Context
import android.hardware.Camera
import android.util.Log
import android.view.SurfaceHolder
import android.view.SurfaceView
import java.io.IOException

class CameraPreview
    (context: Context?, private var mCamera: Camera)
    : SurfaceView(context), SurfaceHolder.Callback{

    private var mHolder: SurfaceHolder = holder

    init {
        // Install a SurfaceHolder.Callback so we get notified when the
        // underlying surface is created and destroyed.
        mHolder.addCallback(this)
        // Deprecated setting, but required on Android versions prior to 3.0
        mHolder.setType(SurfaceHolder.SURFACE_TYPE_PUSH_BUFFERS)
    }

    override fun surfaceCreated(holder: SurfaceHolder?) {
        // The Surface has been created, now tell the camera where to draw the preview.
        try {
            mCamera.setPreviewDisplay(holder)
            mCamera.startPreview()
        } catch (e: IOException) {
            Log.d(TAG, "Error setting camera preview: " + e.message)
        }
    }

    override fun surfaceChanged(holder: SurfaceHolder?, format: Int, width: Int, height: Int) {
        // If your preview can change or rotate, take care of those events here.
        // Make sure to stop the preview before resizing or reformatting it.

        if (mHolder.surface == null) {
            // Preview surface does not exist
            return
        }

        // Stop preview before making changes
        try {
            mCamera.stopPreview()
        } catch (e: Exception) {
            // Ignore: tried to stop a non-existent preview
        }

        // Set preview size and make any resize, rotate or
        // reformatting changes here
        mCamera.setDisplayOrientation(90)

        // Start preview with new settings
        try {
            mCamera.setPreviewDisplay(mHolder)
            mCamera.startPreview()

        } catch (e: Exception) {
            Log.d(TAG, "Error starting camera preview: " + e.message)
        }
    }

    override fun surfaceDestroyed(holder: SurfaceHolder?) {
    }
}