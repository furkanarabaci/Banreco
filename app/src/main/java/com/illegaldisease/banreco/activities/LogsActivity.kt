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

    private lateinit var recyclerView: RecyclerView
    private lateinit var viewAdapter: RecyclerView.Adapter<*>
    private lateinit var viewManager: RecyclerView.LayoutManager

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_logs)
    }
}
