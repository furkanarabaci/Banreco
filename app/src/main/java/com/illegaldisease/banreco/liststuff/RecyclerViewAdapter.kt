package com.illegaldisease.banreco.liststuff

import android.support.v7.widget.RecyclerView
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import com.illegaldisease.banreco.R
import com.illegaldisease.banreco.databaserelated.EventsRemastered

import com.illegaldisease.banreco.liststuff.ItemFragment.OnListFragmentInteractionListener

class RecyclerViewAdapter(private val mValues: MutableList<EventsRemastered>, private val mListener: OnListFragmentInteractionListener?)
    : RecyclerView.Adapter<RecyclerViewAdapter.ViewHolder>() {
    private val mOnClickListener: View.OnClickListener

    init {
        mOnClickListener = View.OnClickListener { v ->
            val item = v.tag as EventsRemastered
            // Notify the active callbacks interface (the activity, if the fragment is attached to
            // one) that an item has been selected.
            mListener?.onListFragmentInteraction(item)
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val view = LayoutInflater.from(parent.context)
                .inflate(R.layout.fragment_list, parent, false)
        return ViewHolder(view)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val item = mValues[position]
        with(holder.mView) {
            holder.eventImage!!.setImageBitmap(item.photo)
            holder.eventDate!!.text = item.date
            setOnClickListener(mOnClickListener)
        }
    }

    override fun getItemCount(): Int = mValues.size

    inner class ViewHolder(val mView: View) : RecyclerView.ViewHolder(mView) {
        var eventDate : TextView? = null
        var eventImage : ImageView? = null
        init {
            eventDate = mView.findViewById(R.id.listDate)
            eventImage = mView.findViewById(R.id.listImage)
        }
    }
}
