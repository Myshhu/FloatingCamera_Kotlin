package com.example.floatingcamerakotlin

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.support.v7.app.AppCompatActivity
import android.os.Bundle
import android.provider.Settings
import android.support.annotation.RequiresApi
import android.support.v4.app.ActivityCompat
import android.support.v4.content.ContextCompat
import android.support.v7.app.AlertDialog
import android.view.View
import android.widget.Toast

class MainActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        requestNeededPermissions()
    }

    private fun requestNeededPermissions() {
        if (isCameraPermissionDenied() || isWriteExternalStoragePermissionDenied()) {
            requestCameraAndStoragePermissions()
        }
        if(isOverlayPermissionDenied()) {
            showOverlayPermissionDialog()
        }
    }

    @RequiresApi(Build.VERSION_CODES.M)
    private fun showOverlayPermissionDialog() {
        val builder = AlertDialog.Builder(this)
        builder.setTitle("Give permissions")
        builder.setMessage("Application don't have permissions to draw overlays, which is needed for floatingcamera, from where " + "you can take photos. Click ok and in next step give this application needed permissions.")

        builder.setPositiveButton("Ok") { dialog, which ->
            requestOverlayPermission()
        }
        builder.setOnCancelListener{
            createOverlayNotAvailableToast()
        }

        builder.show()
    }

    private fun requestCameraAndStoragePermissions() = ActivityCompat.requestPermissions(this, arrayOf(Manifest.permission.CAMERA, Manifest.permission.WRITE_EXTERNAL_STORAGE), 0)

    @RequiresApi(Build.VERSION_CODES.M)
    private fun requestOverlayPermission() {
            val intent = Intent(Settings.ACTION_MANAGE_OVERLAY_PERMISSION,
                Uri.parse("package:$packageName"))
            startActivityForResult(intent, 1)
    }

    private fun isCameraPermissionDenied() = ContextCompat.checkSelfPermission(this, Manifest.permission.CAMERA) == PackageManager.PERMISSION_DENIED

    private fun isWriteExternalStoragePermissionDenied() = ContextCompat.checkSelfPermission(this, Manifest.permission.WRITE_EXTERNAL_STORAGE) == PackageManager.PERMISSION_DENIED

    private fun isOverlayPermissionDenied() = Build.VERSION.SDK_INT >= Build.VERSION_CODES.M && !Settings.canDrawOverlays(this)

    private fun areAllPermissionsGranted() = !isCameraPermissionDenied()
            && !isWriteExternalStoragePermissionDenied()
            && !isOverlayPermissionDenied()

    fun btnLaunchCameraClick(view: View) {
        if(areAllPermissionsGranted()) {
            startService(Intent(this, FloatingViewService::class.java))
            finish()
        } else {
            requestNeededPermissions()
        }
    }

    fun btnInfoClick(view: View) {
        showInformationDialogAboutPermissions()
    }

    private fun showInformationDialogAboutPermissions() {
        val builder = AlertDialog.Builder(this)
        builder.setTitle("Give permissions")
        builder.setMessage(
            "This application needs following permissions:\n" +
                    "- Camera permission for taking photos\n" +
                    "- Storage permission for saving photos\n" +
                    "- Overlay permission for floatingcamera view\n" +
                    "Please give this app all needed permissions for it to work properly."
        )
        builder.setPositiveButton("Ok") { _, _ ->  }
        builder.show()
    }

    private fun MainActivity.makeToast(text: String) = Toast.makeText(
        this,
        text,
        Toast.LENGTH_LONG
    ).show()

    private fun createOverlayNotAvailableToast() {
        makeToast("Draw over other app permission not available. Please give needed permissions for app to work.")
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        if (requestCode == 1) {
            if(isOverlayPermissionDenied()) {
                createOverlayNotAvailableToast()
            }
        }
        super.onActivityResult(requestCode, resultCode, data)
    }
}
