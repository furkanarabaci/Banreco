package com.illegaldisease.banreco

import android.Manifest
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.ImageFormat
import android.graphics.SurfaceTexture
import android.hardware.camera2.CameraAccessException
import android.hardware.camera2.CameraCaptureSession
import android.hardware.camera2.CameraCharacteristics
import android.hardware.camera2.CameraDevice
import android.hardware.camera2.CameraManager
import android.hardware.camera2.CameraMetadata
import android.hardware.camera2.CaptureRequest
import android.hardware.camera2.TotalCaptureResult
import android.media.Image
import android.media.ImageReader
import android.os.Bundle
import android.os.Environment
import android.os.Handler
import android.os.HandlerThread
import android.support.design.widget.FloatingActionButton
import android.support.design.widget.NavigationView
import android.support.v4.app.ActivityCompat
import android.support.v4.app.ActivityCompat.checkSelfPermission
import android.support.v7.app.AppCompatActivity
import android.util.Log
import android.util.Size
import android.util.SparseIntArray
import android.view.Surface
import android.view.TextureView
import android.view.View
import android.view.WindowManager
import android.widget.Toast
import java.io.File
import java.io.FileNotFoundException
import java.io.FileOutputStream
import java.io.IOException
import java.io.OutputStream
import java.net.ConnectException
import java.util.ArrayList
import java.util.Arrays

class CameraActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        window.setFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN, WindowManager.LayoutParams.FLAG_FULLSCREEN)
        setContentView(R.layout.activity_camera)
        val navigation = findViewById<View>(R.id.nav_view) as NavigationView
        navigation.setNavigationItemSelectedListener { item ->
            when (item.itemId) {
                R.id.nav_calendar ->
                    //Opens a google calendar instance.
                    true
                R.id.nav_settings ->
                    //Will be empty for now
                    true
                R.id.nav_about ->
                    //Will be empty for now
                    true
                R.id.nav_logs -> {
                    startActivity(Intent(this@CameraActivity, LogsActivity::class.java))
                    true
                }

                R.id.nav_rate ->
                    //Will be empty. We wont deploy to google play.
                    true
                else -> false
            }
        }
    }

}
