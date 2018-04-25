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
import com.google.android.gms.auth.api.signin.GoogleSignInClient
import com.google.android.gms.auth.api.signin.GoogleSignInOptions

class CameraActivity : AppCompatActivity() {

    private var mGoogleSignInClient : GoogleSignInClient? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        window.setFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN, WindowManager.LayoutParams.FLAG_FULLSCREEN)
        setContentView(R.layout.activity_camera)
        val gso = GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN)
                .requestEmail()
                .build()
        mGoogleSignInClient = GoogleSignIn.getClient(this, gso)
        initializeDrawerBar()
    }
    override fun onStart() {
        super.onStart()
        checkSignIn() // Attempts to login with async callbacks. be careful.
    }
    private fun initializeDrawerBar(){
        drawer {
            accountHeader{
                background = R.drawable.background //TODO: Could find better background.
                profile("placeholder","placeholder@hold.me"){
                    icon = R.drawable.photo1 //TODO: Change placeholders with google sign in thingy.
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
    private fun checkSignIn(){
        val task = mGoogleSignInClient!!.silentSignIn()
        if (task.isSuccessful) {
            // There's immediate result available.
            val signInAccount = task.result
            //TODO: Immediate result. Call your navbar update here.
        } else {
            // There's no immediate result ready, displays some progress indicator and waits for the
            // async callback.
            //TODO: I don't know, maybe show some animation when waiting to log in ?
            task.addOnCompleteListener(this){ signIntask ->
                if(signIntask.isSuccessful){
                    //TODO: Call navigation drawer update here again.
                }
                else{
                    //It is failed. Send some errors or i don't know. Break their heads ?2=
                }
            }
        }
    }
}
