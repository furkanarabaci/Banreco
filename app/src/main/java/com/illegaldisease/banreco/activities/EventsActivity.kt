package com.illegaldisease.banreco.activities

import android.os.Bundle
import android.support.v7.app.AppCompatActivity
import com.illegaldisease.banreco.R
import com.illegaldisease.banreco.databaserelated.EventsRemastered
import com.illegaldisease.banreco.liststuff.ItemFragment

import kotlinx.android.synthetic.main.activity_events.*

class EventsActivity : AppCompatActivity(), ItemFragment.OnListFragmentInteractionListener {
    override fun onListFragmentInteraction(item: EventsRemastered?) {

    }
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_events)
    }

}
