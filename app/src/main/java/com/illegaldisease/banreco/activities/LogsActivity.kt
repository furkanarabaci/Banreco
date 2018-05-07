package com.illegaldisease.banreco.activities

import android.os.Bundle
import android.support.v7.app.AppCompatActivity
import android.support.v7.widget.LinearLayoutManager
import android.support.v7.widget.RecyclerView
import com.illegaldisease.banreco.R
import com.illegaldisease.banreco.databaserelated.EventsRemastered
import com.illegaldisease.banreco.liststuff.ItemFragment

class LogsActivity : AppCompatActivity(), ItemFragment.OnListFragmentInteractionListener {
    override fun onListFragmentInteraction(item: EventsRemastered?) {

    }
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_logs)
    }
}
