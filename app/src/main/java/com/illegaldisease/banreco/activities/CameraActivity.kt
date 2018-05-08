@file:Suppress("DEPRECATION")

package com.illegaldisease.banreco.activities

import android.Manifest
import android.annotation.SuppressLint
import android.app.Activity
import android.app.AlertDialog
import android.content.*
import android.content.pm.PackageManager
import android.os.Bundle
import android.support.v7.app.AppCompatActivity
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Matrix
import android.graphics.drawable.Drawable
import android.hardware.Camera //Deprecated but who cares ? If i am bored, i will switch to camera2 again.
import android.net.Uri
import android.os.Build
import android.provider.CalendarContract
import android.provider.MediaStore
import android.support.design.widget.FloatingActionButton
import android.support.design.widget.Snackbar
import android.support.v4.app.ActivityCompat
import android.util.Log
import android.view.*
import android.widget.ProgressBar
import android.widget.Toast

import co.zsmb.materialdrawerkt.builders.accountHeader
import co.zsmb.materialdrawerkt.builders.drawer
import co.zsmb.materialdrawerkt.draweritems.badgeable.secondaryItem
import co.zsmb.materialdrawerkt.draweritems.divider
import co.zsmb.materialdrawerkt.draweritems.profile.profile

import com.google.android.gms.auth.api.signin.GoogleSignIn
import com.google.android.gms.auth.api.signin.GoogleSignInAccount
import com.google.android.gms.auth.api.signin.GoogleSignInClient
import com.google.android.gms.auth.api.signin.GoogleSignInOptions

import com.bumptech.glide.Glide
import com.bumptech.glide.request.target.SimpleTarget
import com.bumptech.glide.request.transition.Transition
import com.google.android.gms.common.ConnectionResult
import com.google.android.gms.common.GoogleApiAvailability
import com.google.android.gms.common.api.CommonStatusCodes
import com.google.android.gms.vision.text.TextBlock
import com.google.android.gms.vision.text.TextRecognizer
import com.illegaldisease.banreco.ocrstuff.OcrDetectorProcessor
import com.illegaldisease.banreco.ocrstuff.OcrGraphic
import com.illegaldisease.banreco.R
import com.illegaldisease.banreco.camera.CameraSource
import com.illegaldisease.banreco.camera.CameraSourcePreview
import com.illegaldisease.banreco.camera.GraphicOverlay
import com.illegaldisease.banreco.databaserelated.DatabaseHandler
import com.illegaldisease.banreco.databaserelated.EventHandler
import com.illegaldisease.banreco.databaserelated.EventModel
import com.treebo.internetavailabilitychecker.InternetAvailabilityChecker
import com.treebo.internetavailabilitychecker.InternetConnectivityListener
import com.wdullaer.materialdatetimepicker.date.DatePickerDialog
import com.wdullaer.materialdatetimepicker.time.TimePickerDialog
import java.io.ByteArrayOutputStream
import java.io.File
import java.io.FileOutputStream
import java.io.IOException

import java.util.*

class CameraActivity : AppCompatActivity(), TimePickerDialog.OnTimeSetListener,DatePickerDialog.OnDateSetListener, InternetConnectivityListener {
    companion object {
        private const val RC_SIGN_IN = 9001
        private const val TAG = "CameraActivity"

        // Intent request code to handle updating play services if needed.
        private const val RC_HANDLE_GMS = 9001

        // Permission request codes need to be < 256
        private const val RC_HANDLE_CAMERA_PERM = 2

        const val TextBlockObject = "String"
    }
    private var mCameraSource: CameraSource? = null
    private var mPreview: CameraSourcePreview? = null
    private var mGraphicOverlay: GraphicOverlay<OcrGraphic>? = null

    private var alertDialog : AlertDialog? = null
    private var progressBar : ProgressBar? = null
    private var photoButton : FloatingActionButton ?= null
    private var flashButton : FloatingActionButton ?= null
    private var isFlashOn : Boolean ?= null //One boolean value will not hurt. This will be obsolete if flash is not supported.
    private var isAutoFocusOn : Boolean ?= null

    // Helper objects for detecting taps and pinches.
    private var gestureDetector: GestureDetector? = null

    private var mGoogleSignInClient : GoogleSignInClient? = null
    private var signInAccount : GoogleSignInAccount? = null
    private var profilePic : Bitmap? = null
    private var profileMail : String? = null
    private var profileName : String? = null
    private var lastEventDate : Calendar? = null

    private var mInternetAvailabilityChecker : InternetAvailabilityChecker? = null
    override fun onInternetConnectivityChanged(isConnected: Boolean) {
        if(isConnected){
            progressBar!!.visibility = ProgressBar.INVISIBLE
            buildCamera()
            checkSignIn() // Attempts to login with async callbacks. be careful.
        }
        else{
            Snackbar.make(window.decorView.rootView,"Waiting for internet connection",Snackbar.LENGTH_LONG).show()
            progressBar!!.visibility = ProgressBar.VISIBLE
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        window.setFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN, WindowManager.LayoutParams.FLAG_FULLSCREEN)
        setContentView(R.layout.activity_camera)
        InternetAvailabilityChecker.init(this)

        profilePic = BitmapFactory.decodeResource(this@CameraActivity.resources, R.drawable.photo1)
        profileMail = "notsignedin@placeholder.com" //Placeholder values
        profileName = "Anonymouse" //I know it is anonymous, it is intended.
        lastEventDate = GregorianCalendar.getInstance(TimeZone.getDefault()) //Don't forget to re-initialize

        isFlashOn = false
        isAutoFocusOn = false

        photoButton = findViewById(R.id.fab_take_photo)
        photoButton!!.setOnClickListener { takePicture() }
        initializeFlashButton()

        val gso = GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN)
                .requestEmail()
                .requestId()
                .requestProfile()
                .build()
        mGoogleSignInClient = GoogleSignIn.getClient(this, gso)

        mPreview = findViewById(R.id.preview)
        mGraphicOverlay = findViewById(R.id.graphicOverlay)

        progressBar = findViewById(R.id.indeterminateBar)
        warnUserAboutLibraries() //Only creates, does not show it.
    }
    override fun onStart() {
        super.onStart()
        gestureDetector = GestureDetector(this, CaptureGestureListener())
        mInternetAvailabilityChecker = InternetAvailabilityChecker.getInstance()
        mInternetAvailabilityChecker!!.addInternetConnectivityListener(this)
        initializeDrawerBar()
    }
    override fun onResume() {
        super.onResume()
        startCameraSource()
    }
    override fun onPause() {
        super.onPause()
        if (mPreview != null) {
            mPreview!!.stop()
        }
    }
    override fun onDestroy() {
        super.onDestroy()
        mInternetAvailabilityChecker!!.removeInternetConnectivityChangeListener(this)
        if (mPreview != null) {
            mPreview!!.release()
        }
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if(requestCode == RC_SIGN_IN){
            if(resultCode == Activity.RESULT_OK){
                val task = GoogleSignIn.getSignedInAccountFromIntent(data)
                signInAccount = task.result //This is newly signed in user.
                postSignIn()
            }
            else{
                // Sign in rejected. Program will probably cease to work eventually, warn user.
                Snackbar.make(window.decorView.rootView,"You did not sign-in. Program may not work as expected.",Snackbar.LENGTH_LONG)
                initializeDrawerBar()
            }
        }
    }
    override fun onDateSet(view: DatePickerDialog?, year: Int, monthOfYear: Int, dayOfMonth: Int) {
        lastEventDate!!.set(Calendar.MONTH, monthOfYear)
        lastEventDate!!.set(Calendar.YEAR, year)
        lastEventDate!!.set(Calendar.DAY_OF_MONTH, dayOfMonth)
        pickTime()
    }
    override fun onTimeSet(view: TimePickerDialog?, hourOfDay: Int, minute: Int, second: Int) {
        lastEventDate!!.set(Calendar.HOUR_OF_DAY, hourOfDay)
        lastEventDate!!.set(Calendar.MINUTE, minute)
        lastEventDate!!.set(Calendar.SECOND, second)
    }
    override fun onRequestPermissionsResult(requestCode: Int, permissions: Array<String>, grantResults: IntArray) {
        if (requestCode != RC_HANDLE_CAMERA_PERM) {
            Log.d(TAG, "Got unexpected permission result: $requestCode")
            super.onRequestPermissionsResult(requestCode, permissions, grantResults)
            return
        }

        if (grantResults.isNotEmpty() && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
            Log.d(TAG, "Camera permission granted - initialize the camera source")
            buildCamera()
            return
        }

        Log.e(TAG, "Permission not granted: results len = " + grantResults.size +
                " Result code = " + if (grantResults.isNotEmpty()) grantResults[0] else "(empty)")

        val listener = DialogInterface.OnClickListener { _, _ -> finish() }

        val builder = AlertDialog.Builder(this)
        builder.setTitle("Multitracker sample")
                .setMessage(R.string.no_camera_permission)
                .setPositiveButton(R.string.ok, listener)
                .show()
    }

    private fun checkAutoFocus(){
        if(packageManager.hasSystemFeature(PackageManager.FEATURE_CAMERA_AUTOFOCUS)) {
            isAutoFocusOn = true //It might be left as is forever.
        }
    }
    private fun initializeFlashButton(){
        if(packageManager.hasSystemFeature(PackageManager.FEATURE_CAMERA_FLASH)){
            flashButton = findViewById(R.id.fab_flash) //These two is hidden at the beginning.
            toggleButtonVisibility(true)
            flashButton!!.setOnClickListener{
                isFlashOn = isFlashOn!!.not() //Just for better readability.
                if(isFlashOn!!){
                    mCameraSource!!.flashMode = Camera.Parameters.FLASH_MODE_TORCH
                    flashButton!!.setImageResource(R.drawable.ic_flash_off_white_24px)
                    restartCameraSource()
                }
                else{
                    mCameraSource!!.flashMode = Camera.Parameters.FLASH_MODE_OFF
                    flashButton!!.setImageResource(R.drawable.ic_flash_on_white_24px)
                    restartCameraSource()
                }
            }
        }
        else{
            //Flash is not supported. Do not even bother creating flash button. Just be aware of nulls.
        }
    }
    private fun toggleButtonVisibility(newVisibility : Boolean){
        if(!newVisibility){
            flashButton?.hide()
            photoButton!!.hide()
        }
        else{
            flashButton?.show()
            photoButton!!.show()
        }
    }
    private fun warnUserAboutLibraries(){
        val alertBuilder = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            AlertDialog.Builder(this, android.R.style.Theme_Material_Dialog_Alert)
        } else {
            AlertDialog.Builder(this)
        }
        alertBuilder.setTitle(R.string.alertdialogtitle)
                .setMessage(R.string.alertdialogmessage)
                .setPositiveButton(R.string.alertdialogclearcache) { _, _ ->
                    val thePackageName = "com.google.android.gms"
                    try {
                        //Open the specific App Info page:
                        val intent = Intent(android.provider.Settings.ACTION_APPLICATION_DETAILS_SETTINGS)
                        intent.data = Uri.parse("package:$thePackageName")
                        startActivity(intent)

                    } catch ( e : ActivityNotFoundException) {
                        //e.printStackTrace();
                        //Open the generic Apps page:
                        val intent = Intent(android.provider.Settings.ACTION_MANAGE_APPLICATIONS_SETTINGS)
                        startActivity(intent)
                    }
                }.setNegativeButton(R.string.alertdialogrestart) { _, _ ->
                    val i = baseContext.packageManager.getLaunchIntentForPackage(baseContext.packageName)
                    i.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP)
                    startActivity(i)
                    finish()
                }
                .setIcon(android.R.drawable.ic_dialog_alert)
        alertDialog = alertBuilder.create() //Show or destroy it whenever you want.
    }
    private fun initializeDrawerBar(){
        drawer {
            accountHeader{
                background = R.drawable.images
                profile(profileName!!,profileMail!!){
                    //According to google, photoUrl will be null if user does not have Google+ enabled and have profile there. So i will add placeholder for now.
                    iconBitmap = profilePic!! //Fallback is described at oncreate
                    //TODO: Consider adding sign-out options ??
                }
            }
            secondaryItem(getString(R.string.drawer_events)) {
                icon = R.drawable.ic_event_available_black_24dp
                onClick {_ ->
                    startActivity(Intent(this@CameraActivity, EventsActivity:: class.java))
                    false
                }
            }
            secondaryItem(getString(R.string.drawer_calendar)) {
                icon = R.drawable.ic_date_range_black_24dp
                onClick {_ ->
                    openCalendar(System.currentTimeMillis())
                    false
                }
            }
            secondaryItem(getString(R.string.drawer_settings)) {
                icon = R.drawable.ic_settings_black_24dp
                onClick {_ ->
                    startActivity(Intent(this@CameraActivity, SettingsActivity:: class.java))
                    false
                }
            }
            secondaryItem(getString(R.string.drawer_logs)) {
                icon = R.drawable.ic_archive_black_24dp
                onClick {_ ->
                    startActivity(Intent(this@CameraActivity, LogsActivity:: class.java))
                    false
                }
            }
            secondaryItem(getString(R.string.drawer_about)) {
                icon = R.drawable.ic_more_black_24dp
                onClick {_ ->
                    //I don't care, leave it empty
                    false
                }
            }
            secondaryItem(getString(R.string.drawer_rate)) {
                icon = R.drawable.ic_star_border_black_24dp
                onClick { _ ->
                    //Fill here if you put this on google play
                    false
                }
            }
            divider {  }
            secondaryItem(getString(R.string.drawer_logout)) {
                icon = R.drawable.ic_exit_to_app_black_24dp
                onClick {_ ->
                    finish()
                    System.exit(0) //Exit with success
                    false
                }
            }
        }
        restartCameraSource() //There is some mysterious bug about drawer, so i restart every time.
    }
    private fun openCalendar(openTime : Long){
        val builder = CalendarContract.CONTENT_URI.buildUpon()
        builder.appendPath("time")
        ContentUris.appendId(builder, openTime)
        val intent = Intent(Intent.ACTION_VIEW).setData(builder.build())
        startActivity(intent)
    }

    private fun checkSignIn(){
        val task = mGoogleSignInClient!!.silentSignIn()
        if (task.isSuccessful) {
            // There's immediate result available.
            signInAccount = task.result
            postSignIn()
        } else {
            // There's no immediate result ready, displays some progress indicator and waits for the
            // async callback.
            //TODO: I don't know, maybe show some animation when waiting to log in ?
            task.addOnCompleteListener(this){ signIn ->
                if(signIn.isSuccessful){
                    signInAccount = task.result
                    postSignIn()
                }
                else{
                    //It is failed. Send some errors or i don't know. Maybe try signing in again  ?
                    signInToGoogle() //Result is async.
                }
            }
        }
    }
    private fun postSignIn() {
        val currentUri = signInAccount!!.photoUrl
        profileName = signInAccount!!.displayName
        profileMail = signInAccount!!.email
        val target = object : SimpleTarget<Bitmap>() {
            override fun onResourceReady(resource: Bitmap?, transition: Transition<in Bitmap>?) {
                profilePic = resource
                initializeDrawerBar()
            }
            override fun onLoadFailed(errorDrawable: Drawable?) {
                //It is not guaranteed that we can get our url. Fallback is provided in drawerbar.
                super.onLoadFailed(errorDrawable)
                initializeDrawerBar()
            }
        }
        Glide.with(this)
                .asBitmap()
                .load(currentUri)
                .into(target)
    }
    private fun signInToGoogle(){
        val signInIntent = mGoogleSignInClient!!.signInIntent
        startActivityForResult(signInIntent, RC_SIGN_IN) //We checked internet connection before
    }

    private fun buildCamera(){
        // read parameters from the intent used to launch the activity.
        // Check for the camera permission before accessing the camera.  If the
        // permission is not granted yet, request permission.
        val rc = ActivityCompat.checkSelfPermission(this, Manifest.permission.CAMERA)
        if (rc == PackageManager.PERMISSION_GRANTED) {
            createCameraSource()
        } else {
            requestCameraPermission()
        }
    }
    private fun requestCameraPermission() {
        Log.w(TAG, "Camera permission is not granted. Requesting permission")

        val permissions = arrayOf(Manifest.permission.CAMERA)

        if (!ActivityCompat.shouldShowRequestPermissionRationale(this,
                        Manifest.permission.CAMERA)) {
            ActivityCompat.requestPermissions(this, permissions, RC_HANDLE_CAMERA_PERM)
            return
        }

        val listener = View.OnClickListener {
            ActivityCompat.requestPermissions(this, permissions,
                    RC_HANDLE_CAMERA_PERM)
        }

        Snackbar.make(mGraphicOverlay!!, R.string.permission_camera_rationale,
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
        val textRecognizer = TextRecognizer.Builder(this.applicationContext).build()
        textRecognizer.setProcessor(OcrDetectorProcessor(mGraphicOverlay))
        if (!textRecognizer.isOperational) {
            /** isOperational() can be used to check if the required native libraries are currently
             * available. The detectors will automatically become operational once the library
             * downloads complete on device.
             */
            alertDialog!!.show() //Always try to show it.
            // Check for low storage.  If there is low storage, the native library will not be
            // downloaded, so detection will not become operational.
            val lowstorageFilter = IntentFilter(Intent.ACTION_DEVICE_STORAGE_LOW)
            val hasLowStorage = this.registerReceiver(null, lowstorageFilter) != null
            if (hasLowStorage) {
                Toast.makeText(this, R.string.low_storage_error, Toast.LENGTH_LONG).show()
                Log.w(TAG, getString(R.string.low_storage_error))
            }
        }
        else{
            alertDialog!!.dismiss()
            // Creates and starts the camera.  Note that this uses a higher resolution in comparison
            // to other detection examples to enable the text recognizer to detect small pieces of text.
            mCameraSource = CameraSource.Builder(applicationContext, textRecognizer)
                    .setFacing(CameraSource.CAMERA_FACING_BACK)
                    .setRequestedPreviewSize(1280, 1024)
                    .setRequestedFps(2.0f)
                    .setFlashMode(if (isFlashOn!!) Camera.Parameters.FLASH_MODE_TORCH else null)
                    .setFocusMode(if (isAutoFocusOn!!) Camera.Parameters.FOCUS_MODE_CONTINUOUS_PICTURE else null)
                    .build()
        }

    }
    @SuppressLint("MissingPermission")
    private fun startCameraSource() {
        // Check that the device has play services available.
        val code = GoogleApiAvailability.getInstance().isGooglePlayServicesAvailable(applicationContext)
        if (code != ConnectionResult.SUCCESS) {
            val dlg = GoogleApiAvailability.getInstance().getErrorDialog(this, code, RC_HANDLE_GMS)
            dlg.show()
        }
        if (mCameraSource != null) {
            try {

                checkAutoFocus() //And enable it if it is supported.
                mPreview!!.start(mCameraSource, mGraphicOverlay)
                toggleButtonVisibility(true)
            } catch (e : IOException) {
                Log.e(TAG, "Unable to start camera source.", e)
                mCameraSource!!.release()
                mCameraSource = null
            }
        }
    }
    private fun restartCameraSource() {
        mPreview!!.stop()
        buildCamera()
        startCameraSource()
    }
    private fun takePicture(){
        mCameraSource!!.takePicture(null, CameraSource.PictureCallback {
            val imageFile : File
                try {
                    // convert byte array into bitmap
                    val loadedImage : Bitmap = BitmapFactory.decodeByteArray(it, 0, it.size)
                    val rotateMatrix = Matrix()
                    rotateMatrix.postRotate(0f)
                    val rotatedBitmap = Bitmap.createBitmap(loadedImage, 0, 0,
                            loadedImage.width, loadedImage.height,
                            rotateMatrix, false)
                    val filePath = File(filesDir.toURI())
                    val date = System.currentTimeMillis() / 1000 //TODO:Test purposes. Change it later.
                    //val date = lastEventDate!!.timeInMillis / 1000
                    imageFile = File(filePath.absolutePath
                            + File.separator
                            + date
                            + ".jpeg")
                    imageFile.createNewFile()
                    val ostream = ByteArrayOutputStream()
                    // save image into gallery
                    rotatedBitmap.compress(Bitmap.CompressFormat.JPEG, 100, ostream)
                    FileOutputStream(imageFile).apply {
                        write(ostream.toByteArray())
                        close()
                    }
                    EventHandler.addEvent(this, EventModel(0,date.toInt()))
                } catch (e : Exception) {
                    e.printStackTrace()
                }
        })
    }

    private fun pickTime(){
        val tpd = TimePickerDialog.newInstance(
                this@CameraActivity,
                true
        )
        tpd.show(fragmentManager,"TimePicker")
        tpd.version = TimePickerDialog.Version.VERSION_2
    }
    private fun pickDate(){
        val now = Calendar.getInstance()
        val dpd = DatePickerDialog.newInstance(
                this@CameraActivity,
                now.get(Calendar.YEAR),
                now.get(Calendar.MONTH),
                now.get(Calendar.DAY_OF_MONTH)
        )
        dpd.show(fragmentManager, "DatePicker")
        dpd.version = DatePickerDialog.Version.VERSION_2
    }

    private fun onTap(rawX : Float ,rawY : Float) : Boolean {
        val graphic = mGraphicOverlay!!.getGraphicAtLocation(rawX, rawY)
        var text : TextBlock? = null
        if (graphic != null) {
            text = graphic.textBlock
            if (text != null && text.value != null) {
                val data = Intent()
                data.putExtra(TextBlockObject, text.value)
                setResult(CommonStatusCodes.SUCCESS, data)
            }
            else {
                Log.d(TAG, "text d ata is null")
            }
        }
        else {
            Log.d(TAG,"no text detected")
        }
        return text != null
    }
    override fun dispatchTouchEvent(e: MotionEvent?): Boolean {
        val c = gestureDetector!!.onTouchEvent(e)

        return c || super.dispatchTouchEvent(e)
    }
    private inner class CaptureGestureListener : GestureDetector.SimpleOnGestureListener() {

        override fun onSingleTapConfirmed(e: MotionEvent): Boolean {
            return onTap(e.rawX, e.rawY) || super.onSingleTapConfirmed(e)
        }
    }
}
