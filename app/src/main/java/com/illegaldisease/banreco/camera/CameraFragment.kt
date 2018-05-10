@file:Suppress("DEPRECATION")
package com.illegaldisease.banreco.camera

import android.Manifest
import android.annotation.SuppressLint
import android.app.Activity
import android.app.AlertDialog
import android.content.*
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Matrix
import android.hardware.Camera //TODO: Add camera2 support.
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.provider.Settings
import android.support.design.widget.FloatingActionButton
import android.support.design.widget.Snackbar
import android.support.v4.app.ActivityCompat
import android.support.v4.app.Fragment
import android.util.Log
import android.view.*
import android.widget.ProgressBar
import android.widget.Toast

import com.google.android.gms.common.ConnectionResult
import com.google.android.gms.common.GoogleApiAvailability
import com.google.android.gms.vision.text.TextRecognizer

import com.illegaldisease.banreco.R
import com.illegaldisease.banreco.activities.ImageActivity
import com.illegaldisease.banreco.databaserelated.EventHandler
import com.illegaldisease.banreco.databaserelated.EventModel
import com.illegaldisease.banreco.ocrstuff.OcrDetectorProcessor
import com.illegaldisease.banreco.ocrstuff.OcrGraphic
import com.illegaldisease.banreco.ocrstuff.OcrHandler

import com.treebo.internetavailabilitychecker.InternetAvailabilityChecker
import com.treebo.internetavailabilitychecker.InternetConnectivityListener
import org.jetbrains.anko.doAsync

import java.io.ByteArrayOutputStream
import java.io.File
import java.io.FileOutputStream
import java.io.IOException

/**
 * Note that this fragment will probably crash if it detaches from activity.
 */
class CameraFragment : Fragment(), InternetConnectivityListener {
    companion object {
        private const val TAG = "CameraActivity"

        // Intent request code to handle updating play services if needed.
        private const val RC_HANDLE_GMS = 9001

        // Permission request codes need to be < 256
        private const val RC_HANDLE_CAMERA_PERM = 2

        //const val TextBlockObject = "String"
    }
    private lateinit var mCameraSource: CameraSource
    private lateinit var mPreview: CameraSourcePreview
    private lateinit var mGraphicOverlay: GraphicOverlay<OcrGraphic>

    private lateinit var alertDialog : AlertDialog
    private lateinit var progressBar : ProgressBar
    private lateinit var photoButton : FloatingActionButton
    private lateinit var flashButton : FloatingActionButton

    private var isFlashOn : Boolean = false //One boolean value will not hurt. This will be obsolete if flash is not supported.
    private var isAutoFocusOn : Boolean = false
    private var isCameraClicked : Boolean = false //This is to prevent repeatedly clicking camera button
    private var isFlashClicked : Boolean = false //Same aspect with isCameraClicked

    private lateinit var fragmentCallBack : MyFragmentCallback

    // Helper objects for detecting taps and pinches.

    private lateinit var mInternetAvailabilityChecker : InternetAvailabilityChecker

    override fun onInternetConnectivityChanged(isConnected: Boolean) {
        if(isConnected){
            progressBar.visibility = ProgressBar.INVISIBLE
            fragmentCallBack.drawBar()
            restartCameraSource()
        }
        else{
            Snackbar.make(activity!!.window.decorView.rootView,"Waiting for internet connection", Snackbar.LENGTH_LONG).show()
            progressBar.visibility = ProgressBar.VISIBLE
        }
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        mInternetAvailabilityChecker = InternetAvailabilityChecker.getInstance()
        mInternetAvailabilityChecker.addInternetConnectivityListener(this)
        return inflater.inflate(R.layout.fragment_camera, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        mPreview = view.findViewById(R.id.preview)
        mGraphicOverlay = view.findViewById(R.id.graphicOverlay)

        mInternetAvailabilityChecker = InternetAvailabilityChecker.getInstance()
        progressBar = view.findViewById(R.id.indeterminateBar)

        initializeCameraButton()
        initializeFlashButton()

        warnUserAboutLibraries() //Only creates, does not show it.
    }
    override fun onStart() {
        super.onStart()
    }
    override fun onResume() {
        super.onResume()
        restartCameraSource()
    }
    override fun onPause() {
        super.onPause()
        mPreview.stop()
    }
    override fun onDestroy() {
        super.onDestroy()
        mInternetAvailabilityChecker.removeInternetConnectivityChangeListener(this)
        mPreview.release()
    }

    override fun onAttach(activity : Context){
        fragmentCallBack = activity as MyFragmentCallback
        super.onAttach(activity)
    }

    private fun checkAutoFocus(){
        if(activity!!.packageManager.hasSystemFeature(PackageManager.FEATURE_CAMERA_AUTOFOCUS)) {
            isAutoFocusOn = true //It might be left as is forever.
        }
    }
    private fun initializeFlashButton(){
        flashButton = view!!.findViewById(R.id.fab_flash) //These two is hidden at the beginning.
        if(activity!!.packageManager.hasSystemFeature(PackageManager.FEATURE_CAMERA_FLASH)){
            flashButton.show()
            flashButton.setOnClickListener{
                if(!isFlashClicked){
                    isFlashOn = isFlashOn.not() //Just for better readability.
                    if(isFlashOn){
                        mCameraSource.flashMode = Camera.Parameters.FLASH_MODE_TORCH
                        flashButton.setImageResource(R.drawable.ic_flash_off_white_24px)
                        restartCameraSource()
                    }
                    else{
                        mCameraSource.flashMode = Camera.Parameters.FLASH_MODE_OFF
                        flashButton.setImageResource(R.drawable.ic_flash_on_white_24px)
                        restartCameraSource()
                    }
                    isFlashClicked = true
                }
            }
        }
        else{
            //Flash is not supported. Do not even bother creating flash button. Just be aware of nulls.
        }
    }
    private fun initializeCameraButton(){
        photoButton = view!!.findViewById(R.id.fab_take_photo)
        photoButton.setOnClickListener {
            if(!isCameraClicked) {
                takePicture() //This also redirects to the activity.
                isCameraClicked = true
            }
        }
    }
    private fun warnUserAboutLibraries(){
        val alertBuilder = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            AlertDialog.Builder(activity, android.R.style.Theme_Material_Dialog_Alert)
        } else {
            AlertDialog.Builder(activity)
        }
        alertBuilder.setTitle(R.string.alertdialogtitle)
                .setMessage(R.string.alertdialogmessage)
                .setPositiveButton(R.string.alertdialogclearcache) { _, _ ->
                    val thePackageName = "com.google.android.gms"
                    try {
                        //Open the specific App Info page:
                        val intent = Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS)
                        intent.data = Uri.parse("package:$thePackageName")
                        startActivity(intent)

                    } catch ( e : ActivityNotFoundException) {
                        //e.printStackTrace();
                        //Open the generic Apps page:
                        val intent = Intent(Settings.ACTION_MANAGE_APPLICATIONS_SETTINGS)
                        startActivity(intent)
                    }
                }.setNegativeButton(R.string.alertdialogrestart) { _, _ ->
                    val i = activity!!.baseContext.packageManager.getLaunchIntentForPackage(activity!!.baseContext.packageName)
                    i.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP)
                    startActivity(i)
                    activity!!.finish()
                }
                .setIcon(android.R.drawable.ic_dialog_alert)
        alertDialog = alertBuilder.create() //Show or destroy it whenever you want.
    }

    private fun buildCamera(){
        // read parameters from the intent used to launch the activity.
        // Check for the camera permission before accessing the camera.  If the
        // permission is not granted yet, request permission.
        val rc = ActivityCompat.checkSelfPermission(activity!!.baseContext, Manifest.permission.CAMERA)
        if (rc == PackageManager.PERMISSION_GRANTED) {
            createCameraSource()
        } else {
            requestCameraPermission()
        }
    }
    private fun requestCameraPermission() {
        Log.w(CameraFragment.TAG, "Camera permission is not granted. Requesting permission")

        val permissions = arrayOf(Manifest.permission.CAMERA)

        if (!ActivityCompat.shouldShowRequestPermissionRationale(activity!!,
                        Manifest.permission.CAMERA)) {
            ActivityCompat.requestPermissions(activity!!, permissions, RC_HANDLE_CAMERA_PERM)
            return
        }

        val listener = View.OnClickListener {
            ActivityCompat.requestPermissions(activity!!, permissions,
                    RC_HANDLE_CAMERA_PERM)
        }

        Snackbar.make(mGraphicOverlay, R.string.permission_camera_rationale,
                Snackbar.LENGTH_INDEFINITE)
                .setAction(R.string.ok, listener)
                .show()
    }
    private fun createCameraSource() {
        /**
         * Creates and starts the camera.  Note that this uses a higher resolution in comparison
         * to other detection examples to enable the ocr detector to detect small text samples
         * at long distances.
         *
         * A text recognizer is created to find text.  An associated processor instance
         * is set to receive the text recognition results and display graphics for each text block
         * on screen.
         */
        val textRecognizer = TextRecognizer.Builder(activity!!.applicationContext).build()
        textRecognizer.setProcessor(OcrDetectorProcessor(mGraphicOverlay))
        if (!textRecognizer.isOperational) {
            /** isOperational() can be used to check if the required native libraries are currently
             * available. The detectors will automatically become operational once the library
             * downloads complete on device.
             */
            alertDialog.show() //Always try to show it.
            // Check for low storage.  If there is low storage, the native library will not be
            // downloaded, so detection will not become operational.
            val lowstorageFilter = IntentFilter(Intent.ACTION_DEVICE_STORAGE_LOW)
            val hasLowStorage = activity!!.registerReceiver(null, lowstorageFilter) != null
            if (hasLowStorage) {
                Toast.makeText(activity!!, R.string.low_storage_error, Toast.LENGTH_LONG).show()
                Log.w(CameraFragment.TAG, getString(R.string.low_storage_error))
            }
        }
        else{
            alertDialog.dismiss()
            // Creates and starts the camera.  Note that this uses a higher resolution in comparison
            // to other detection examples to enable the text recognizer to detect small pieces of text.
            mCameraSource = CameraSource.Builder(activity!!.applicationContext, textRecognizer)
                    .setFacing(CameraSource.CAMERA_FACING_BACK)
                    .setRequestedPreviewSize(1280, 1024)
                    .setRequestedFps(2.0f)
                    .setFlashMode(if (isFlashOn) Camera.Parameters.FLASH_MODE_TORCH else null)
                    .setFocusMode(if (isAutoFocusOn) Camera.Parameters.FOCUS_MODE_CONTINUOUS_PICTURE else null)
                    .build()
        }

    }
    @SuppressLint("MissingPermission")
    private fun startCameraSource() {
        // Check that the device has play services available.
        val code = GoogleApiAvailability.getInstance().isGooglePlayServicesAvailable(activity!!.applicationContext)
        if (code != ConnectionResult.SUCCESS) {
            val dlg = GoogleApiAvailability.getInstance().getErrorDialog(activity!!, code, RC_HANDLE_GMS)
            dlg.show()
        }
        try {
            checkAutoFocus() //And enable it if it is supported.
            mPreview.start(mCameraSource, mGraphicOverlay)
            photoButton.show()
        }
        catch (e : IOException) {
            Log.e(CameraFragment.TAG, "Unable to start camera source.", e)
            mCameraSource.release()
            //mCameraSource = null
        }
    }
    private fun restartCameraSource() {
        mPreview.stop()
        buildCamera()
        startCameraSource()
        isFlashClicked = false
    }
    private fun takePicture(){
        tryToParseDate()
        mCameraSource.takePicture(null, CameraSource.PictureCallback {
            try {
                // convert byte array into bitmap
                val loadedImage : Bitmap = BitmapFactory.decodeByteArray(it, 0, it.size)
                val rotateMatrix = Matrix()
                rotateMatrix.postRotate(0f)
                val rotatedBitmap = Bitmap.createBitmap(loadedImage, 0, 0,
                        loadedImage.width, loadedImage.height,
                        rotateMatrix, false)
                EventHandler.lastImageBitmap = rotatedBitmap //Pass it to our static class.
                isCameraClicked = false //Make it clickable again.
                openImageActivity()
            } catch (e : Exception) {
                e.printStackTrace()
            }
        })
    }

    private fun tryToParseDate(){
        //No need to callback, this will be over under 10 miliseconds, but still no need to make program wait more.
        doAsync {
            val ocrHandler = OcrHandler(mGraphicOverlay.mGraphics)
            ocrHandler.tryToParse() //We start to parse now so that it will have immediate results when user went to other activity.
        }

    }

    private fun openImageActivity(){
        try{
            val intent = Intent(context, ImageActivity:: class.java).apply {
                flags = Intent.FLAG_ACTIVITY_NEW_TASK
                putExtra("willshow",true)//We will be editing dates and be done with it.
            }
            context!!.startActivity(intent)
        }
        catch (er : NullPointerException){
            Log.e("onclicklistener",Log.getStackTraceString(er))
        }
    }
    override fun onRequestPermissionsResult(requestCode: Int, permissions: Array<String>, grantResults: IntArray) {
        if (requestCode != RC_HANDLE_CAMERA_PERM) {
            Log.d(CameraFragment.TAG, "Got unexpected permission result: $requestCode")
            super.onRequestPermissionsResult(requestCode, permissions, grantResults)
            return
        }

        if (grantResults.isNotEmpty() && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
            Log.d(CameraFragment.TAG, "Camera permission granted - initialize the camera source")
            buildCamera()
            return
        }

        Log.e(CameraFragment.TAG, """Permission not granted: results len = ${grantResults.size} Result code = ${if (grantResults.isNotEmpty()) grantResults[0] else "(empty)"}""")

        val listener = DialogInterface.OnClickListener { _, _ -> activity!!.finish() }

        val builder = AlertDialog.Builder(activity!!)
        builder.setTitle("Multitracker sample")
                .setMessage(R.string.no_camera_permission)
                .setPositiveButton(R.string.ok, listener)
                .show()
    }

    interface MyFragmentCallback {
        fun drawBar()
    }
}
