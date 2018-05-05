package com.illegaldisease.banreco.activities

import android.os.Bundle
import android.support.v7.app.AppCompatActivity
import com.illegaldisease.banreco.R

import kotlinx.android.synthetic.main.activity_events.*

class EventsActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_events)
        setSupportActionBar(toolbar)
    }

}
