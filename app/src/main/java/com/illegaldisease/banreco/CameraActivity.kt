package com.illegaldisease.banreco

import android.content.Intent
import android.os.Bundle
import android.support.v7.app.AppCompatActivity
import android.view.WindowManager
import co.zsmb.materialdrawerkt.builders.accountHeader
import co.zsmb.materialdrawerkt.builders.drawer
import co.zsmb.materialdrawerkt.draweritems.badgeable.secondaryItem
import co.zsmb.materialdrawerkt.draweritems.divider
import co.zsmb.materialdrawerkt.draweritems.profile.profile
import com.google.android.gms.auth.api.signin.GoogleSignIn
import com.google.android.gms.auth.api.signin.GoogleSignInAccount
import com.google.android.gms.auth.api.signin.GoogleSignInClient
import com.google.android.gms.auth.api.signin.GoogleSignInOptions
import android.net.ConnectivityManager
import android.net.Uri
import android.util.Log
import com.illegaldisease.banreco.R.mipmap.ic_launcher
import android.content.ContentResolver



class CameraActivity : AppCompatActivity() {

    private var mGoogleSignInClient : GoogleSignInClient? = null
    private var signInAccount : GoogleSignInAccount? = null
    private var placeHolderUri : Uri? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        window.setFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN, WindowManager.LayoutParams.FLAG_FULLSCREEN)
        setContentView(R.layout.activity_camera)
        placeHolderUri = Uri.parse(ContentResolver.SCHEME_ANDROID_RESOURCE +
                "://" + resources.getResourcePackageName(R.drawable.photo1)
                + '/'.toString() + resources.getResourceTypeName(R.drawable.photo1) + '/'.toString() + resources.getResourceEntryName(R.drawable.photo1))
        val gso = GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN)
                .requestEmail()
                .build()
        mGoogleSignInClient = GoogleSignIn.getClient(this, gso)
        //initializeDrawerBar() Commented out until i find a way to update drawer runtime
    }
    override fun onStart() {
        super.onStart()
        checkSignIn() // Attempts to login with async callbacks. be careful.
    }
    private fun initializeDrawerBar(){
        var testValue = signInAccount!!.photoUrl ?: "whatever"
        Log.e("Testurl", testValue.toString())
        drawer {
            accountHeader{
                background = R.drawable.background //TODO: Could find better background.
                //TODO: Change placeholders with google sign in thingy.

                profile(signInAccount!!.displayName.toString(),signInAccount!!.email.toString()){
                    //According to google, photoUrl will be null if user does not have Google+ enabled and have profile there. So i will add placeholder for now.
                    iconUri = signInAccount!!.photoUrl ?: placeHolderUri!! // I hope placeholder won't be null ....

                }
            }
            secondaryItem(getString(R.string.drawer_events)) {
                icon = R.drawable.ic_event_available_black_24dp
                onClick {_ ->
                    startActivity(Intent(this@CameraActivity,EventsActivity :: class.java))
                    false
                }
            }
            secondaryItem(getString(R.string.drawer_calendar)) {
                icon = R.drawable.ic_date_range_black_24dp
                onClick {_ ->
                    //TODO: Connect to calender here.
                    false
                }
            }
            secondaryItem(getString(R.string.drawer_settings)) {
                icon = R.drawable.ic_settings_black_24dp
                onClick {_ ->
                    startActivity(Intent(this@CameraActivity,SettingsActivity :: class.java))
                    false
                }
            }
            secondaryItem(getString(R.string.drawer_logs)) {
                icon = R.drawable.ic_archive_black_24dp
                onClick {_ ->
                    startActivity(Intent(this@CameraActivity,LogsActivity :: class.java))
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
    private fun isOnline(): Boolean {
        val cm = getSystemService(CONNECTIVITY_SERVICE) as ConnectivityManager

        val activeNetwork = cm.activeNetworkInfo
        return activeNetwork != null && activeNetwork.isConnectedOrConnecting
    }
    private fun checkSignIn(){
        if(!isOnline()){
            //Means we are not connected to internet. Prompt user.
            //TODO: Add dialog here.
        }
        val task = mGoogleSignInClient!!.silentSignIn()
        if (task.isSuccessful) {
            // There's immediate result available.
            signInAccount = task.result
            initializeDrawerBar()
        } else {
            // There's no immediate result ready, displays some progress indicator and waits for the
            // async callback.
            //TODO: I don't know, maybe show some animation when waiting to log in ?
            task.addOnCompleteListener(this){ signIn ->
                if(signIn.isSuccessful){
                    signInAccount = task.result
                    initializeDrawerBar()
                }
                else{
                    //It is failed. Send some errors or i don't know. Maybe prompt sign in ?
                    signInToGoogle() //Result is async.
                }
            }
        }
    }
    private fun signInToGoogle(){
        val signInIntent = mGoogleSignInClient!!.signInIntent
        if(isOnline()){
            startActivityForResult(signInIntent, RC_SIGN_IN)
        }
        else{
            //Prompt user for not being online.
        }

    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if(requestCode == RC_SIGN_IN){
            val task = GoogleSignIn.getSignedInAccountFromIntent(data)
            signInAccount = task.result //This is newly signed in user.
            initializeDrawerBar()
        }
    }
    companion object {
        private const val RC_SIGN_IN = 9001
    }
}
