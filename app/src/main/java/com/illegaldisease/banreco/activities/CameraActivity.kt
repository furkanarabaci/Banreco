package com.illegaldisease.banreco.activities

import android.app.Activity
import android.content.ContentUris
import android.content.Intent
import android.os.Bundle
import android.support.v7.app.AppCompatActivity
import android.view.WindowManager
import android.net.ConnectivityManager
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.drawable.Drawable
import android.provider.CalendarContract

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
import com.illegaldisease.banreco.R
import com.wdullaer.materialdatetimepicker.date.DatePickerDialog
import com.wdullaer.materialdatetimepicker.time.TimePickerDialog

import java.util.*

class CameraActivity : AppCompatActivity(), TimePickerDialog.OnTimeSetListener,DatePickerDialog.OnDateSetListener {
    companion object {
        private const val RC_SIGN_IN = 9001
    }

    private var mGoogleSignInClient : GoogleSignInClient? = null
    private var signInAccount : GoogleSignInAccount? = null
    private var profilePic : Bitmap? = null
    private var profileMail : String? = null
    private var profileName : String? = null
    private var lastEventDate : Calendar? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        window.setFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN, WindowManager.LayoutParams.FLAG_FULLSCREEN)
        setContentView(R.layout.activity_camera)

        profilePic = BitmapFactory.decodeResource(this@CameraActivity.resources, R.drawable.photo1)
        profileMail = "notsignedin@placeholder.com" //Placeholder values
        profileName = "Anonymouse" //I know it is anonymous, it is intended.
        lastEventDate = GregorianCalendar.getInstance(TimeZone.getDefault()) //Don't forget to re-initialize

        val gso = GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN)
                .requestEmail()
                .requestId()
                .requestProfile()
                .build()
        mGoogleSignInClient = GoogleSignIn.getClient(this, gso)
        //initializeDrawerBar() Commented out until i find a way to update drawer runtime
    }
    override fun onStart() {
        super.onStart()
        checkSignIn() // Attempts to login with async callbacks. be careful.
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

    }
    private fun openCalendar(openTime : Long){
        val builder = CalendarContract.CONTENT_URI.buildUpon()
        builder.appendPath("time")
        ContentUris.appendId(builder, openTime)
        val intent = Intent(Intent.ACTION_VIEW).setData(builder.build())
        startActivity(intent)
    }
    private fun isOnline(): Boolean {
        val cm = getSystemService(CONNECTIVITY_SERVICE) as ConnectivityManager

        val activeNetwork = cm.activeNetworkInfo
        return activeNetwork != null && activeNetwork.isConnectedOrConnecting
    }
    private fun checkSignIn(){
        if(!isOnline()){
            //Means we are not connected to internet. Prompt user and leave necessary
            //TODO: Add dialog here.
        }
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

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if(requestCode == RC_SIGN_IN){
            if(resultCode == Activity.RESULT_OK){
                val task = GoogleSignIn.getSignedInAccountFromIntent(data)
                signInAccount = task.result //This is newly signed in user.
                postSignIn()
            }
            else{
                //TODO: Sign in is cancelled, do something else ?
                initializeDrawerBar() // Initialize it with placeholders. Program will probably cease to work at other steps.
            }
        }
    }
    private fun pickTime(){
        val tpd = TimePickerDialog.newInstance(
                this@CameraActivity,
                true //TODO: You might consider time modes
        )
        tpd.show(fragmentManager,"TimePicker")
        tpd.version = TimePickerDialog.Version.VERSION_2
    }
    public fun pickDate(){
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

}
