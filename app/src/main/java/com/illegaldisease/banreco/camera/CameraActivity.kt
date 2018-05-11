package com.illegaldisease.banreco.camera

import android.app.Activity
import android.content.*
import android.os.Bundle
import android.support.v7.app.AppCompatActivity
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.drawable.Drawable
import android.provider.CalendarContract
import android.support.design.widget.Snackbar
import android.view.*

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
import com.illegaldisease.banreco.activities.AboutActivity
import com.illegaldisease.banreco.activities.EventsActivity
import com.illegaldisease.banreco.activities.LogsActivity
import com.illegaldisease.banreco.activities.SettingsActivity
import com.illegaldisease.banreco.databaserelated.EventHandler

import com.treebo.internetavailabilitychecker.InternetAvailabilityChecker
import mehdi.sakout.aboutpage.AboutPage
import mehdi.sakout.aboutpage.Element


class CameraActivity : AppCompatActivity(), CameraFragment.MyFragmentCallback {
    override fun drawBar() {
        initializeDrawerBar() //We always need this.
    }

    companion object {
        private const val RC_SIGN_IN = 9001
    }

    private lateinit var mGoogleSignInClient : GoogleSignInClient
    private lateinit var signInAccount : GoogleSignInAccount
    private lateinit var profilePic : Bitmap

    private var profileMail : String = "notsignedin@placeholder.com"
    private var profileName : String = "Anonymouse"

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        window.setFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN, WindowManager.LayoutParams.FLAG_FULLSCREEN)
        InternetAvailabilityChecker.init(this)
        setContentView(R.layout.activity_camera)
        profilePic = BitmapFactory.decodeResource(this@CameraActivity.resources, R.drawable.photo1)



        val gso = GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN)
                .requestEmail()
                .requestId()
                .requestProfile()
                .build()
        mGoogleSignInClient = GoogleSignIn.getClient(this, gso)
        checkSignIn() // Attempts to login with async callbacks. be careful.
    }

    override fun onStart() {
        super.onStart()
        EventHandler.fillOutTables(this) //Fill out tables before we open this activity.
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

    fun initializeDrawerBar(){
        drawer {
            accountHeader{
                background = R.drawable.images
                profile(profileName,profileMail){
                    //According to google, photoUrl will be null if user does not have Google+ enabled and have profile there. So i will add placeholder for now.
                    iconBitmap = profilePic //Fallback is described at oncreate
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
                    startActivity(Intent(this@CameraActivity, AboutActivity:: class.java))
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

    private fun checkSignIn(){
        val task = mGoogleSignInClient.silentSignIn()
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
        val currentUri = signInAccount.photoUrl
        profileName = signInAccount.displayName ?: profileName
        profileMail = signInAccount.email ?: profileMail
        val target = object : SimpleTarget<Bitmap>() {
            override fun onResourceReady(resource: Bitmap?, transition: Transition<in Bitmap>?) {
                profilePic = resource ?: profilePic
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
        val signInIntent = mGoogleSignInClient.signInIntent
        startActivityForResult(signInIntent, RC_SIGN_IN) //We checked internet connection before
    }
}
