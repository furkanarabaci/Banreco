package com.illegaldisease.banreco.liststuff

import android.content.Context
import android.support.design.widget.FloatingActionButton
import android.support.v7.widget.RecyclerView
import android.text.format.DateFormat
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import com.illegaldisease.banreco.R
import com.illegaldisease.banreco.databaserelated.EventHandler
import com.illegaldisease.banreco.databaserelated.EventsRemastered

import com.illegaldisease.banreco.liststuff.ItemFragment.OnListFragmentInteractionListener
import java.util.*

class RecyclerViewAdapter(private val context : Context, private var mValues: MutableList<EventsRemastered>, private val mListener: OnListFragmentInteractionListener?)
    : RecyclerView.Adapter<RecyclerViewAdapter.ViewHolder>() {
    private val mOnClickListener: View.OnClickListener
    private var fab : FloatingActionButton? = null

    init {
        mOnClickListener = View.OnClickListener { v ->
            val item = v.tag as? EventsRemastered
            // Notify the active callbacks interface (the activity, if the fragment is attached to
            // one) that an item has been selected.
            mListener?.onListFragmentInteraction(item)
        }

    }
    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val view = LayoutInflater.from(parent.context).inflate(R.layout.fragment_list, parent, false)
        fab = view.findViewById(R.id.fragmentfloatingActionButton)
        return ViewHolder(view)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val item = mValues[position]
        with(holder.mView) {
            holder.eventImage!!.setImageBitmap(item.photo)
            holder.eventDate!!.text = convertTimestampToString(item.date)
            setOnClickListener(mOnClickListener)
        }
        fab!!.setOnClickListener {
            try {
                EventHandler.deleteEvent(context, item) //Deletes from database and original static list.
            }
            catch (e : Exception){
                mValues.remove(item) //Deletes from current adapter.
            }
            notifyItemRemoved(position)
            notifyItemRangeChanged(position, itemCount)
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
    private fun convertTimestampToString(timestamp : Int) : String {
        val calendar = Calendar.getInstance(Locale.ENGLISH)
        calendar.timeInMillis = timestamp.toLong() * 1000
        return DateFormat.format("dd-MM-yyyy HH:mm",calendar).toString()
    }
}
