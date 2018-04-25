package com.illegaldisease.banreco

import android.content.Intent
import android.os.Bundle
import android.support.design.widget.NavigationView
import android.support.v7.app.AppCompatActivity
import android.view.View
import android.view.WindowManager
import com.google.android.gms.auth.api.signin.GoogleSignIn
import com.google.android.gms.auth.api.signin.GoogleSignInClient
import com.google.android.gms.auth.api.signin.GoogleSignInOptions
import com.mikepenz.materialdrawer.DrawerBuilder



class CameraActivity : AppCompatActivity() {

    private var mGoogleSignInClient : GoogleSignInClient? = null
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        window.setFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN, WindowManager.LayoutParams.FLAG_FULLSCREEN)
        setContentView(R.layout.activity_camera)
        val gso = GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN)
                .requestEmail()
                .build()
        mGoogleSignInClient = GoogleSignIn.getClient(this, gso);
        DrawerBuilder().withActivity(this).build()
    }

    override fun onStart() {
        super.onStart()
        checkSignIn() // Attempts to login with async callbacks. be careful.
    }
    fun checkSignIn(){
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
