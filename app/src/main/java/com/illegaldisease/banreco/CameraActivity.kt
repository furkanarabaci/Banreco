package com.illegaldisease.banreco

import android.os.Bundle
import android.support.v4.widget.DrawerLayout
import android.support.v7.app.ActionBarDrawerToggle
import android.support.v7.app.AppCompatActivity
import android.view.MenuItem


class CameraActivity : AppCompatActivity() {

    private var mDrawerLayout: DrawerLayout? = null

    private var mToggle: ActionBarDrawerToggle? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_camera)

        mDrawerLayout = findViewById(R.id.drawer_layout) as DrawerLayout
        mToggle = ActionBarDrawerToggle(this, mDrawerLayout, R.string.open, R.string.close)

        mDrawerLayout!!.addDrawerListener(mToggle!!)
        mToggle!!.syncState()
        getSupportActionBar()?.setDisplayHomeAsUpEnabled(true)


    }


    override fun onOptionsItemSelected(item: MenuItem): Boolean {


        if (mToggle!!.onOptionsItemSelected(item))

                    return true ;


                    return super.onOptionsItemSelected(item)


}
}
